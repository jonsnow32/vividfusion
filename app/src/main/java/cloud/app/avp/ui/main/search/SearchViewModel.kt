package cloud.app.avp.ui.main.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.avp.ui.main.FeedViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.SearchClient
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
  override val extensionFlow: MutableStateFlow<BaseExtension?>,
) : FeedViewModel(throwableFlow, extensionFlow){

  var query: String? = "thor"
  override suspend fun getTabs(client: BaseExtension) =
    (client as? SearchClient)?.searchTabs(query)

  override fun getFeed(client: BaseExtension): Flow<PagingData<MediaItemsContainer>>? =
    (client as? SearchClient)?.searchFeed(query, tab)?.toFlow()

  val quickFeed = MutableStateFlow<List<QuickSearchItem>>(emptyList())

  fun quickSearch(query: String) {
    val client = extensionFlow.value
    if (client !is SearchClient) return
    viewModelScope.launch(Dispatchers.IO) {
      val list = tryWith { client.quickSearch(query) } ?: emptyList()
      quickFeed.value = list
    }
  }


}
