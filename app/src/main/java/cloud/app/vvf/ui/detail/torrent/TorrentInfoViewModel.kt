package cloud.app.vvf.ui.detail.torrent

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
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
        val file = when (uri.scheme?.lowercase()) {
          "file" -> File(uri.path ?: "")
          "content" -> {
            application.contentResolver.openInputStream(uri)?.use { input ->
              val tempFile = File.createTempFile("torrent_", ".torrent", application.cacheDir)
              tempFile.outputStream().use { input.copyTo(it) }
              tempFile
            }
          }
          else -> null
        }
        if (file != null && file.exists()) {
          _torrentInfo.value = BencodeParser().parseTorrent(file.absolutePath)
        } else {
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
        _torrentStatus.value = torrentManager.get(hash)
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
