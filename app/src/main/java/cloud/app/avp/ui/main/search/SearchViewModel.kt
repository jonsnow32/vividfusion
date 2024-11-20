package cloud.app.avp.ui.main.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.avp.ui.main.FeedViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.common.clients.BaseClient
import cloud.app.common.clients.DatabaseExtension
import cloud.app.common.clients.mvdatabase.DatabaseClient
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.QuickSearchItem
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
  override val databaseExtensionFlow: MutableStateFlow<DatabaseExtension?>,
) : FeedViewModel(throwableFlow, databaseExtensionFlow) {

  var query: String? = ""
  override suspend fun getTabs(client: BaseClient) =
    (client as? DatabaseClient)?.searchTabs(query)

  override fun getFeed(client: BaseClient): Flow<PagingData<MediaItemsContainer>>? {
    if (query.isNullOrBlank()) return null
    return (client as? DatabaseClient)?.searchFeed(query, tab)?.toFlow()
  }

  val quickFeed = MutableStateFlow<List<QuickSearchItem>>(emptyList())

  fun quickSearch(query: String) {
    val client = databaseExtensionFlow.value?.instance
    if (client !is DatabaseClient) return
    viewModelScope.launch(Dispatchers.IO) {
      val list = tryWith { client.quickSearch(query) } ?: emptyList()
      quickFeed.value = list
    }
  }

  fun clearSearch() {
    quickFeed.value = emptyList()
  }

  fun updateQuickFeed() {
    quickFeed.value = listOf(QuickSearchItem.SearchQueryItem("Thor", false))
  }


  override fun onCleared() {
    super.onCleared()
  }
}
