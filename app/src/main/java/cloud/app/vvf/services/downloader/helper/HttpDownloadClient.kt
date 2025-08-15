package cloud.app.vvf.services.downloader.helper

import cloud.app.vvf.utils.KUniFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import okio.use
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class HttpDownloadClient {

  fun createClient(progressCallback: (Long, Long) -> Unit): OkHttpClient {
    return OkHttpClient.Builder()
      .addNetworkInterceptor(ProgressInterceptor(progressCallback))
      .build()
  }

  /**
   * Check if server supports range requests for chunked download
   */
  private suspend fun checkRangeSupport(url: String): RangeSupport = withContext(Dispatchers.IO) {
    val client = createClient { _, _ -> }

    try {
      // Method 1: Check HEAD request for Accept-Ranges header
      val headRequest = Request.Builder().url(url).head().build()
      val headResponse = client.newCall(headRequest).execute()

      val contentLength = headResponse.header("Content-Length")?.toLongOrNull()
      val acceptRanges = headResponse.header("Accept-Ranges")
      val etag = headResponse.header("ETag")
      val lastModified = headResponse.header("Last-Modified")

      headResponse.close()

      Timber.d("Range support check - Accept-Ranges: $acceptRanges, Content-Length: $contentLength")

      // Method 2: If no explicit Accept-Ranges header, test with a small range request
      if (acceptRanges == null && contentLength != null && contentLength > 1024) {
        val testRequest = Request.Builder()
          .url(url)
          .addHeader("Range", "bytes=0-1023") // Request first 1KB
          .build()

        val testResponse = client.newCall(testRequest).execute()
        val isPartialContent = testResponse.code == 206
        val contentRange = testResponse.header("Content-Range")

        testResponse.close()

        Timber.d("Range test request - Status: ${testResponse.code}, Content-Range: $contentRange")

        return@withContext RangeSupport(
          supportsRange = isPartialContent,
          totalSize = contentLength,
          etag = etag,
          lastModified = lastModified,
          method = if (isPartialContent) "test_request" else "no_support"
        )
      }

      // Method 3: Based on Accept-Ranges header
      val supportsRange = when (acceptRanges?.lowercase()) {
        "bytes" -> true
        "none" -> false
        null -> false // Assume no support if header is missing
        else -> false
      }

      return@withContext RangeSupport(
        supportsRange = supportsRange,
        totalSize = contentLength ?: -1L,
        etag = etag,
        lastModified = lastModified,
        method = "accept_ranges_header"
      )

    } catch (e: Exception) {
      Timber.w("Failed to check range support: ${e.message}")
      return@withContext RangeSupport(
        supportsRange = false,
        totalSize = -1L,
        etag = null,
        lastModified = null,
        method = "error"
      )
    }
  }

  /**
   * Data class to hold range support information
   */
  data class RangeSupport(
    val supportsRange: Boolean,
    val totalSize: Long,
    val etag: String? = null,
    val lastModified: String? = null,
    val method: String = "unknown"
  )

  /**
   * Download a file in parallel using multiple threads (coroutines).
   * Supports resume and safe recovery from interruptions.
   * @param url The file URL
   * @param outputFile The output KUniFile
   * @param threadCount Number of parallel threads (default: CPU cores)
   * @param progressCallback (downloadedBytes, totalBytes) -> Unit
   */
  suspend fun downloadFileParallel(
    url: String,
    outputFile: KUniFile,
    threadCount: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
    progressCallback: (Long, Long) -> Unit = { _, _ -> }
  ) = withContext(Dispatchers.IO) {
    // Check range support with improved detection
    val rangeSupport = checkRangeSupport(url)

    Timber.d("Range support result: ${rangeSupport.supportsRange} (method: ${rangeSupport.method})")

    if (!rangeSupport.supportsRange || threadCount == 1 || rangeSupport.totalSize < 1024 * 1024) {
      // Fallback to single-threaded download if:
      // - No range support
      // - Only 1 thread requested
      // - File too small (< 1MB) for chunking to be beneficial
      Timber.d("Using single-threaded download")
      downloadFileSingle(url, outputFile, progressCallback)
      return@withContext
    }

    val totalSize = rangeSupport.totalSize
    val chunkSize = totalSize / threadCount
    val progressTracker = AtomicLong(0)
    val tempDir = File(outputFile.uri.path).parentFile
    val baseName = outputFile.name ?: "download"

    Timber.d("Using parallel download with $threadCount threads, chunk size: $chunkSize")

    try {
      // Download chunks in parallel to temporary files
      val chunkFiles = (0 until threadCount).map { i ->
        val start = i * chunkSize
        val end = if (i == threadCount - 1) totalSize - 1 else (start + chunkSize - 1)
        val chunkFile = File(tempDir, "${baseName}.chunk$i.tmp")

        async {
          downloadChunk(url, chunkFile, start, end, progressTracker, totalSize, progressCallback)
          chunkFile
        }
      }

      val completedChunks = chunkFiles.awaitAll()

      // Merge chunks into final file
      mergeChunks(completedChunks, outputFile)

      // Cleanup temporary files
      completedChunks.forEach { it.delete() }

    } catch (e: Exception) {
      // Cleanup on error
      (0 until threadCount).forEach { i ->
        File(tempDir, "${baseName}.chunk$i.tmp").delete()
      }
      throw e
    }
  }

  /**
   * Download a single chunk with resume support
   */
  private suspend fun downloadChunk(
    url: String,
    chunkFile: File,
    start: Long,
    end: Long,
    progressTracker: AtomicLong,
    totalSize: Long,
    progressCallback: (Long, Long) -> Unit
  ) {
    var currentStart = start

    // Check if chunk already partially downloaded
    if (chunkFile.exists()) {
      currentStart = start + chunkFile.length()
      progressTracker.addAndGet(chunkFile.length())
    }

    if (currentStart > end) {
      return // Chunk already complete
    }

    val client = createClient { _, _ -> }
    var retryCount = 0
    val maxRetries = 3

    while (retryCount < maxRetries) {
      try {
        val request = Request.Builder()
          .url(url)
          .addHeader("Range", "bytes=$currentStart-$end")
          .addHeader("User-Agent", "VividFusion-Downloader/1.0")
          .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
          response.close()
          if (response.code == 416) {
            // Range not satisfiable - chunk might be complete
            return
          }
          throw IOException("HTTP ${response.code}")
        }

        response.body?.byteStream()?.use { inputStream ->
          chunkFile.outputStream().use { outputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
              outputStream.write(buffer, 0, bytesRead)
              val currentProgress = progressTracker.addAndGet(bytesRead.toLong())
              progressCallback(currentProgress, totalSize)
            }
          }
        }
        response.body?.close()
        response.close()
        return // Success

      } catch (e: Exception) {
        retryCount++
        if (retryCount >= maxRetries) {
          throw IOException("Failed to download chunk after $maxRetries retries: ${e.message}", e)
        }

        // Wait before retry with exponential backoff
        kotlinx.coroutines.delay(1000L * retryCount)
        Timber.w("Retrying chunk download ($retryCount/$maxRetries): ${e.message}")
      }
    }
  }

  /**
   * Fallback single-threaded download with resume support
   */
  private suspend fun downloadFileSingle(
    url: String,
    outputFile: KUniFile,
    progressCallback: (Long, Long) -> Unit
  ) {
    val existingSize = outputFile.length()
    val client = createClient { bytesRead, totalBytes ->
      progressCallback(existingSize + bytesRead, totalBytes + existingSize)
    }

    val request = createRequest(url, existingSize)
    val response = client.newCall(request).execute()

    if (!validateResponse(response, existingSize > 0)) {
      response.close()
      throw IOException("Invalid response for single download")
    }

    response.body.byteStream().use { inputStream ->
      outputFile.openOutputStream(existingSize > 0).use { outputStream ->
        val buffer = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          outputStream.write(buffer, 0, bytesRead)
        }
      }
    }
    response.body.close()
    response.close()
  }

  /**
   * Merge downloaded chunks into final file
   */
  private suspend fun mergeChunks(chunkFiles: List<File>, outputFile: KUniFile) {
    outputFile.openOutputStream(false).use { outputStream ->
      chunkFiles.forEach { chunkFile ->
        chunkFile.inputStream().use { inputStream ->
          val buffer = ByteArray(8192)
          var bytesRead: Int
          while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
          }
        }
      }
    }
  }

  fun createRequest(url: String, resumeBytes: Long = 0L, endByte: Long? = null): Request {
    val requestBuilder = Request.Builder()
      .url(url)
      .addHeader("User-Agent", "VividFusion-Downloader/1.0")
      .addHeader("Accept", "*/*")
      .addHeader("Connection", "keep-alive")

    if (resumeBytes > 0 || endByte != null) {
      val rangeHeader = if (endByte != null) {
        "bytes=$resumeBytes-$endByte"
      } else {
        "bytes=$resumeBytes-"
      }
      requestBuilder.addHeader("Range", rangeHeader)
      Timber.d("Adding Range header: $rangeHeader")
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
