package cloud.app.vvf.ui.main.library

import androidx.paging.PagingData
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.helpers.Page
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.MediaItemsContainer.Companion.toPaged
import cloud.app.vvf.common.models.Tab
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.datastore.app.helper.getAllBookmarks
import cloud.app.vvf.datastore.app.helper.getFavorites
import cloud.app.vvf.ui.main.FeedViewModel
import cloud.app.vvf.ui.paging.toFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  extListFlow: MutableStateFlow<List<Extension<*>>>,
  dataFlow: MutableStateFlow<AppDataStore>,
) : FeedViewModel(throwableFlow,  extListFlow, dataFlow) {

//  override fun onInitialize() {
//    viewModelScope.launch {
//      refresh(resetTab = true)
//    }
//  }

  override suspend fun getTabs(client: BaseClient): List<Tab> {
    return withContext(Dispatchers.IO) {
      listOf(
        Tab("Favorites", "Favorites"),
        Tab("Bookmarks", "Bookmarks"),
      )
    }
  }

  private fun List<AVPMediaItem>.toPaged() = PagedData.Single { this }

  override fun getFeed(client: BaseClient): Flow<PagingData<MediaItemsContainer>>? {
    return when (tab?.id) {
      "Favorites" -> {
        val data = dataFlow.value.getFavorites()
        data?.map { MediaItemsContainer.Item(it) }?.toPaged()?.toFlow()
      }

      "Bookmarks" ->
        PagedData.Continuous<MediaItemsContainer> { it ->
          val items = mutableListOf<MediaItemsContainer.Category>();
          dataFlow.value.getAllBookmarks()?.groupBy { it::class.java }?.map {
            val category = MediaItemsContainer.Category(title = it.key.simpleName ?: "Unknown",
              more = it.value.map { it.item }.toPaged()
            )
            items.add(category)
          }
          Page(items, null)
        }.toFlow()

      else -> null
    }
  }

  fun refreshDataStore() {
    refresh(false)
  }
}

