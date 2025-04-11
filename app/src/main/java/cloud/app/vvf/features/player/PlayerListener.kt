package cloud.app.vvf.features.player

import androidx.media3.common.Player
import androidx.media3.common.VideoSize

class PlayerListener(val viewModel: PlayerViewModel, val fragment: PlayerFragment) :
  Player.Listener {
  override fun onVideoSizeChanged(videoSize: VideoSize) {
    if (videoSize.width != 0 && videoSize.height != 0) {
      fragment.setOrientation(videoSize)
    }
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    viewModel.isPlaying.value = isPlaying
  }

  override fun onPositionDiscontinuity(
    oldPosition: Player.PositionInfo,
    newPosition: Player.PositionInfo,
    reason: Int
  ) {
    viewModel.playbackPosition.value = newPosition.positionMs
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    viewModel.playbackState.value = playbackState
    super.onPlaybackStateChanged(playbackState)
  }
}
