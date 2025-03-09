package cloud.app.vvf.ui.widget.dialog

import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.datastore.app.AppDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class SelectionViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<List<Extension<*>>?>,
) : CatchingViewModel(throwableFlow) {
}
