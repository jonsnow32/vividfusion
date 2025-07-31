package cloud.app.vvf.features.player.torrent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.network.api.torrentserver.TorrentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class TorrentPlayerViewModel @Inject constructor(
  private val context: Context,
  private val torrentManager: TorrentManager,
) : ViewModel() {
  private val _torrentState = MutableStateFlow<TorrentPlayerState>(TorrentPlayerState.Idle)
  val torrentState: StateFlow<TorrentPlayerState> = _torrentState.asStateFlow()

  private val _downloadProgress = MutableStateFlow(0f)
  val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

  private val _bufferingState = MutableStateFlow(false)
  val bufferingState: StateFlow<Boolean> = _bufferingState.asStateFlow()

  private var currentTorrentHash: String? = null

  /**
   * Process any media item and return a new media item with a streamable URL if it's a torrent
   */
  suspend fun processMediaItem(mediaItem: AVPMediaItem): AVPMediaItem? {
    val url = when (mediaItem) {
      is AVPMediaItem.VideoItem -> mediaItem.video.uri
      is AVPMediaItem.TrackItem -> mediaItem.track.uri
      else -> null
    }
    return if (url != null && isTorrentUrl(url)) {
      try {
        _torrentState.value = TorrentPlayerState.AddingTorrent
        _bufferingState.value = true
        val (streamUrl, status) = torrentManager.transformLink(url, context.cacheDir)
        currentTorrentHash = status.hash
        _torrentState.value = TorrentPlayerState.Streaming
        Timber.i("Torrent ready for streaming: $streamUrl")
        // Return a copy of the mediaItem with the new url
        when (mediaItem) {
          is AVPMediaItem.VideoItem -> {
            mediaItem.video.uri = streamUrl
            mediaItem
          }
          is AVPMediaItem.TrackItem -> {
            mediaItem.track.uri = streamUrl
            mediaItem
          }
          else -> mediaItem
        }
      } catch (e: Exception) {
        _torrentState.value = TorrentPlayerState.Error("Failed to process torrent: ${e.message}")
        Timber.e(e, "Error processing torrent URL")
        null
      } finally {
        _bufferingState.value = false
      }
    } else {
      mediaItem
    }
  }


  /**
   * Remove current torrent
   */
  fun removeTorrent() {
    currentTorrentHash?.let { hash ->
      viewModelScope.launch {
        try {
          torrentManager.rem(hash)
          currentTorrentHash = null
          _torrentState.value = TorrentPlayerState.Idle
          _downloadProgress.value = 0f
        } catch (e: Exception) {
          Timber.e(e, "Error removing torrent")
        }
      }
    }
  }

  /**
   * Clean up resources
   */
  fun cleanup() {
    removeTorrent()
    viewModelScope.launch {
      try {
        torrentManager.clearAll()
        torrentManager.shutdown()
      } catch (e: Exception) {
        Timber.e(e, "Error during cleanup")
      }
    }
  }

  override fun onCleared() {
    cleanup()
    super.onCleared()
  }
//  /**
//   * Show consent dialog for torrent usage
//   */
//  fun showTorrentConsentIfNeeded(onConsent: () -> Unit, onDenied: () -> Unit = {}) {
//    if (torrentManager.hasAcceptedTorrentForThisSession == true) {
//      onConsent()
//    } else {
//      // For now, auto-accept. You can implement a proper dialog here
//      torrentManager.hasAcceptedTorrentForThisSession = true
//      onConsent()
//    }
//  }

  private fun isTorrentUrl(url: String): Boolean {
    return url.startsWith("magnet:") || url.endsWith(".torrent", ignoreCase = true)
  }
}

sealed class TorrentPlayerState {
  object Idle : TorrentPlayerState()
  object InitializingServer : TorrentPlayerState()
  object Ready : TorrentPlayerState()
  object AddingTorrent : TorrentPlayerState()
  object WaitingForMetadata : TorrentPlayerState()
  object Downloading : TorrentPlayerState()
  object Streaming : TorrentPlayerState()
  data class Error(val message: String) : TorrentPlayerState()
}

/**
 * Torrent state enum, matching typical torrent server states
 */
enum class TorrentState(val value: Int) {
  DownloadingMetadata(0),
  Downloading(1),
  Finished(2),
  Seeding(3),
  Error(4);

  companion object {
    fun fromInt(value: Int): TorrentState? = values().find { it.value == value }
  }
}
