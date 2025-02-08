package cloud.app.vvf.ui.main.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.deleteHistorySearch
import cloud.app.vvf.datastore.helper.getSearchHistory
import cloud.app.vvf.datastore.helper.saveSearchHistory
import cloud.app.vvf.ui.main.FeedViewModel
import cloud.app.vvf.ui.paging.toFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  dbExtFlow: MutableStateFlow<Extension<DatabaseClient>?>,
  extListFlow: MutableStateFlow<List<Extension<*>>?>,
  val dataStore: DataStore,
) : FeedViewModel(throwableFlow, dbExtFlow, extListFlow) {

  var query: String? = ""
  override suspend fun getTabs(client: BaseClient) =
    (client as? DatabaseClient)?.searchTabs(query)

  override fun getFeed(client: BaseClient): Flow<PagingData<MediaItemsContainer>>? {
    if (query.isNullOrBlank()) {
      return null
    }
    return (client as? DatabaseClient)?.searchFeed(query, tab)?.toFlow()
  }

  val historyQuery = MutableStateFlow<List<SearchItem>>(emptyList())

  fun getHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      val list = dataStore.getSearchHistory() ?: emptyList()
      historyQuery.emit(list)
    }
  }

  fun saveHistory() {
    query?.let {
      dataStore.saveSearchHistory(SearchItem(it, true, System.currentTimeMillis()))
    }
  }

  fun deleteHistory(item: SearchItem) {
    dataStore.deleteHistorySearch(item)
  }
}
