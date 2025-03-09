package cloud.app.vvf.ui.main.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.datastore.app.helper.deleteHistorySearch
import cloud.app.vvf.datastore.app.helper.getSearchHistory
import cloud.app.vvf.datastore.app.helper.saveSearchHistory
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
  extListFlow: MutableStateFlow<List<Extension<*>>>,
  dataFlow: MutableStateFlow<AppDataStore>,
) : FeedViewModel(throwableFlow,  extListFlow, dataFlow) {

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
      val list = dataFlow.value.getSearchHistory() ?: emptyList()
      historyQuery.emit(list)
    }
  }

  fun saveHistory() {
    query?.takeIf { it.isNotBlank() }?.let {
      dataFlow.value.saveSearchHistory(SearchItem(it, true, System.currentTimeMillis()))
    }
  }

  fun deleteHistory(item: SearchItem) {
    dataFlow.value.deleteHistorySearch(item)
  }
}
