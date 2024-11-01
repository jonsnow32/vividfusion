package cloud.app.avp.ui.detail

import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.helper.addFavoritesData
import cloud.app.avp.datastore.helper.getFavoritesData
import cloud.app.avp.datastore.helper.removeFavoritesData
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.clients.mvdatabase.ShowClient
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
class ItemViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>,
  val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableStateFlow(false);
  var fullMediaItem = MutableStateFlow<AVPMediaItem?>(null)
  var seasons = MutableStateFlow<List<Season>?>(null)

  //topbar
  val favoriteStatus = MutableStateFlow(false)

  fun getItemDetails(shortItem: AVPMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      viewModelScope.launch(Dispatchers.IO) {
        extensionFlow.collect { client ->
          if (client is FeedClient) {
            loading.value = true
            val showDetail = client.getMediaDetail(shortItem) ?: shortItem
            fullMediaItem.value = showDetail

            val favoriteDeferred = async { dataStore.getFavoritesData(fullMediaItem.value?.id?.toString()) }

            val seasonsDeferred = async {
              if (shortItem is AVPMediaItem.ShowItem) {
                if (client is ShowClient) {
                  tryWith {
                    client.getSeason((showDetail as AVPMediaItem.ShowItem).show)
                  }
                } else null
              } else null
            }

            favoriteStatus.value = favoriteDeferred.await()
            seasons.value = seasonsDeferred.await()
            loading.value = false
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
