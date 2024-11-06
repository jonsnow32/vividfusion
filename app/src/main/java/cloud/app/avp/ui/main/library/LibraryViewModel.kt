package cloud.app.avp.ui.main.library

import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.ui.main.FeedViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.models.Tab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>,
) : CatchingViewModel(throwableFlow) {

  init {
    initialize();
  }

  suspend fun getTabs(client: BaseExtension): List<Tab> {
    return listOf(Tab("watchlist", "Watchlist"), Tab("history", "History"), Tab("downloads", "Downloads"))
  }
}

