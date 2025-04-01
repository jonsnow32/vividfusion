package cloud.app.vvf.ui.widget.dialog.itemOption

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.datastore.app.helper.BookmarkItem
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
  val dataFlow: MutableStateFlow<AppDataStore>,
  ) : CatchingViewModel(throwableFlow) {

  var loading = MutableSharedFlow<Boolean>();
  var detailItem = MutableStateFlow<AVPMediaItem?>(null)
  val favoriteStatus = MutableStateFlow(false)
  val bookmarkStatus = MutableStateFlow<BookmarkItem?>(null)
  val knowFors = MutableStateFlow<MediaItemsContainer.Category?>(null)
  var extension: MutableStateFlow<Extension<*>?> = MutableStateFlow(null)
  fun getItemDetails(shortItem: AVPMediaItem, extensionId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.collect { extensions ->
        extension.value = extensions?.find { it.id == extensionId } ?: return@collect
        loading.emit(true)
        val showDetail = extension.value?.run<DatabaseClient, AVPMediaItem?>(throwableFlow) {
          getMediaDetail(shortItem)
        } ?: shortItem
        detailItem.value = showDetail

        val favoriteDeferred = when (shortItem) {
          is AVPMediaItem.MovieItem,
          is AVPMediaItem.ActorItem,
          is AVPMediaItem.EpisodeItem,
          is AVPMediaItem.SeasonItem,
          is AVPMediaItem.ShowItem,
          is AVPMediaItem.StreamItem -> async {
            dataFlow.value.getFavoritesData(
              detailItem.value?.id?.toString()
            )
          }
          is AVPMediaItem.LocalVideoAlbum,
          is AVPMediaItem.LocalVideoItem,
          is AVPMediaItem.PlaybackProgressItem,
          is AVPMediaItem.TrailerItem -> async {
            true
          }


        }
        favoriteStatus.value = favoriteDeferred.await()
        bookmarkStatus.value = dataFlow.value.findBookmark(shortItem)

        loading.emit(false)
      }
    }
  }

  fun toggleFavoriteStatus(statusChangedCallback: (Boolean?) -> Unit) {
    if (!favoriteStatus.value) {
      dataFlow.value.addFavoritesData(detailItem.value)
    } else {
      dataFlow.value.removeFavoritesData(detailItem.value)
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

