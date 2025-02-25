package cloud.app.vvf.ui.main.home

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.Tab
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.setCurrentDBExtension
import cloud.app.vvf.ui.main.FeedViewModel
import cloud.app.vvf.ui.paging.toFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  extListFlow: MutableStateFlow<List<Extension<*>>>,
  dataStore: DataStore,
) : FeedViewModel(throwableFlow, extListFlow, dataStore) {

  init {
    initialize();
  }

  override suspend fun getTabs(client: BaseClient): List<Tab>? = (client as? DatabaseClient)?.getHomeTabs()
  override fun getFeed(client: BaseClient) = (client as? DatabaseClient)?.getHomeFeed(tab)?.toFlow()
  fun selectDbExtension(extension: Extension<*>) {
    selectedExtension.value = extension
    dataStore.setCurrentDBExtension(extension.metadata)
  }
}

