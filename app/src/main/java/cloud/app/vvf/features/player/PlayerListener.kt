package cloud.app.vvf.features.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

@UnstableApi
class PlayerListener(val viewModel: PlayerViewModel) : Player.Listener {
  private val scope = CoroutineScope(Dispatchers.Main + Job())

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

    super.onPositionDiscontinuity(oldPosition, newPosition, reason)
    val oldMediaItem = oldPosition.mediaItem ?: return

    when (reason) {
      DISCONTINUITY_REASON_SEEK,
      DISCONTINUITY_REASON_AUTO_TRANSITION,
        -> {
        val newMediaItem = newPosition.mediaItem
        if (newMediaItem != null && oldMediaItem != newMediaItem) {
          viewModel.playbackPositionMs.value =
            oldPosition.positionMs.takeIf { reason == DISCONTINUITY_REASON_SEEK } ?: C.TIME_UNSET
        }
      }

      DISCONTINUITY_REASON_REMOVE -> {
        viewModel.playbackPositionMs.value = oldPosition.positionMs
      }

      else -> return
    }

  }


  override fun onPlaybackStateChanged(playbackState: Int) {
    viewModel.playbackState.value = playbackState
    super.onPlaybackStateChanged(playbackState)
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

    if (tracks.groups.isNotEmpty()) {
      viewModel.tracks.value = tracks
      viewModel.audioTrackIdx.value?.let {
        viewModel.player?.switchTrack(C.TRACK_TYPE_AUDIO, it)
      }
      viewModel.textTrackIdx.value?.let {
        viewModel.player?.switchTrack(C.TRACK_TYPE_TEXT, it)
      }
      viewModel.videoTrackIdx.value?.let {
        viewModel.player?.switchTrack(C.TRACK_TYPE_VIDEO, it)
      }
    }
  }



  override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    super.onMediaItemTransition(mediaItem, reason)
    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
      viewModel.pause()
      return
    }
    if (mediaItem != null) {
      viewModel.playbackPositionMs.value.let {
        viewModel.seekTo(it)
      }
    }
  }

  // Clean up coroutines when the listener is no longer needed
  fun release() {
    scope.coroutineContext.cancel()
  }
}
