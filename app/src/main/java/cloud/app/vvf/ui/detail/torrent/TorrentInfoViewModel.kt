package cloud.app.vvf.ui.detail.torrent

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.features.player.torrent.TorrentManager
import cloud.app.vvf.network.api.torrentserver.TorrentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TorrentInfoViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<List<Extension<*>>?>,
  val updateUIFlow: MutableStateFlow<AVPMediaItem?>,
  val torrentManager: TorrentManager,
  val application: Application,
  private val dataFlow: MutableStateFlow<AppDataStore>,
) : CatchingViewModel(throwableFlow) {

  private val _torrentStatus = MutableLiveData<TorrentStatus?>()
  val torrentStatus: LiveData<TorrentStatus?> = _torrentStatus

  private val _torrentInfo = MutableStateFlow<TorrentInfo?>(null)
  val torrentInfo: MutableStateFlow<TorrentInfo?> = _torrentInfo

  private val _isLoading = MutableLiveData<Boolean>()
  val isLoading: LiveData<Boolean> = _isLoading

  fun loadTorrentInfo(uri: Uri) {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        Timber.d("Loading torrent info from uri: $uri")
        val file = when (uri.scheme?.lowercase()) {
          "file" -> File(uri.path ?: "")
          "content" -> {
            // Handle content uri: copy to temp file
            try {
              val inputStream = application.contentResolver.openInputStream(uri)
              if (inputStream != null) {
                val tempFile = File.createTempFile("torrent_", ".torrent", application.cacheDir)
                tempFile.outputStream().use { output ->
                  inputStream.copyTo(output)
                }
                tempFile
              } else null
            } catch (e: Exception) {
              Timber.e(e, "Failed to copy content uri to temp file: $uri")
              null
            }
          }
          else -> null // TODO: handle other uri schemes if needed
        }
        if (file != null && file.exists()) {
          val torrentInfo = BencodeParser().parseTorrent(file.absolutePath)
          _torrentInfo.value = torrentInfo
          Timber.d("Successfully decoded torrent info: ${torrentInfo.name}")
        } else {
          Timber.w("File not found for uri: $uri")
          _torrentInfo.value = null
        }
      } catch (e: Exception) {
        Timber.e(e, "Error loading torrent info")
        throwableFlow.tryEmit(e)
        _torrentInfo.value = null
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun loadTorrentStatus(hash: String) {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        Timber.d("Loading torrent status for hash: $hash")

        val status = torrentManager.get(hash)
        _torrentStatus.value = status

        if (status != null) {
          Timber.d("Successfully loaded torrent status: ${status.name}")
        } else {
          Timber.w("No torrent found for hash: $hash")
        }
      } catch (e: Exception) {
        Timber.e(e, "Error loading torrent status")
        throwableFlow.tryEmit(e)
        _torrentStatus.value = null
      } finally {
        _isLoading.value = false
      }
    }
  }
}
