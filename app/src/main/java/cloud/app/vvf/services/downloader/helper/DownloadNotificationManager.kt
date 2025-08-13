package cloud.app.vvf.services.downloader.helper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import cloud.app.vvf.BuildConfig.AUTHORITY_FILE_PROVIDER
import cloud.app.vvf.R
import cloud.app.vvf.services.downloader.DownloadStatus
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadNotificationManager(private val context: Context) {

  companion object {
    private const val CHANNEL_ID = "media_download_channel_1"
    private const val CHANNEL_NAME = "Media Downloads"
    private const val CHANNEL_DESCRIPTION = "Media download notifications"
    private const val BASE_NOTIFICATION_ID = 187210
    private const val MAX_CONCURRENT_NOTIFICATIONS = 10
  }

  // Cache for notification IDs per download
  private val notificationIds = ConcurrentHashMap<String, Int>()
  private var nextNotificationId = BASE_NOTIFICATION_ID

  // Cache for notification manager
  private val notificationManager by lazy {
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  // Cache for permission check result with validity time
  private var permissionCheckTime = 0L
  private var cachedPermissionResult = false
  private val permissionCacheTimeout = 30_000L // 30 seconds

  // Optimized notification builder factory
  private fun createNotificationBuilder(): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_download_24)
      .setOngoing(true)
      .setAutoCancel(false)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOnlyAlertOnce(true) // Prevent repeated alerts
  }

  init {
    setupNotificationChannel()
  }

  fun hasPermission(): Boolean {
    val currentTime = System.currentTimeMillis()

    // Use cached result if still valid
    if (currentTime - permissionCheckTime < permissionCacheTimeout) {
      return cachedPermissionResult
    }

    cachedPermissionResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    permissionCheckTime = currentTime
    return cachedPermissionResult
  }

  /**
   * Get or create a unique notification ID for a download
   */
  private fun getNotificationId(downloadId: String): Int {
    return notificationIds.getOrPut(downloadId) {
      val id = nextNotificationId++
      // Prevent ID overflow and limit concurrent notifications
      if (notificationIds.size >= MAX_CONCURRENT_NOTIFICATIONS) {
        // Remove oldest entry
        val oldestEntry = notificationIds.entries.minByOrNull { it.value }
        oldestEntry?.let {
          notificationIds.remove(it.key)
          cancelNotification(it.value)
        }
      }
      id
    }
  }

  fun createForegroundInfo(
    downloadId: String,
    displayName: String,
    subtitle: String = "Downloading media...",
    progress: Int = 0,
    status: DownloadStatus = DownloadStatus.DOWNLOADING
  ): ForegroundInfo {
    val notificationId = getNotificationId(downloadId)
    val notification = createNotificationBuilder()
      .setContentTitle(getNotificationTitle(status, displayName))
      .setContentText(subtitle)
      .setProgress(100, progress, progress == 0)
      .addAction(createCancelAction(downloadId))
      .build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ForegroundInfo(
        notificationId,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      )
    } else {
      ForegroundInfo(notificationId, notification)
    }
  }

  // Overloaded method for backward compatibility
  fun createForegroundInfo(
    displayName: String,
    subtitle: String = "Downloading media...",
    progress: Int = 0
  ): ForegroundInfo {
    return createForegroundInfo("default", displayName, subtitle, progress)
  }

  private fun getNotificationTitle(status: DownloadStatus, displayName: String): String {
    return when (status) {
      DownloadStatus.DOWNLOADING -> "Downloading: $displayName"
      DownloadStatus.PAUSED -> "Paused: $displayName"
      DownloadStatus.COMPLETED -> "Completed: $displayName"
      DownloadStatus.FAILED -> "Failed: $displayName"
      DownloadStatus.CANCELLED -> "Cancelled: $displayName"
      else -> "Processing: $displayName"
    }
  }

  private fun createCancelAction(downloadId: String): NotificationCompat.Action {
    val cancelIntent = Intent("CANCEL_DOWNLOAD").apply {
      putExtra("download_id", downloadId)
    }
    val cancelPendingIntent = PendingIntent.getBroadcast(
      context,
      downloadId.hashCode(),
      cancelIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Action.Builder(
      R.drawable.ic_close,
      "Cancel",
      cancelPendingIntent
    ).build()
  }

  /**
   * Update notification with throttling to prevent excessive updates
   */
  private val lastUpdateTimes = ConcurrentHashMap<String, Long>()
  private val updateThrottleMs = 1000L // 1 second

  suspend fun updateNotification(
    worker: CoroutineWorker,
    downloadId: String,
    displayName: String,
    progress: Int,
    status: DownloadStatus = DownloadStatus.DOWNLOADING,
    subtitle: String = "Downloading... $progress%"
  ) {
    // Throttle updates
    val currentTime = System.currentTimeMillis()
    val lastUpdate = lastUpdateTimes[downloadId] ?: 0L
    if (currentTime - lastUpdate < updateThrottleMs && progress < 100) {
      return
    }
    lastUpdateTimes[downloadId] = currentTime

    if (hasPermission()) {
      try {
        worker.setForeground(
          createForegroundInfo(downloadId, displayName, subtitle, progress, status)
        )
      } catch (e: Exception) {
        Timber.w(e, "Failed to update notification for $downloadId")
      }
    }
  }

  // Overloaded method for backward compatibility
  suspend fun updateNotification(worker: CoroutineWorker, displayName: String, progress: Int) {
    updateNotification(worker, "default", displayName, progress)
  }

  fun showCompletionNotification(downloadId: String, displayName: String, filePath: String?) {
    if (!hasPermission()) return

    val notificationId = getNotificationId(downloadId)

    val openIntent = filePath?.let { path ->
      try {
        // Clean the file path - remove file:// prefix if present
        val cleanPath = if (path.startsWith("file://")) {
          path.removePrefix("file://")
        } else {
          path
        }

        val file = File(cleanPath)
        if (!file.exists()) {
          Timber.w("File does not exist: ${file.absolutePath}")
          return@let null
        }

        val uri = FileProvider.getUriForFile(
          context,
          AUTHORITY_FILE_PROVIDER,
          file
        )

        // Determine MIME type based on file extension
        val mimeType = when (file.extension.lowercase()) {
          "mp4", "mkv", "avi", "mov", "webm", "m4v" -> "video/*"
          "mp3", "m4a", "wav", "flac", "ogg" -> "audio/*"
          "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image/*"
          "pdf" -> "application/pdf"
          "txt" -> "text/plain"
          else -> "*/*"
        }

        Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(uri, mimeType)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      } catch (e: Exception) {
        Timber.e(e, "Failed to create file URI for notification: $path")
        null
      }
    }

    val openPendingIntent = openIntent?.let { intent ->
      PendingIntent.getActivity(
        context,
        downloadId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    }

    val notification = createNotificationBuilder()
      .setContentTitle("Download Complete")
      .setContentText(displayName)
      .setProgress(0, 0, false)
      .setOngoing(false)
      .setAutoCancel(true)
      .apply {
        openPendingIntent?.let { setContentIntent(it) }
        if (openPendingIntent != null) {
          addAction(R.drawable.ic_play_arrow_24, "Open", openPendingIntent)
        }
      }
      .build()

    try {
      notificationManager.notify(notificationId, notification)
    } catch (e: Exception) {
      Timber.w(e, "Failed to show completion notification for $downloadId")
    }
  }

  fun cancelNotification(downloadId: String) {
    val notificationId = notificationIds.remove(downloadId)
    notificationId?.let { cancelNotification(it) }
    lastUpdateTimes.remove(downloadId)
  }

  private fun cancelNotification(notificationId: Int) {
    try {
      notificationManager.cancel(notificationId)
    } catch (e: Exception) {
      Timber.w(e, "Failed to cancel notification $notificationId")
    }
  }

  fun cancelAllNotifications() {
    notificationIds.values.forEach { notificationId ->
      try {
        notificationManager.cancel(notificationId)
      } catch (e: Exception) {
        Timber.w(e, "Failed to cancel notification $notificationId")
      }
    }
    notificationIds.clear()
    lastUpdateTimes.clear()
  }

  private fun setupNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = CHANNEL_DESCRIPTION
        setSound(null, null)
        enableVibration(false)
        setShowBadge(false)
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
      }

      try {
        notificationManager.createNotificationChannel(channel)
      } catch (e: Exception) {
        Timber.w(e, "Failed to create notification channel")
      }
    }
  }
}
