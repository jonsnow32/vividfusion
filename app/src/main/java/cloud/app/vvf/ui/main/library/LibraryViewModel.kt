package cloud.app.vvf.ui.main.library

import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.models.Tab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
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

