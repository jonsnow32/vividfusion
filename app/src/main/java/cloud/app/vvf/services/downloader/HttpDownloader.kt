package cloud.app.vvf.services.downloader

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.app.vvf.services.downloader.helper.DownloadFileManager
import cloud.app.vvf.services.downloader.helper.DownloadNotificationManager
import cloud.app.vvf.services.downloader.helper.DownloadProgressTracker
import cloud.app.vvf.services.downloader.helper.HttpDownloadClient
import cloud.app.vvf.utils.KUniFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import timber.log.Timber

@HiltWorker
class HttpDownloader @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

  companion object {
    const val KEY_DOWNLOAD_ID = "key_download_id"
    const val KEY_DOWNLOAD_URL = "key_download_url"
    const val KEY_FILE_NAME = "key_file_name"
    const val KEY_DOWNLOAD_TYPE = "key_download_type"
    const val KEY_RESUME_PROGRESS = "resume_progress"
    const val KEY_RESUME_BYTES = "resume_bytes"
    const val KEY_RESUME_FROM_PAUSE = "resume_from_pause"
  }

  private val notificationManager = DownloadNotificationManager(context)
  private val fileManager = DownloadFileManager(context)
  private val httpClient = HttpDownloadClient()
  private lateinit var progressTracker: DownloadProgressTracker

  override suspend fun doWork(): Result {
    val downloadParams = extractDownloadParams()
    if (!downloadParams.isValid()) {
      return Result.failure(workDataOf("error" to "Missing required parameters"))
    }

    progressTracker = DownloadProgressTracker(this, notificationManager)

    Timber.Forest.d("Starting download: ${downloadParams.downloadId} | ${downloadParams.downloadUrl} | Resume: ${downloadParams.isResuming}")

    // Set foreground if notification permission available
    if (notificationManager.hasPermission()) {
      val statusText = if (downloadParams.isResuming) "Resuming download..." else "Starting download..."
      setForeground(notificationManager.createForegroundInfo(downloadParams.fileName, statusText, downloadParams.resumeProgress))
    }

    return try {
      downloadFile(downloadParams)
    } catch (e: Exception) {
      Timber.Forest.e(e, "Download failed for \\${downloadParams.downloadId}")
      val keys = DownloadData.Companion.Keys
      Result.failure(
        workDataOf(
          keys.ERROR to (e.message ?: "Unknown error"),
          keys.DOWNLOAD_ID to downloadParams.downloadId,
          keys.FILE_NAME to downloadParams.fileName
        )
      )
    }
  }

  private suspend fun downloadFile(params: HttpDownloadParams): Result {
    if (isStopped) {
      return Result.failure(workDataOf("error" to "Download was stopped"))
    }

    // Create HTTP client with progress callback
    val client = httpClient.createClient { downloadedBytes, totalBytes ->
      // Progress callback will be handled by the tracker
      runBlocking {
        progressTracker.updateProgress(
          params.downloadId,
          params.fileName,
          downloadedBytes,
          totalBytes,
          params.resumeBytes
        )
      }
    }

    // Create request with resume support
    val request = httpClient.createRequest(params.downloadUrl, params.resumeBytes)
    val response = client.newCall(request).execute()

    if (isStopped) {
      response.close()
      return Result.failure(workDataOf("error" to "Download was stopped"))
    }

    // Validate response
    if (!httpClient.validateResponse(response, params.isResuming)) {
      response.close()
      return Result.success(workDataOf(DownloadData.Companion.Keys.DOWNLOAD_ID to params.downloadId))
    }

    val responseBody = response.body

    // Create or get file for download
    val (mediaFile, shouldAppend) = fileManager.createOrGetFile(
      params.fileName,
      responseBody.contentType()?.toString(),
      params.isResuming,
      params.resumeBytes
    )

    return try {
      downloadToFile(responseBody, mediaFile, shouldAppend, params)
    } finally {
      responseBody.close()
    }
  }

  private suspend fun downloadToFile(
    responseBody: ResponseBody,
    mediaFile: KUniFile,
    shouldAppend: Boolean,
    params: HttpDownloadParams
  ): Result {
    var totalBytesWritten = 0L

    responseBody.byteStream().use { input ->
      mediaFile.openOutputStream(shouldAppend).use { output ->
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        var iterationCount = 0

        while (input.read(buffer).also { bytesRead = it } != -1) {
          // Check for cancellation periodically
          if (iterationCount % 100 == 0 && isStopped) {
            return Result.failure(workDataOf(DownloadData.Companion.Keys.ERROR to "Download was stopped"))
          }

          output.write(buffer, 0, bytesRead)
          totalBytesWritten += bytesRead
          iterationCount++
        }
        output.flush()
      }
    }

    val finalSize = mediaFile.length()
    val filePath = mediaFile.uri.toString()
    val localPath = mediaFile.uri.path ?: filePath

    // Final progress update
    progressTracker.updateFinalProgress(
      params.downloadId,
      params.fileName,
      filePath,
      localPath,
      finalSize
    )

    return Result.success(
      workDataOf(
        DownloadData.Companion.Keys.DOWNLOAD_ID to params.downloadId,
        DownloadData.Companion.Keys.FILE_PATH to filePath,
        DownloadData.Companion.Keys.LOCAL_PATH to localPath,
        DownloadData.Companion.Keys.FILE_SIZE to finalSize,
        DownloadData.Companion.Keys.FILE_NAME to params.fileName
      )
    )
  }

  private fun extractDownloadParams(): HttpDownloadParams {
    return HttpDownloadParams(
      downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: "",
      downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: "",
      fileName = inputData.getString(KEY_FILE_NAME) ?: "",
      downloadType = inputData.getString(KEY_DOWNLOAD_TYPE) ?: "HTTP",
      resumeProgress = inputData.getInt(KEY_RESUME_PROGRESS, 0),
      resumeBytes = inputData.getLong(KEY_RESUME_BYTES, 0L),
      isResuming = inputData.getBoolean(KEY_RESUME_FROM_PAUSE, false)
    )
  }

  data class HttpDownloadParams(
    val downloadId: String,
    val downloadUrl: String,
    val fileName: String,
    val downloadType: String,
    val resumeProgress: Int,
    val resumeBytes: Long,
    val isResuming: Boolean
  ) {
    fun isValid(): Boolean = downloadId.isNotEmpty() && downloadUrl.isNotEmpty() && fileName.isNotEmpty()
  }
}
