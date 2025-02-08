package cloud.app.vvf.ui.main

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.Tab
import cloud.app.vvf.extension.run
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

abstract class FeedViewModel(
  throwableFlow: MutableSharedFlow<Throwable>,
  val dbExtFlow: MutableStateFlow<Extension<DatabaseClient>?>,
  val extListFlow: MutableStateFlow<List<Extension<*>>?>
) : CatchingViewModel(throwableFlow) {

  override fun onInitialize() {
    dbExtFlow
      .onEach { refresh(resetTab = true) }
      .launchIn(viewModelScope)
  }

  abstract suspend fun getTabs(client: BaseClient): List<Tab>?
  abstract fun getFeed(client: BaseClient): Flow<PagingData<MediaItemsContainer>>?

  var recyclerPosition = 0
  var recyclerOffset = 0

  val loading = MutableSharedFlow<Boolean>()
  val feed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
  val tabs = MutableStateFlow<List<Tab>>(emptyList())
  var tab: Tab? = null

  private suspend fun loadTabs(extension: Extension<*>) {
    useLoading {
      tabs.value = extension.run(throwableFlow) { getTabs(this) } ?: emptyList()
      tab = tabs.value.find { it.id == tab?.id } ?: tabs.value.firstOrNull()
    }
  }

  private suspend fun loadFeed(extension: Extension<*>) {
    extension.run(throwableFlow) {
      feed.value = getFeed(this)?.cachedIn(viewModelScope)?.first()
    }
  }

  private var job: Job? = null

  fun refresh(resetTab: Boolean = false) {
    job?.cancel()
    job = viewModelScope.launch(Dispatchers.IO) {
      if (resetTab) loadTabs(dbExtFlow.value ?: return@launch)
      loadFeed(dbExtFlow.value ?: return@launch)
    }
  }

  private suspend inline fun useLoading(block: () -> Unit) {
    loading.emit(true)
    block()
    loading.emit(false)
  }
}
