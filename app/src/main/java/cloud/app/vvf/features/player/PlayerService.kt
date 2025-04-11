package cloud.app.vvf.features.player

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Service

class PlayerService : MediaSessionService(){
  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    TODO("Not yet implemented")
  }
}
