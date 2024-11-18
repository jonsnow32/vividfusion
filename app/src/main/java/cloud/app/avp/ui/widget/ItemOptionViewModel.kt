package cloud.app.avp.ui.widget

import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.helper.addFavoritesData
import cloud.app.avp.datastore.helper.getFavoritesData
import cloud.app.avp.datastore.helper.removeFavoritesData
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ItemOptionViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>,
  val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableSharedFlow<Boolean>();
  var detailItem = MutableStateFlow<AVPMediaItem?>(null)
  val favoriteStatus = MutableStateFlow(false)
  val knowFors = MutableStateFlow<MediaItemsContainer.Category?>(null)

  fun getItemDetails(shortItem: AVPMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      viewModelScope.launch(Dispatchers.IO) {
        extensionFlow.collect { client ->
          if (client is FeedClient) {
            loading.emit(true)
            val showDetail = client.getMediaDetail(shortItem) ?: shortItem
            detailItem.value = showDetail
            val favoriteDeferred = when(shortItem) {
              is AVPMediaItem.MovieItem -> async { dataStore.getFavoritesData<AVPMediaItem.MovieItem>(detailItem.value?.id?.toString()) }
              is AVPMediaItem.ActorItem -> async { dataStore.getFavoritesData<AVPMediaItem.ActorItem>(detailItem.value?.id?.toString()) }
              is AVPMediaItem.EpisodeItem -> async { dataStore.getFavoritesData<AVPMediaItem.EpisodeItem>(detailItem.value?.id?.toString()) }
              is AVPMediaItem.SeasonItem -> async { dataStore.getFavoritesData<AVPMediaItem.SeasonItem>(detailItem.value?.id?.toString()) }
              is AVPMediaItem.ShowItem -> async { dataStore.getFavoritesData<AVPMediaItem.ShowItem>(detailItem.value?.id?.toString()) }
              is AVPMediaItem.StreamItem -> async { dataStore.getFavoritesData<AVPMediaItem.StreamItem>(detailItem.value?.id?.toString()) }
            }
            favoriteStatus.value = favoriteDeferred.await()

            loading.emit(false)
          }
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

  fun getKnowFor(clientId: String, item: AVPMediaItem.ActorItem) {
    viewModelScope.launch {
      extensionFlow.collect{
        if (it is FeedClient) {
          knowFors.value = MediaItemsContainer.Category(title = item.title, more = it.getKnowFor(item))
        }
      }
    }
  }

}

