package cloud.app.vvf.services.downloader.helper

import okhttp3.*
import okio.*
import timber.log.Timber
import java.io.IOException

class HttpDownloadClient {

  fun createClient(progressCallback: (Long, Long) -> Unit): OkHttpClient {
    return OkHttpClient.Builder()
      .addNetworkInterceptor(ProgressInterceptor(progressCallback))
      .build()
  }

  fun createRequest(url: String, resumeBytes: Long = 0L): Request {
    val requestBuilder = Request.Builder().url(url)

    if (resumeBytes > 0) {
      requestBuilder.addHeader("Range", "bytes=$resumeBytes-")
      Timber.d("Adding Range header: bytes=$resumeBytes-")
    }

    return requestBuilder.build()
  }

  fun validateResponse(response: Response, isResuming: Boolean): Boolean {
    if (!response.isSuccessful) {
      if (isResuming && response.code == 416) {
        Timber.w("Range not satisfiable, file might be complete")
        return false
      }
      throw IOException("HTTP ${response.code}")
    }
    return true
  }

  private class ProgressInterceptor(
    private val listener: (Long, Long) -> Unit
  ) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
      val originalResponse = chain.proceed(chain.request())
      return originalResponse.newBuilder()
        .body(ProgressResponseBody(originalResponse.body, listener))
        .build()
    }
  }

  private class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val listener: (Long, Long) -> Unit
  ) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = responseBody.contentType()
    override fun contentLength(): Long = responseBody.contentLength()

    override fun source(): BufferedSource {
      if (bufferedSource == null) {
        bufferedSource = source(responseBody.source()).buffer()
      }
      return bufferedSource!!
    }

    private fun source(source: Source): Source {
      return object : ForwardingSource(source) {
        private var totalBytesRead = 0L

        override fun read(sink: Buffer, byteCount: Long): Long {
          val bytesRead = super.read(sink, byteCount)
          totalBytesRead += if (bytesRead != -1L) bytesRead else 0
          listener(totalBytesRead, responseBody.contentLength())
          return bytesRead
        }
      }
    }
  }
}
