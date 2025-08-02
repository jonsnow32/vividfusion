package cloud.app.vvf.ui.main.networkstream

import androidx.lifecycle.ViewModel
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.datastore.app.helper.UriHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class NetworkStreamViewModel @Inject constructor(
  val throwableFlow: MutableSharedFlow<Throwable>,
  val dataFlow: MutableStateFlow<AppDataStore>
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
}
