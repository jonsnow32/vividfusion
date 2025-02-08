package cloud.app.vvf.ui.main.home

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.Tab
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.ui.main.FeedViewModel
import cloud.app.vvf.ui.paging.toFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  dbExtFlow: MutableStateFlow<Extension<DatabaseClient>?>,
  extListFlow: MutableStateFlow<List<Extension<*>>?>,
  val dataStore: DataStore,
) : FeedViewModel(throwableFlow, dbExtFlow, extListFlow) {

  init {
    initialize();
  }

  override suspend fun getTabs(client: BaseClient): List<Tab>? = (client as? DatabaseClient)?.getHomeTabs()
  override fun getFeed(client: BaseClient) = (client as? DatabaseClient)?.getHomeFeed(tab)?.toFlow()
}

