package cloud.app.vvf.ui.main.networkstream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.DownloadItem
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.datastore.app.helper.UriHistoryItem
import cloud.app.vvf.services.downloader.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NetworkStreamViewModel @Inject constructor(
  val throwableFlow: MutableSharedFlow<Throwable>,
  val dataFlow: MutableStateFlow<AppDataStore>,
  private val downloadManager: DownloadManager
) : ViewModel() {

  private val _streamUris = MutableStateFlow<List<UriHistoryItem>?>(null)
  val streamUris get() = _streamUris

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

  fun addToDownloadQueue(uri: String) {
    viewModelScope.launch {
      try {
        // Create a Video and MediaItem from the URI
        val video = Video.RemoteVideo(uri = uri, title = getFileNameFromUri(uri))
        val mediaItem = AVPMediaItem.VideoItem(video)

        // Start download with DownloadManager (this will create and save the DownloadItem internally)
        val downloadId = downloadManager.startDownload(
          mediaItem = mediaItem,
          downloadUrl = uri,
          quality = "default"
        )

        Timber.d("Added to download queue: $uri with ID: $downloadId")
      } catch (e: Exception) {
        Timber.e(e, "Failed to add to download queue: $uri")
        throwableFlow.emit(e)
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
