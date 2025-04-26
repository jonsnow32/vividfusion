package cloud.app.vvf.features.player

import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerListener(val viewModel: PlayerViewModel) : Player.Listener {
  private val scope = CoroutineScope(Dispatchers.Main + Job())
  private var updateJob: Job? = null
  private val delay = 500L

  override fun onVideoSizeChanged(videoSize: VideoSize) {
    if (videoSize.width != 0 && videoSize.height != 0) {
      viewModel.setOrientation(videoSize)
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
    updateProgress()
  }

  private fun updateProgress() {
    viewModel.player?.let { player ->
      viewModel.playbackPosition.value = player.currentPosition
      updateJob?.cancel() // Cancel any existing job
      if (player.playWhenReady && player.playbackState == ExoPlayer.STATE_READY) {
        updateJob = scope.launch {
          while (isActive) {
            viewModel.playbackPosition.value = player.currentPosition
            delay(delay)
          }
        }
      }
    }
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    viewModel.playbackState.value = playbackState
    super.onPlaybackStateChanged(playbackState)
    updateProgress()
  }

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
    super.onMediaMetadataChanged(mediaMetadata)
    viewModel.mediaMetaData.value = mediaMetadata
  }

  override fun onPlayerError(error: PlaybackException) {
    viewModel.playbackException.value = error
    super.onPlayerError(error)
  }

  override fun onTracksChanged(tracks: Tracks) {
    super.onTracksChanged(tracks)
    viewModel.tracks.value = tracks
  }

  // Clean up coroutines when the listener is no longer needed
  fun release() {
    scope.coroutineContext.cancel()
  }
}
