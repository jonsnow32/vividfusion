package cloud.app.vvf.common.helpers.network

import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import java.io.File
import java.net.URI
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
object Utils {

  fun getBaseUrl(urlStr: String): String {
    try {
      val url = URL(urlStr)
      return url.protocol + "://" + url.host
    } catch (e: Exception) {
      return urlStr
    }
  }

  fun getHost(urlStr: String): String {
    try {
      val url = URL(urlStr)
      return url.host
    } catch (e: Exception) {
      return ""
    }
  }

  fun fixEpisode(number: String): String {
    if (number.length == 1) {
      return "0$number"
    }
    return number;
  }
  fun replaceAllkeywithTarget(str: String, target: String?): String {
    var str = str
    str = str.replace("([ .-](?:and|AND|And|&)[ .-])".toRegex(), " ")
    return str.replace("'", "")
      .replace("(\\\\|\\/| -|:|\\(|\\)|;|-|\\.|\\*|\\?|\"|\\'|<|>|,)".toRegex(), " ")
      .replace("  ", " ").replace(
        " ",
        target!!
      )
  }
  fun URLEncoder(str: String): String {
    return try {
      java.net.URLEncoder.encode(str, "UTF-8")
    } catch (e2: Throwable) {
      str.replace(":", "%3A")
        .replace("/", "%2F").replace("#", "%23")
        .replace("?", "%3F").replace("&", "%24").replace("@", "%40").replace("%", "%25")
        .replace("+", "%2B").replace(" ", "+").replace(";", "%3B")
        .replace("=", "%3D").replace("$", "%26").replace(",", "%2C").replace("<", "%3C")
        .replace(">", "%3E").replace("~", "%25").replace("^", "%5E").replace("`", "%60")
        .replace("\\", "%5C").replace("[", "%5B").replace("]", "%5D").replace("{", "%7B")
        .replace("|", "%7C").replace("\"", "%22")
    }
  }
  fun getHashXMLHttpRequest(): HashMap<String, String> {
    val hashMap: HashMap<String, String> = HashMap<String, String>()
    hashMap["X-Requested-With"] = "XMLHttpRequest"
    return hashMap
  }
}
