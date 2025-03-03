package cloud.app.vvf.ui.widget.dialog

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.BookmarkItem
import cloud.app.vvf.datastore.helper.addFavoritesData
import cloud.app.vvf.datastore.helper.findBookmark
import cloud.app.vvf.datastore.helper.getFavoritesData
import cloud.app.vvf.datastore.helper.removeFavoritesData
import cloud.app.vvf.extension.run
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemOptionViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<List<Extension<*>>?>,
  val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableSharedFlow<Boolean>();
  var detailItem = MutableStateFlow<AVPMediaItem?>(null)
  val favoriteStatus = MutableStateFlow(false)
  val bookmarkStatus = MutableStateFlow<BookmarkItem?>(null)
  val knowFors = MutableStateFlow<MediaItemsContainer.Category?>(null)
  var extension: MutableStateFlow<Extension<*>?> = MutableStateFlow(null)
  fun getItemDetails(shortItem: AVPMediaItem, extensionId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      viewModelScope.launch(Dispatchers.IO) {
        extensionFlow.collect { extensions ->
          extension.value = extensions?.find { it.id == extensionId } ?: return@collect
          loading.emit(true)
          val showDetail = extension.value?.run<DatabaseClient, AVPMediaItem?>(throwableFlow) {
            getMediaDetail(shortItem)
          } ?: shortItem
          detailItem.value = showDetail
          val favoriteDeferred = when (shortItem) {
            is AVPMediaItem.MovieItem -> async {
              dataStore.getFavoritesData<AVPMediaItem.MovieItem>(
                detailItem.value?.id?.toString()
              )
            }

            is AVPMediaItem.ActorItem -> async {
              dataStore.getFavoritesData<AVPMediaItem.ActorItem>(
                detailItem.value?.id?.toString()
              )
            }

            is AVPMediaItem.EpisodeItem -> async {
              dataStore.getFavoritesData<AVPMediaItem.EpisodeItem>(
                detailItem.value?.id?.toString()
              )
            }

            is AVPMediaItem.SeasonItem -> async {
              dataStore.getFavoritesData<AVPMediaItem.SeasonItem>(
                detailItem.value?.id?.toString()
              )
            }

            is AVPMediaItem.ShowItem -> async {
              dataStore.getFavoritesData<AVPMediaItem.ShowItem>(
                detailItem.value?.id?.toString()
              )
            }

            is AVPMediaItem.StreamItem -> async {
              dataStore.getFavoritesData<AVPMediaItem.StreamItem>(
                detailItem.value?.id?.toString()
              )
            }

            is AVPMediaItem.TrailerItem -> async {
              true
            }
          }
          favoriteStatus.value = favoriteDeferred.await()
          bookmarkStatus.value = dataStore.findBookmark(shortItem)

          loading.emit(false)
        }
      }
    }
  }

  fun toggleFavoriteStatus(statusChangedCallback: (Boolean?) -> Unit) {
    if (!favoriteStatus.value) {
      dataStore.addFavoritesData(detailItem.value)
    } else {
      dataStore.removeFavoritesData(detailItem.value)
    }
    favoriteStatus.value = !favoriteStatus.value
    statusChangedCallback.invoke(favoriteStatus.value)
  }

  fun getKnowFor(extensionId: String, item: AVPMediaItem.ActorItem) {
    viewModelScope.launch {
      knowFors.value =
        MediaItemsContainer.Category(
          title = item.title,
          more = extension.value?.run<DatabaseClient, PagedData<AVPMediaItem>>(throwableFlow) { getKnowFor(item) })
    }
  }


}

