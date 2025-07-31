package cloud.app.vvf.ui.main.networkstream

import androidx.lifecycle.ViewModel
import cloud.app.vvf.datastore.app.AppDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class NetworkStreamViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  dataFlow: MutableStateFlow<AppDataStore>) : ViewModel() {
  fun playStream(streamUrl: String) {
    //todo setup network stream and goto @PlayerFragment to play stream url
  }
}
