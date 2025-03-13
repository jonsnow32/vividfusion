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
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.extension.run
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class FeedViewModel(
  throwableFlow: MutableSharedFlow<Throwable>,
  val dataFlow: MutableStateFlow<AppDataStore>,
  val selectedExtension: MutableStateFlow<Extension<DatabaseClient>?>
) : CatchingViewModel(throwableFlow) {

  override fun onInitialize() {
    super.onInitialize()
    viewModelScope.launch{
      selectedExtension.collectLatest{
        if (it != null) refresh(it, true)
      }
    }
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
      tabs.value =
        extension.run<DatabaseClient, List<Tab>?>(throwableFlow) { getTabs(this) }
          ?: emptyList()
      tab = tabs.value.find { it.id == tab?.id } ?: tabs.value.firstOrNull()
    }
  }

  private suspend fun loadFeed(extension: Extension<*>) {
    extension.run<DatabaseClient, Unit>(throwableFlow) {
      feed.value = getFeed(this)?.cachedIn(viewModelScope)?.first()
    }
  }

  private var job: Job? = null

  fun refresh(dbExtension : Extension<DatabaseClient>?, resetTab: Boolean = false) {
    dbExtension ?: return
    job?.cancel()
    job = viewModelScope.launch(Dispatchers.IO) {
      feed.value = null
      if (resetTab) loadTabs(dbExtension)
      loadFeed(dbExtension)
    }
  }

  private suspend inline fun useLoading(block: () -> Unit) {
    loading.emit(true)
    block()
    loading.emit(false)
  }
}
