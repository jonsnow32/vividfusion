package cloud.app.avp.ui.detail.show

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.helper.addFavoritesData
import cloud.app.avp.datastore.helper.getFavoritesData
import cloud.app.avp.datastore.helper.removeFavoritesData
import cloud.app.avp.ui.main.FeedViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.clients.mvdatabase.ShowClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Season
import cloud.app.common.models.movie.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShowViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>,
  val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableStateFlow(false);
  var fullMediaItem = MutableStateFlow<AVPMediaItem?>(null)
  var seasons = MutableStateFlow<List<Season>?>(null)
  private val _subscribeStatus: MutableLiveData<Boolean?> = MutableLiveData(null)
  val subscribeStatus: LiveData<Boolean?> = _subscribeStatus

  val favoriteStatus = MutableStateFlow(false)

  fun getFullShowItem(shortShowItem: AVPMediaItem.ShowItem) {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.collect { client ->
        if (client is FeedClient) {
          loading.value = true
          val showDetail = client.getMediaDetail(shortShowItem) ?: shortShowItem
          fullMediaItem.value = showDetail

          val favoriteDeferred = async { dataStore.getFavoritesData(fullMediaItem.value?.id.toString()) }
          val seasonsDeferred = async {
            if (client is ShowClient) {
              tryWith {
                client.getSeason((showDetail as AVPMediaItem.ShowItem).show)
              }
            } else null
          }

          favoriteStatus.value = favoriteDeferred.await()
          seasons.value = seasonsDeferred.await()
          loading.value = false
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
