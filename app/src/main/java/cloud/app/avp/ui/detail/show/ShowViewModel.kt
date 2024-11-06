package cloud.app.avp.ui.detail.show

import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.helper.addFavoritesData
import cloud.app.avp.datastore.helper.getFavoritesData
import cloud.app.avp.datastore.helper.getHistoryData
import cloud.app.avp.datastore.helper.removeFavoritesData
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.movie.Season
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>,
  val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableSharedFlow<Boolean>();
  var fullMediaItem = MutableStateFlow<AVPMediaItem.ShowItem?>(null)

  //topbar
  val favoriteStatus = MutableStateFlow(false)
  val lastWatchedEpisode = MutableStateFlow<AVPMediaItem?>(null)

  fun getItemDetails(shortItem: AVPMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      viewModelScope.launch(Dispatchers.IO) {
        extensionFlow.collect { client ->
          if (client is FeedClient) {
            loading.emit(true)
            val showDetail = client.getMediaDetail(shortItem) ?: shortItem
            fullMediaItem.value = showDetail as AVPMediaItem.ShowItem

            val favoriteDeferred = async { dataStore.getFavoritesData<AVPMediaItem.ShowItem>(fullMediaItem.value?.id?.toString()) }
            val lastWatchedDeferred = async { dataStore.getHistoryData(fullMediaItem.value?.id?.toString())}

            favoriteStatus.value = favoriteDeferred.await()
            lastWatchedEpisode.value = lastWatchedDeferred.await()

            loading.emit(false)

          }
        }
      }
    }
  }

  fun toggleFavoriteStatus(statusChangedCallback: (Boolean?) -> Unit) {
    if (!favoriteStatus.value) {
      dataStore.addFavoritesData(fullMediaItem.value)
    } else {
      dataStore.removeFavoritesData(fullMediaItem.value)
    }
    favoriteStatus.value = !favoriteStatus.value
    statusChangedCallback.invoke(favoriteStatus.value)
  }
}
