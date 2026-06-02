package cloud.app.vvf.features.player

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlayerService : MediaSessionService() {

  companion object {
    // Set by PlayerViewModel when the player is created, cleared when it's released.
    var mediaSession: MediaSession? = null
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  override fun onDestroy() {
    // The session lifecycle is managed by PlayerViewModel; we just stop the service.
    super.onDestroy()
  }
}
