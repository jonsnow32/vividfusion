package cloud.app.common.helpers.network


import android.util.Log
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
  /**
   * Parse Json based on response text and the type T from parsed<T>()
   * This function can throw errors.
   * */
  fun <T : Any> parse(text: String, kClass: KClass<T>): T

  /**
   * Same as parse() but when overridden use try catch and return null on failure.
   * */
  fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T?

  /**
   * Used internally to sterilize objects to json in the data parameter.
   * requests.get(json = obj)
   * */
  fun writeValueAsString(obj: Any): String
}

data class JsonAsString(val string: String)

object RequestBodyTypes {
  const val JSON = "application/json;charset=utf-8"
  const val TEXT = "text/plain;charset=utf-8"
}


class HttpHelper (val okHttpClient: OkHttpClient) {
  var USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"
  suspend inline fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
      val callback = ContinuationCallback(this, continuation)
      enqueue(callback)
      continuation.invokeOnCancellation(callback)
    }
  }


  suspend fun get(url: String, headers: Map<String, String>? = mapOf(),referer: String? = null ,responseParser: ResponseParser? = null): HttpResponse {
    val requestBuilder = Request.Builder().url(url)
    if (headers != null) {
      requestBuilder.headers(headers.toHeaders())
      if (headers.get("User-Agent").isNullOrEmpty() && headers.get("user-agent")
          .isNullOrEmpty()
      ) {
        requestBuilder.addHeader("User-Agent" , USER_AGENT);
      }
    }
    referer?.let {
      requestBuilder.addHeader("referer", it)
    }
    val request = requestBuilder.get().build()
    val response = okHttpClient .newCall(request).await()
    return HttpResponse(response, responseParser)
  }


  suspend fun post(url: String, data: Map<String, String>? = emptyMap(),json: Any? = null, headers: Map<String, String>? = mapOf(),referer: String? = null , responseParser: ResponseParser? = null): HttpResponse {
    val requestBuilder = Request.Builder().url(url)
    if (headers != null) {
      requestBuilder.headers(headers.toHeaders())
      if (headers.get("User-Agent").isNullOrEmpty() && headers.get("user-agent")
          .isNullOrEmpty()
      ) {
        requestBuilder.addHeader("User-Agent" , USER_AGENT);
      }
    }
    referer?.let {
      requestBuilder.addHeader("Referer", it)
    }
    val body = if (!data.isNullOrEmpty() ) {

      val builder = FormBody.Builder()
      data.forEach {
        builder.addEncoded(it.key, it.value)
      }
      builder.build()

    } else if (json != null) {

      val jsonString = when {
        json is JSONObject -> json.toString()
        json is JSONArray -> json.toString()
        json is String -> json
        json is JsonAsString -> json.string
        (responseParser != null) -> responseParser.writeValueAsString(json)
        else -> json.toString()
      }

      val type = if (json is String) RequestBodyTypes.TEXT else RequestBodyTypes.JSON

      jsonString.toRequestBody(type.toMediaTypeOrNull())

    } else {
      null
    }
    val request = requestBuilder.post(body?:FormBody.Builder().build()).build()
    val response = okHttpClient.newCall(request).await()
    return HttpResponse(response, responseParser)
  }
  fun loadCookieForRequest(url: String): String? {
    val httpUrl: HttpUrl? = url.toHttpUrlOrNull()
    val stringBuilder = StringBuilder()
    if (httpUrl != null) {
      for (cookie in okHttpClient.cookieJar.loadForRequest(httpUrl)) {
        stringBuilder.append(cookie.name).append("=").append(cookie.value).append(";")
      }
    }
    return stringBuilder.toString()
  }

  fun saveCookieFromResponse(url: String, cookieStr: String) {
    var i = 0
    var newUrl = url;
    if (!newUrl.isEmpty() && !cookieStr.isEmpty()) {
      if (!newUrl.endsWith("/")) {
        newUrl += '/'
      }
      val httpUrl = newUrl.toHttpUrlOrNull()
      if (httpUrl != null) {
        val cookieJar = okHttpClient.cookieJar
        val split = if (cookieStr.contains("|||")) cookieStr.split("\\|\\|\\|".toRegex())
          .dropLastWhile { it.isEmpty() }
          .toTypedArray() else arrayOf(cookieStr)
        val length = split.size
        while (i < length) {
          val lcookie = Cookie.parse(httpUrl, split[i])
          if (lcookie != null) {
            var arrayList = ArrayList<Cookie>()
            arrayList.add(lcookie)
            cookieJar.saveFromResponse(httpUrl, arrayList)
          }
          i++
        }
      }
    }
  }
}
