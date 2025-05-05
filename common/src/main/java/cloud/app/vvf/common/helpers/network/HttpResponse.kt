package cloud.app.vvf.common.helpers.network

import cloud.app.vvf.common.utils.toData
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

val Response.cookies: Map<String, String>
    get() = this.headers.getCookies("set-cookie")

val Request.cookies: Map<String, String>
    get() = this.headers.getCookies("Cookie")


class HttpResponse(
    val okhttpResponse: Response,
) {
    /** Lazy, initialized on use. Returns empty string on null. Automatically closes the body! */
    val text by lazy {
        body.string().also {
            body.closeQuietly()
        }
    }
    val url by lazy { okhttpResponse.request.url.toString() }
    val cookies by lazy { okhttpResponse.cookies }

    /** Remember to close the body! */
    val body by lazy { okhttpResponse.body }

    /** Return code */
    val code = okhttpResponse.code
    val headers = okhttpResponse.headers

    /** Size, as reported by Content-Length */
    val size by lazy {
        (okhttpResponse.headers["Content-Length"]
            ?: okhttpResponse.headers["content-length"])?.toLongOrNull()
    }

    val isSuccessful = okhttpResponse.isSuccessful

    /** As parsed by Jsoup.parse(text) */
    val document: Document by lazy { Jsoup.parse(text) }

    /** Same as using mapper.readValue<T>() */
    inline fun <reified T : Any> parsed(): T {
        return this.text.toData<T>()
    }

    /** Same as using try { mapper.readValue<T>() } */
    inline fun <reified T : Any> parsedSafe(): T? {
        return try {
            return this.text.toData<T>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Only prints the return body */
    override fun toString(): String {
        return text
    }
}
