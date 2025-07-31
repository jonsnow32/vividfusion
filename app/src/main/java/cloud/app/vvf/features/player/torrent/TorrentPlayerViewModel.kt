package cloud.app.vvf.features.player.torrent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.network.api.torrentserver.TorrentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
  private val _torrentStatus = MutableStateFlow<TorrentStatus?>(null)
  val torrentStatus: StateFlow<TorrentStatus?> = _torrentStatus.asStateFlow()

  private var currentTorrentHash: String? = null
  private var statusJob: Job? = null

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
        val (streamUrl, status) = torrentManager.transformLink(url, context.cacheDir)
        currentTorrentHash = status.hash
        startStatusPolling()
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
        Timber.e(e, "Error processing torrent URL")
        null
      }
    } else {
      mediaItem
    }
  }

  private fun startStatusPolling() {
    statusJob?.cancel()
    val hash = currentTorrentHash ?: return
    statusJob = viewModelScope.launch {
      while (true) {
        try {
          _torrentStatus.value = torrentManager.get(hash)
        } catch (e: Exception) {
          Timber.e(e, "Error polling torrent status")
        }
        delay(1000)
      }
    }
  }

  private fun stopStatusPolling() {
    statusJob?.cancel()
    statusJob = null
    _torrentStatus.value = null
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
          stopStatusPolling()
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
    stopStatusPolling()
  }

  override fun onCleared() {
    cleanup()
    super.onCleared()
  }


  private fun isTorrentUrl(url: String): Boolean {
    return url.startsWith("magnet:") || url.endsWith(".torrent", ignoreCase = true)
  }
}
