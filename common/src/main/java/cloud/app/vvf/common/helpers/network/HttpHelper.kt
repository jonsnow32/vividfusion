package cloud.app.vvf.common.helpers.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KClass

interface ResponseParser {
  fun <T : Any> parse(text: String, kClass: KClass<T>): T
  fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T?
  fun writeValueAsString(obj: Any): String
}

data class JsonAsString(val string: String)

object RequestBodyTypes {
  const val JSON = "application/json;charset=utf-8"
  const val TEXT = "text/plain;charset=utf-8"
}

class HttpHelper(val okHttpClient: OkHttpClient) {

  var USER_AGENT: String =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"

  suspend inline fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
      val callback = ContinuationCallback(this, continuation)
      enqueue(callback)
      continuation.invokeOnCancellation(callback)
    }
  }

  suspend fun get(
    url: String,
    headers: Map<String, String>? = emptyMap(),
    referer: String? = null,
    responseParser: ResponseParser? = null
  ): HttpResponse {
    val request = Request.Builder()
      .url(url)
      .applyHeaders(headers)
      .applyReferer(referer)
      .get()
      .build()

    val response = okHttpClient.newCall(request).await()
    return HttpResponse(response, responseParser)
  }

  suspend fun post(
    url: String,
    data: Map<String, String>? = emptyMap(),
    json: Any? = null,
    headers: Map<String, String>? = emptyMap(),
    referer: String? = null,
    responseParser: ResponseParser? = null
  ): HttpResponse {
    val body = createRequestBody(data, json, responseParser)
    val request = Request.Builder()
      .url(url)
      .applyHeaders(headers)
      .applyReferer(referer)
      .post(body ?: FormBody.Builder().build())
      .build()

    val response = okHttpClient.newCall(request).await()
    return HttpResponse(response, responseParser)
  }

  fun loadCookieForRequest(url: String): String? {
    val httpUrl = url.toHttpUrlOrNull() ?: return null
    return okHttpClient.cookieJar.loadForRequest(httpUrl).joinToString(";") { "${it.name}=${it.value}" }
  }

  fun saveCookieFromResponse(url: String, cookieStr: String) {
    val httpUrl = url.ensureTrailingSlash().toHttpUrlOrNull() ?: return
    val cookies = cookieStr.split("|||").mapNotNull { Cookie.parse(httpUrl, it) }
    okHttpClient.cookieJar.saveFromResponse(httpUrl, cookies)
  }

  private fun Request.Builder.applyHeaders(headers: Map<String, String>?): Request.Builder {
    headers?.toHeaders()?.let { this.headers(it) }
    if (headers.isNullOrEmpty() || headers["User-Agent"].isNullOrEmpty()) {
      addHeader("User-Agent", USER_AGENT)
    }
    return this
  }

  private fun Request.Builder.applyReferer(referer: String?): Request.Builder {
    referer?.let { addHeader("Referer", it) }
    return this
  }

  private fun createRequestBody(
    data: Map<String, String>?,
    json: Any?,
    responseParser: ResponseParser?
  ): RequestBody? {
    return when {
      !data.isNullOrEmpty() -> FormBody.Builder().apply {
        data.forEach { (key, value) -> addEncoded(key, value) }
      }.build()
      json != null -> {
        val jsonString = when (json) {
          is JSONObject, is JSONArray, is String, is JsonAsString -> json.toString()
          else -> responseParser?.writeValueAsString(json) ?: json.toString()
        }
        val mediaType = if (json is String) RequestBodyTypes.TEXT else RequestBodyTypes.JSON
        jsonString.toRequestBody(mediaType.toMediaTypeOrNull())
      }
      else -> null
    }
  }

  private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
}
