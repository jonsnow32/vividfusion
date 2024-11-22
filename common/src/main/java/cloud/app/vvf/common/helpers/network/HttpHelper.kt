package cloud.app.vvf.common.helpers.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
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

  var userAgent: String =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"

  suspend fun get(
    url: String,
    headers: Map<String, String>? = null,
    referer: String? = null,
    responseParser: ResponseParser? = null
  ): HttpResponse {
    val request = buildRequest(url, headers, referer) {
      get()
    }
    return executeRequest(request, responseParser)
  }

  suspend fun post(
    url: String,
    data: Map<String, String>? = null,
    json: Any? = null,
    headers: Map<String, String>? = null,
    referer: String? = null,
    responseParser: ResponseParser? = null
  ): HttpResponse {
    val body = createRequestBody(data, json, responseParser) ?: FormBody.Builder().build()
    val request = buildRequest(url, headers, referer) {
      post(body)
    }
    return executeRequest(request, responseParser)
  }

  fun loadCookieForRequest(url: String): String? {
    val httpUrl = url.toHttpUrlOrNull() ?: return null
    return okHttpClient.cookieJar.loadForRequest(httpUrl)
      .joinToString(";") { "${it.name}=${it.value}" }
  }

  fun saveCookieFromResponse(url: String, cookieStr: String) {
    val httpUrl = url.ensureTrailingSlash().toHttpUrlOrNull() ?: return
    val cookies = cookieStr.split("|||").mapNotNull { Cookie.parse(httpUrl, it) }
    okHttpClient.cookieJar.saveFromResponse(httpUrl, cookies)
  }

  private fun buildRequest(
    url: String,
    headers: Map<String, String>?,
    referer: String?,
    methodSetup: Request.Builder.() -> Unit
  ): Request {
    return Request.Builder()
      .url(url)
      .applyHeaders(headers)
      .applyReferer(referer)
      .apply(methodSetup)
      .build()
  }

  private fun executeRequest(request: Request, responseParser: ResponseParser?): HttpResponse {
    val response = okHttpClient.newCall(request).execute()
    return HttpResponse(response, responseParser)
  }

  private fun Request.Builder.applyHeaders(headers: Map<String, String>?): Request.Builder {
    headers?.toHeaders()?.let { this.headers(it) }
    if (headers.isNullOrEmpty() || headers["User-Agent"].isNullOrEmpty()) {
      addHeader("User-Agent", userAgent)
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
          is JsonObject, is JsonArray -> Json.encodeToString(json)
          is String -> json
          is JsonAsString -> json.string
          else -> responseParser?.writeValueAsString(json) ?: Json.encodeToString(json)
        }
        val mediaType = if (json is String) RequestBodyTypes.TEXT else RequestBodyTypes.JSON
        jsonString.toRequestBody(mediaType.toMediaTypeOrNull())
      }
      else -> null
    }
  }

  private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
}
