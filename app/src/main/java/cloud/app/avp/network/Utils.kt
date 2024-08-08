package cloud.app.avp.network

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resumeWithException

private val mustHaveBody = listOf("POST", "PUT")
private val cantHaveBody = listOf("GET", "HEAD")

const val USER_AGENT =
  "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"

fun Map<String, String>.toNiceFiles(): List<NiceFile> =
  this.map {
    NiceFile(it.key, it.value)
  }

class NiceFile(val name: String, val fileName: String, val file: File?, val fileType: String?) {
  constructor(name: String, value: String) : this(name, value, null, null)
  constructor(name: String, file: File) : this(name, file.name, file, null)
  constructor(file: File) : this(file.name, file)
}

/**
 * Referer > Set headers > Set getCookies > Default headers > Default Cookies
 */

fun getHeaders(
  headers: Map<String, String>,
  referer: String?,
  cookie: Map<String, String>
): Headers {
  val refererMap = referer?.let { mapOf("referer" to it) } ?: mapOf()
  val cookieMap =
    if (cookie.isNotEmpty()) mapOf(
      "Cookie" to cookie.entries.joinToString(" ") {
        "${it.key}=${it.value};"
      }) else mapOf()
  val tempHeaders = (headers + cookieMap + refererMap)
  return tempHeaders.toHeaders()
}

// https://stackoverflow.com/a/59322754
fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
  val naiveTrustManager = @Suppress("CustomX509TrustManager")
  object : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
  }

  val insecureSocketFactory = SSLContext.getInstance("SSL").apply {
    val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
    init(null, trustAllCerts, SecureRandom())
  }.socketFactory

  sslSocketFactory(insecureSocketFactory, naiveTrustManager)
  hostnameVerifier { _, _ -> true }
  return this
}

fun Headers.getCookies(cookieKey: String): Map<String, String> {
  // Get a list of cookie strings
  // set-cookie: name=value;
  // set-cookie: name2=value2;
  // ---->
  // [name=value, name2=value2]
  val cookieList =
    this.filter { it.first.equals(cookieKey, ignoreCase = true) }.map {
      it.second.substringBefore(";")
    }

  // [name=value, name2=value2] -----> mapOf(name to value, name2 to value2)
  return cookieList.associate {
    val split = it.split("=")
    (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
  }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
}


//Provides async-able Calls
class ContinuationCallback(
  private val call: Call,
  private val continuation: CancellableContinuation<Response>
) : Callback, CompletionHandler {

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun onResponse(call: Call, response: Response) {
    continuation.resume(response, null)
  }

  override fun onFailure(call: Call, e: IOException) {
    // Cannot throw exception on SocketException since that can lead to un-catchable crashes
    // when you exit an activity as a request
    println("Exception in NiceHttp: ${e.javaClass.name} ${e.message}")
    if (call.isCanceled()) {
      // Must be able to throw errors, for example timeouts
      if (e is InterruptedIOException)
        continuation.cancel(e)
      else
        e.printStackTrace()
    } else {
      continuation.resumeWithException(e)
    }
  }

  override fun invoke(cause: Throwable?) {
    try {
      call.cancel()
    } catch (_: Throwable) {
    }
  }
}


// https://github.com, id=test -> https://github.com?id=test
internal fun appendUri(uri: String, appendQuery: String): String {
  val oldUri = URI(uri)
  return URI(
    oldUri.scheme,
    oldUri.authority,
    oldUri.path,
    if (oldUri.query == null) appendQuery else oldUri.query + "&" + appendQuery,
    oldUri.fragment
  ).toString()
}

// Can probably be done recursively
internal fun addParamsToUrl(url: String, params: Map<String, String?>): String {
  var appendedUrl = url
  params.forEach {
    it.value?.let { value ->
      appendedUrl = appendUri(appendedUrl, "${it.key}=${value}")
    }
  }
  return appendedUrl
}

internal fun getCache(cacheTime: Int, cacheUnit: TimeUnit): CacheControl {
  return CacheControl.Builder()
    .maxAge(cacheTime, cacheUnit)
    .build()
}
