package cloud.app.vvf.ui.main

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.extension.run
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.DatabaseExtension
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class FeedViewModel(
  throwableFlow: MutableSharedFlow<Throwable>,
  open val databaseExtensionFlow: MutableStateFlow<DatabaseExtension?>,
) : CatchingViewModel(throwableFlow) {

  override fun onInitialize() {
    viewModelScope.launch {
      databaseExtensionFlow.collectLatest {
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
    loading.emit(true)
    val list = extension.run(throwableFlow) { getTabs(this) } ?: emptyList()
    loading.emit(false)
    tab = list.find { it.id == tab?.id } ?: list.firstOrNull()
    tabs.value = list
  }

  private suspend fun loadFeed(extension: Extension<*>) = extension.run(throwableFlow) {
    getFeed(this)?.collectTo(feed)
  }

  private var job: Job? = null

  fun refresh(resetTab: Boolean = false) {

    job?.cancel()
    feed.value = null
    val extension = databaseExtensionFlow.value ?: return

    job = viewModelScope.launch(Dispatchers.IO) {
      if (resetTab) loadTabs(extension)
      feed.value = null
      loadFeed(extension)
    }
  }
}

