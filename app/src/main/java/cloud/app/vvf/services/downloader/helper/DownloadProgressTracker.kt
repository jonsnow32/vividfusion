package cloud.app.vvf.services.downloader.helper

import androidx.work.CoroutineWorker
import androidx.work.workDataOf

class DownloadProgressTracker(
  private val worker: CoroutineWorker,
  private val notificationManager: DownloadNotificationManager
) {

  private var lastProgressUpdate = 0
  private var lastNotificationUpdate = 0L
  private val notificationUpdateInterval = 2000L // 2 seconds

  suspend fun updateProgress(
    downloadId: String,
    fileName: String,
    downloadedBytes: Long,
    totalBytes: Long,
    resumeBytes: Long = 0L
  ) {
    val actualDownloaded = if (resumeBytes > 0) resumeBytes + downloadedBytes else downloadedBytes
    val actualTotal = if (resumeBytes > 0 && totalBytes > 0) resumeBytes + totalBytes else totalBytes

    val progress = if (actualTotal > 0) {
      (actualDownloaded * 100 / actualTotal).toInt()
    } else 0

    // Only update if progress changed significantly
    if (progress != lastProgressUpdate) {
      lastProgressUpdate = progress

      // Update WorkManager progress
      worker.setProgressAsync(
        workDataOf(
          "progress" to progress,
          "downloadedBytes" to actualDownloaded,
          "totalBytes" to actualTotal,
          "downloadId" to downloadId,
          "fileName" to fileName
        )
      )

      // Update notification less frequently
      updateNotificationIfNeeded(fileName, progress)
    }
  }

  fun updateFinalProgress(
    downloadId: String,
    fileName: String,
    filePath: String,
    localPath: String,
    fileSize: Long
  ) {
    worker.setProgressAsync(
      workDataOf(
        "progress" to 100,
        "downloadedBytes" to fileSize,
        "totalBytes" to fileSize,
        "downloadId" to downloadId,
        "filePath" to filePath,
        "localPath" to localPath,
        "fileName" to fileName
      )
    )
  }

  private suspend fun updateNotificationIfNeeded(fileName: String, progress: Int) {
    val currentTime = System.currentTimeMillis()

    if (notificationManager.hasPermission() &&
        (currentTime - lastNotificationUpdate > notificationUpdateInterval || progress % 5 == 0)) {
      lastNotificationUpdate = currentTime

      worker.setForegroundAsync(
        notificationManager.createForegroundInfo(
          fileName,
          "Downloading...",
          progress
        )
      )
    }
  }
}
