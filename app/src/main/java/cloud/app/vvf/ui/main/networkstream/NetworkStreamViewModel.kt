package cloud.app.vvf.ui.main.networkstream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.datastore.app.helper.UriHistoryItem
import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.stateMachine.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NetworkStreamViewModel @Inject constructor(
  val throwableFlow: MutableSharedFlow<Throwable>,
  val dataFlow: MutableStateFlow<AppDataStore>,
  private val downloadManager: DownloadManager
) : ViewModel() {

  private val _streamUris = MutableStateFlow<List<UriHistoryItem>?>(null)
  val streamUris get() = _streamUris

  val downloads: StateFlow<Map<String, DownloadData>> = downloadManager.downloads

  fun saveToUriHistory(streamUrl: String) {
    dataFlow.value.saveUriHistory(UriHistoryItem(streamUrl))
  }

  fun clearUriHistory() {
    dataFlow.value.cleanUriHistory()
  }

  fun refresh() {
    _streamUris.value = dataFlow.value.getUriHistory()
  }

  fun deleteHistory(it: UriHistoryItem) {
    dataFlow.value.deleteUriHistory(it)
    refresh()
  }

  sealed class DownloadResult {
    object Success : DownloadResult()
    object AlreadyExists : DownloadResult()
    data class Error(val message: String) : DownloadResult()
  }

  suspend fun addToDownloadQueueWithResult(uri: String): DownloadResult {
    return try {
      // Check if download already exists before starting
      val existingDownload = downloadManager.downloads.value.values
        .find { it.url == uri && (it.isActive() || it.isCompleted()) }

      if (existingDownload != null) {
        Timber.d("Download already exists for URI: $uri with status: ${existingDownload.status}")
        return when {
          existingDownload.isCompleted() -> DownloadResult.AlreadyExists
          existingDownload.isActive() -> DownloadResult.AlreadyExists
          else -> {
            // If it's failed or cancelled, we can retry
            val video = Video.RemoteVideo(uri = uri, title = getFileNameFromUri(uri))
            val mediaItem = AVPMediaItem.VideoItem(video)
            downloadManager.startDownload(mediaItem, uri, "default")
            DownloadResult.Success
          }
        }
      }

      // Create a Video and MediaItem from the URI
      val video = Video.RemoteVideo(uri = uri, title = getFileNameFromUri(uri))
      val mediaItem = AVPMediaItem.VideoItem(video)

      // Start download with DownloadManager
      val downloadId = downloadManager.startDownload(
        mediaItem = mediaItem,
        downloadUrl = uri,
        quality = "default"
      )

      Timber.d("Added to download queue: $uri with ID: $downloadId")
      DownloadResult.Success
    } catch (e: Exception) {
      Timber.e(e, "Failed to add to download queue: $uri")
      DownloadResult.Error(e.message ?: "Unknown error")
    }
  }

  // Keep the old method for backward compatibility but make it use the new one
  fun addToDownloadQueue(uri: String) {
    viewModelScope.launch {
      val result = addToDownloadQueueWithResult(uri)
      if (result is DownloadResult.Error) {
        throwableFlow.emit(Exception(result.message))
      }
    }
  }

  private fun getFileNameFromUri(uri: String): String {
    return try {
      val fileName = uri.substringAfterLast("/")
      if (fileName.isNotBlank() && fileName.contains(".")) {
        fileName
      } else {
        "download_${System.currentTimeMillis()}"
      }
    } catch (e: Exception) {
      "download_${System.currentTimeMillis()}"
    }
  }
}
