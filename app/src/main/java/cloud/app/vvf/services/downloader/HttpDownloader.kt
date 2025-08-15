package cloud.app.vvf.services.downloader

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.app.vvf.R
import cloud.app.vvf.services.downloader.helper.DownloadFileManager
import cloud.app.vvf.services.downloader.helper.DownloadFileManager.Companion.uriToSlug
import cloud.app.vvf.services.downloader.helper.DownloadNotificationManager
import cloud.app.vvf.services.downloader.helper.HttpDownloadClient
import cloud.app.vvf.utils.KUniFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import okhttp3.ResponseBody
import timber.log.Timber
import javax.inject.Inject

@HiltWorker
class HttpDownloader @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

  @Inject lateinit var sharedPreferences: SharedPreferences

  companion object {
    const val KEY_DOWNLOAD_ID = "key_download_id"
    const val KEY_DOWNLOAD_URL = "key_download_url"
    const val KEY_DOWNLOAD_TYPE = "key_download_type"
    const val KEY_RESUME_PROGRESS = "resume_progress"
    const val KEY_RESUME_BYTES = "resume_bytes"
    const val KEY_RESUME_FROM_PAUSE = "resume_from_pause"
  }

  private val notificationManager = DownloadNotificationManager(context)
  private val fileManager = DownloadFileManager(context)
  private val httpClient = HttpDownloadClient()

  override suspend fun doWork(): Result {
    val downloadParams = extractDownloadParams()
    if (!downloadParams.isValid()) {
      return Result.failure(workDataOf("error" to "Missing required parameters"))
    }

    Timber.d("Starting download: ${downloadParams.downloadId} | ${downloadParams.downloadUrl} | Resume: ${downloadParams.isResuming}")

    // Set foreground with proper downloadId
    if (notificationManager.hasPermission()) {
      setForeground(
        notificationManager.createForegroundInfo(
          downloadParams.downloadId,
          downloadParams.downloadUrl,
          "Preparing download...",
          0
        )
      )
    }

    return try {
      val result = downloadFile(downloadParams)
      Result.success(result)
    } catch (e: Exception) {
      Timber.e(e, "HTTP download failed for ${downloadParams.downloadId}")
      val keys = DownloadData.Companion.Keys
      Result.failure(
        workDataOf(
          keys.ERROR to (e.message ?: "Unknown HTTP download error"),
          keys.DOWNLOAD_ID to downloadParams.downloadId
        )
      )
    }
  }

  private suspend fun downloadFile(params: HttpDownloadParams): Data {
    if (isStopped) {
      throw InterruptedException("Download was stopped")
    }

    // Create or get file for download
    val (mediaFile, _) = fileManager.createOrGetFile(
      params.downloadUrl.uriToSlug(),
      null,
      params.isResuming,
      params.resumeBytes
    )

    // Use parallel download
    var lastProgress = 0L
    val keys = DownloadData.Companion.Keys
    val threadCount = sharedPreferences.getInt(
      context.getString(R.string.download_batch_size),
      Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    )
    httpClient.downloadFileParallel(
      params.downloadUrl,
      mediaFile,
      threadCount = threadCount ,
      progressCallback = { downloaded, total ->
        if (isStopped) return@downloadFileParallel
        if (total > 0 && downloaded > 0 && downloaded != lastProgress) {
          lastProgress = downloaded
          val progress = ((downloaded * 100) / total).toInt()
          setProgressAsync(
            workDataOf(
              keys.PROGRESS to progress,
              keys.DOWNLOADED_BYTES to downloaded,
              keys.TOTAL_BYTES to total,
              keys.DOWNLOAD_ID to params.downloadId,
              keys.DISPLAY_NAME to (mediaFile.name ?: mediaFile.uri.toString())
            )
          )
        }
      }
    )

    val filename = mediaFile.name ?: mediaFile.uri.toString().substringAfterLast('/')
    notificationManager.showCompletionNotification(
      params.downloadId,
      filename,
      (mediaFile.uri.path ?: mediaFile.uri.toString()),
    )
    return workDataOf(
      keys.DOWNLOAD_ID to params.downloadId,
      keys.FILE_PATH to (mediaFile.uri.path ?: mediaFile.uri.toString()),
      keys.DISPLAY_NAME to filename,
      keys.FILE_PATH to mediaFile.uri.toString(),
      keys.FILE_SIZE to (mediaFile.length() ?: 0L),
      keys.DOWNLOADED_BYTES to (mediaFile.length() ?: 0L),
      keys.TOTAL_BYTES to (mediaFile.length() ?: 0L),
      keys.PROGRESS to 100,
    )
  }

  private var lastProgressUpdateTime = 0L
  private var lastDownloadedBytes = 0L

  private suspend fun updateProgress(
    downloadId: String,
    fileName: String,
    downloadedBytes: Long,
    totalBytes: Long
  ) {
    val now = System.currentTimeMillis()
    if (now - lastProgressUpdateTime < 1000) return // Only update once per second

    // Calculate download speed
    val timeDiff = now - lastProgressUpdateTime
    val bytesDiff = downloadedBytes - lastDownloadedBytes
    val downloadSpeed = if (timeDiff > 0) {
      (bytesDiff * 1000) / timeDiff // bytes per second
    } else 0L

    lastProgressUpdateTime = now
    lastDownloadedBytes = downloadedBytes

    val progress = if (totalBytes > 0) {
      ((downloadedBytes * 100) / totalBytes).toInt()
    } else 0

    val keys = DownloadData.Companion.Keys
    setProgressAsync(
      workDataOf(
        keys.PROGRESS to progress,
        keys.DOWNLOADED_BYTES to downloadedBytes,
        keys.TOTAL_BYTES to totalBytes,
        keys.DOWNLOAD_SPEED to downloadSpeed,
        keys.DOWNLOAD_ID to downloadId,
        keys.DISPLAY_NAME to fileName
      )
    )

    // Update notification using optimized notification manager
    notificationManager.updateNotification(
      this,
      downloadId,
      fileName,
      progress,
      DownloadStatus.DOWNLOADING,
      "Downloading... $progress% • ${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}"
    )
  }

  private suspend fun updateFinalProgress(
    downloadId: String,
    fileName: String,
    filePath: String,
    localPath: String,
    fileSize: Long
  ) {
    val keys = DownloadData.Companion.Keys
    setProgressAsync(
      workDataOf(
        keys.PROGRESS to 100,
        keys.DOWNLOADED_BYTES to fileSize,
        keys.TOTAL_BYTES to fileSize,
        keys.DOWNLOAD_ID to downloadId,
        keys.DISPLAY_NAME to fileName,
        keys.FILE_PATH to localPath,
        keys.FILE_PATH to filePath,
        keys.FILE_SIZE to fileSize
      )
    )

    // Show completion notification
    notificationManager.showCompletionNotification(downloadId, fileName, localPath)
  }

  private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return String.format(
      java.util.Locale.getDefault(),
      "%.1f %s",
      bytes / Math.pow(1024.0, digitGroups.toDouble()),
      units[digitGroups]
    )
  }

  private fun extractDownloadParams(): HttpDownloadParams {
    return HttpDownloadParams(
      downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: "",
      downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: "",
      downloadType = inputData.getString(KEY_DOWNLOAD_TYPE) ?: "HTTP",
      resumeProgress = inputData.getInt(KEY_RESUME_PROGRESS, 0),
      resumeBytes = inputData.getLong(KEY_RESUME_BYTES, 0L),
      isResuming = inputData.getBoolean(KEY_RESUME_FROM_PAUSE, false)
    )
  }

  data class HttpDownloadParams(
    val downloadId: String,
    val downloadUrl: String,
    val downloadType: String,
    val resumeProgress: Int,
    val resumeBytes: Long,
    val isResuming: Boolean
  ) {
    fun isValid(): Boolean = downloadId.isNotEmpty() && downloadUrl.isNotEmpty()
  }
}
