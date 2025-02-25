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
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.getCurrentDBExtension
import cloud.app.vvf.datastore.helper.setCurrentDBExtension
import cloud.app.vvf.extension.run
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

abstract class FeedViewModel(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extListFlow: MutableStateFlow<List<Extension<*>>>,
  val dataStore: DataStore
) : CatchingViewModel(throwableFlow) {

  var selectedExtension: MutableStateFlow<Extension<*>?> = MutableStateFlow(null)

  override fun onInitialize() {
    extListFlow
      .onEach { extensions ->
        if(extensions.isNotEmpty()) {
          val currentExtension = dataStore.getCurrentDBExtension()
          if(currentExtension != null) {
            val extension = extensions.find { currentExtension.className == it.id } ?: return@onEach
            selectedExtension.value = extension
            //extension?.let { dataStore.setCurrentDBExtension(it.metadata) }
            refresh(resetTab = true)
          } else {
            val extension = extensions.random()
            selectedExtension.value = extension
            //extension?.let { dataStore.setCurrentDBExtension(it.metadata) }
            refresh(resetTab = true)
          }
        }
      }
      .launchIn(viewModelScope)
    viewModelScope.launch {
      selectedExtension.collectLatest { extension ->
        Timber.i(extension?.name)
        if(extension != null)
          refresh(resetTab = true)
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
        extension.run<DatabaseClient, List<Tab>?>(throwableFlow) { getTabs(this) } ?: emptyList()
      tab = tabs.value.find { it.id == tab?.id } ?: tabs.value.firstOrNull()
    }
  }

  private suspend fun loadFeed(extension: Extension<*>) {
    extension.run<DatabaseClient, Unit>(throwableFlow) {
      feed.value = getFeed(this)?.cachedIn(viewModelScope)?.first()
    }
  }

  private var job: Job? = null

  fun refresh(resetTab: Boolean = false) {
    job?.cancel()
    job = viewModelScope.launch(Dispatchers.IO) {
      feed.value = null
      if (resetTab) loadTabs(selectedExtension.value ?: return@launch)
      loadFeed(selectedExtension.value ?: return@launch)
    }
  }
  private suspend inline fun useLoading(block: () -> Unit) {
    loading.emit(true)
    block()
    loading.emit(false)
  }
}
