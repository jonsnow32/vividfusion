package cloud.app.avp.ui.detail.show.season

import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.helper.addFavoritesData
import cloud.app.avp.datastore.helper.addHistoryData
import cloud.app.avp.datastore.helper.getFavoritesData
import cloud.app.avp.datastore.helper.removeFavoritesData
import cloud.app.avp.network.api.trakt.services.model.stats.Episodes
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.movie.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeasonViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>,
  val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableStateFlow(false);
  var fullMediaItem = MutableStateFlow<AVPMediaItem?>(null)
  val favoriteStatus = MutableStateFlow(false)

  fun getItemDetails(shortItem: AVPMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      viewModelScope.launch(Dispatchers.IO) {
        extensionFlow.collect { client ->
          if (client is FeedClient) {
            loading.value = true
            fullMediaItem.value = client.getMediaDetail(shortItem) ?: shortItem
            val favoriteDeferred = async { dataStore.getFavoritesData<AVPMediaItem.SeasonItem>(fullMediaItem.value?.id?.toString()) }
            favoriteStatus.value = favoriteDeferred.await()
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

  fun saveHistory(episode: AVPMediaItem.EpisodeItem) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.addHistoryData(episode)
    }
  }

}
