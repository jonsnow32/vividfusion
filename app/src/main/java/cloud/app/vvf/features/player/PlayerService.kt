package cloud.app.vvf.features.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlayerService : MediaSessionService() {

  companion object {
    var mediaSession: MediaSession? = null
    private const val CHANNEL_ID = "media_playback_channel"
    private const val CHANNEL_NAME = "Media Playback"
    private const val NOTIFICATION_ID = 101
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    setMediaNotificationProvider(
      DefaultMediaNotificationProvider.Builder(this)
        .setChannelId(CHANNEL_ID)
        .setNotificationId(NOTIFICATION_ID)
        .build()
    )
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  override fun onDestroy() {
    super.onDestroy()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Shows media playback controls"
        setSound(null, null)
        enableVibration(false)
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }
}
