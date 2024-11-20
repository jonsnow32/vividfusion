package cloud.app.avp.ui.main.library

import cloud.app.avp.base.CatchingViewModel
import cloud.app.common.clients.BaseClient
import cloud.app.common.models.Tab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
) : CatchingViewModel(throwableFlow) {

  init {
    initialize();
  }

  suspend fun getTabs(client: BaseClient): List<Tab> {
    return listOf(Tab("watchlist", "Watchlist"), Tab("history", "History"), Tab("downloads", "Downloads"))
  }
}

