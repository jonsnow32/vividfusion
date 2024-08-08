package cloud.app.avp.utils

import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import java.net.URL

object UrlUtils {
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

    fun toHashMap(headers: Headers): HashMap<String, String> {
        return HashMap(headers.toMap())
    }

    fun toHeaders(hashMap: HashMap<String, String>): Headers {
        return hashMap.toHeaders()
    }
    fun intToString(number: String): String {
        if (number.length == 1) {
           return "0$number"
        }
        return number;
    }

}
