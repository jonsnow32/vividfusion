package cloud.app.avp.ui.main

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.avp.base.CatchingViewModel
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.infos.FeedClient
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class FeedViewModel(
  throwableFlow: MutableSharedFlow<Throwable>,
  open val extensionFlow: MutableStateFlow<BaseExtension?>
) : CatchingViewModel(throwableFlow) {

  override fun onInitialize() {
    viewModelScope.launch {
      extensionFlow.collect { refresh(true) }
    }
  }

  abstract suspend fun getTabs(client: BaseExtension): List<Tab>?
  abstract fun getFeed(client: BaseExtension): Flow<PagingData<MediaItemsContainer>>?

  var recyclerPosition = 0

  val loading = MutableSharedFlow<Boolean>()
  val feed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
  val tabs = MutableStateFlow<List<Tab>>(emptyList())
  var tab: Tab? = null

  private suspend fun loadTabs(client: BaseExtension) {
    loading.emit(true)
    val list = tryWith { getTabs(client) } ?: emptyList()
    loading.emit(false)
    if (!list.any { it.id == tab?.id }) tab = list.firstOrNull()
    tabs.value = list
  }

  private suspend fun loadFeed(client: BaseExtension) = tryWith {  getFeed(client)?.collectTo(feed) }

  private var job: Job? = null
  fun refresh(resetTab: Boolean = false) {
    val client = extensionFlow.value ?: return
    job?.cancel()
    job = viewModelScope.launch(Dispatchers.IO) {
      if (resetTab) loadTabs(client)
      feed.value = null;
      loadFeed(client)
    }

  }


}
