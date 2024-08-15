package cloud.app.avp.network


import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
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


class HttpHelper @Inject constructor(val okHttpClient: OkHttpClient) {

  suspend inline fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
      val callback = ContinuationCallback(this, continuation)
      enqueue(callback)
      continuation.invokeOnCancellation(callback)
    }
  }


  suspend fun get(url: String, headers: Headers?, responseParser: ResponseParser?): HttpResponse? {
    val requestBuilder = Request.Builder().url(url)
    if (headers != null) {
      requestBuilder.headers(headers)
    }
    val request = requestBuilder.build()
    val response = okHttpClient.newCall(request).await()
    return HttpResponse(response, responseParser)
  }


  suspend fun post(url: String,data: Map<String, String>?,json: Any?, headers: Headers?, responseParser: ResponseParser?): HttpResponse? {
    val requestBuilder = Request.Builder().url(url)
    if (headers != null) {
      requestBuilder.headers(headers)
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


//  fun getHtml(url: String, referer: String): String? {
//    var headers: HashMap<String, String> = HashMap();
//    headers.put("Referer", referer)
//    return getHtml(url, headers)
//  }
//
//  fun getHtml(url: String): String? {
//    var headers: HashMap<String, String> = HashMap();
//    headers["User-Agent"] = USER_AGENT;
//    return getHtml(url, headers)
//  }
//
//  fun getHtml(url: String, headers: HashMap<String, String>): String? {
//    if (headers.get("User-Agent").isNullOrEmpty() && headers.get("user-agent")
//        .isNullOrEmpty()
//    ) {
//      headers["User-Agent"] = USER_AGENT;
//    }
//
//    return getHtml(url, headers.toHeaders())
//  }
//
//  fun getHtml(url: String, headers: Headers): String? {
//    return okHttpClient.newCall(Request.Builder().url(url).headers(headers).build())
//      .execute()?.body?.string()
//  }
//
//  fun getResponse(url: String, referer: String): Response? {
//    var headers: HashMap<String, String> = HashMap();
//    headers.put("Referer", referer)
//    return getResponse(url, headers)
//  }
//
//  fun getResponse(url: String, headers: HashMap<String, String>?): Response? {
//    var temp: HashMap<String, String> = HashMap();
//    if (headers.isNullOrEmpty())
//      temp["User-Agent"] = USER_AGENT;
//    else {
//      temp.putAll(headers);
//    }
//    if (temp.get("User-Agent").isNullOrEmpty() && temp.get("user-agent")
//        .isNullOrEmpty()
//    ) {
//      temp["User-Agent"] = USER_AGENT;
//    }
//    return okHttpClient.newCall(Request.Builder().url(url).headers(temp.toHeaders()).build())
//      .execute()
//  }
//
//  fun getRedirectUrl(url: String, headers: HashMap<String, String>?): String? {
//    var temp: HashMap<String, String> = HashMap();
//    if (headers.isNullOrEmpty())
//      temp["User-Agent"] = USER_AGENT;
//    else {
//      temp.putAll(headers);
//    }
//    if (temp.get("User-Agent").isNullOrEmpty() && temp.get("user-agent")
//        .isNullOrEmpty()
//    ) {
//      temp["User-Agent"] = USER_AGENT;
//    }
//    var response = okHttpClient.newBuilder()
//      .followRedirects(false)
//      .followSslRedirects(false)
//      .build()
//      .newCall(Request.Builder().url(url).headers(temp.toHeaders()).build())
//      .execute()
//    if (response.isRedirect) {
//      var location = response.headers["Location"]
//      if (location.isNullOrEmpty())
//        location = response.headers["location"]
//      if (location.isNullOrEmpty())
//        location = url
//      if (location.startsWith("/"))
//        location = UrlUtils.getBaseUrl(url) + location
//      return location
//    } else {
//      return response?.request?.url.toString();
//    }
//  }
//
//  fun getHtml(url: String, requestBody: RequestBody, headers: Headers): String? {
//    return okHttpClient.newCall(
//      Request.Builder().url(url).method("POST", requestBody).headers(headers).build()
//    ).execute()?.body?.string()
//  }
//
//  fun getHtml(url: String, body: String, headers: HashMap<String, String>): String? {
//    if (headers.get("User-Agent").isNullOrEmpty() && headers.get("user-agent")
//        .isNullOrEmpty()
//    ) {
//      headers["User-Agent"] = USER_AGENT;
//    }
//    val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
//
//    val requestBody = body.toRequestBody(mediaType)
//    return getHtml(url, requestBody, headers.toHeaders())
//  }
//
//  fun getResponseBody(url: String, headers: HashMap<String, String>): ResponseBody? {
//    if (headers["User-Agent"].isNullOrEmpty() && headers["user-agent"]
//        .isNullOrEmpty()
//    ) {
//      headers["User-Agent"] = USER_AGENT;
//    }
//    return okHttpClient.newCall(Request.Builder().url(url).headers(headers.toHeaders()).build())
//      .execute().body
//  }
//
//  fun getHtml(
//    url: String,
//    body: String,
//    use_content_type: Boolean,
//    headers: HashMap<String, String>
//  ): String? {
//    if (headers.get("User-Agent").isNullOrEmpty() && headers.get("user-agent")
//        .isNullOrEmpty()
//    ) {
//      headers["User-Agent"] = USER_AGENT;
//    }
//    var mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
//    if (use_content_type)
//      mediaType = "content-type".toMediaTypeOrNull()
//
//    val requestBody = body.toRequestBody(mediaType)
//    return getHtml(url, requestBody, headers.toHeaders())
//  }
//
//  fun loadCookieForRequest(url: String): String? {
//    val httpUrl: HttpUrl? = url.toHttpUrlOrNull()
//    val stringBuilder = StringBuilder()
//    if (httpUrl != null) {
//      for (cookie in okHttpClient.cookieJar.loadForRequest(httpUrl)) {
//        stringBuilder.append(cookie.name).append("=").append(cookie.value).append(";")
//      }
//    }
//    return stringBuilder.toString()
//  }
//
//  fun saveCookieFromResponse(url: String, cookieStr: String) {
//    var i = 0
//    var newUrl = url;
//    if (!newUrl.isEmpty() && !cookieStr.isEmpty()) {
//      if (!newUrl.endsWith("/")) {
//        newUrl += "/"
//      }
//      val httpUrl = newUrl.toHttpUrlOrNull()
//      if (httpUrl != null) {
//        val cookieJar = okHttpClient.cookieJar
//        val split = if (cookieStr.contains("|||")) cookieStr.split("\\|\\|\\|".toRegex())
//          .dropLastWhile { it.isEmpty() }
//          .toTypedArray() else arrayOf(cookieStr)
//        val length = split.size
//        while (i < length) {
//          val lcookie = Cookie.parse(httpUrl, split[i])
//          if (lcookie != null) {
//            var arrayList = ArrayList<Cookie>()
//            arrayList.add(lcookie)
//            cookieJar.saveFromResponse(httpUrl, arrayList)
//          }
//          i++
//        }
//      }
//    }
//  }


}
