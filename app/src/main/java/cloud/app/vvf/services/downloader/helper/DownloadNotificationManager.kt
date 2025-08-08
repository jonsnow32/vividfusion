package cloud.app.vvf.services.downloader.helper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import cloud.app.vvf.R

class DownloadNotificationManager(private val context: Context) {

  companion object {
    private const val CHANNEL_ID = "media_download_channel_1"
    private const val CHANNEL_NAME = "media_download_channel"
    private const val CHANNEL_DESCRIPTION = "Media download notifications"
    private const val NOTIFICATION_ID = 187210
  }

  private val notificationBuilder by lazy {
    NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setOngoing(true)
      .setAutoCancel(false)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
  }

  init {
    setupNotificationChannel()
  }

  fun hasPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else true
  }

  fun createForegroundInfo(
    fileName: String,
    subtitle: String = "Downloading media...",
    progress: Int = 0
  ): ForegroundInfo {
    val notification = notificationBuilder
      .setContentTitle("Downloading: $fileName")
      .setContentText(subtitle)
      .setProgress(100, progress, progress == 0)
      .build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ForegroundInfo(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      )
    } else {
      ForegroundInfo(NOTIFICATION_ID, notification)
    }
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
      }

      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
      manager?.createNotificationChannel(channel)
    }
  }
}
