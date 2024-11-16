package cloud.app.avp.ui.detail.show

import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.helper.WatchedFolder
import cloud.app.avp.datastore.helper.WatchedItem
import cloud.app.avp.datastore.helper.addFavoritesData
import cloud.app.avp.datastore.helper.getFavoritesData
import cloud.app.avp.datastore.helper.getLastWatched
import cloud.app.avp.datastore.helper.removeFavoritesData
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.AVPMediaItem.Companion.toMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>,
  val updateUIFlow: MutableStateFlow<AVPMediaItem?>,
  private val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableSharedFlow<Boolean>();
  var fullMediaItem = MutableStateFlow<AVPMediaItem.ShowItem?>(null)
  val watchedSeasons = MutableStateFlow<List<AVPMediaItem.SeasonItem>?>(emptyList())

  val favoriteStatus = MutableStateFlow(false)
  val lastWatchedEpisode = MutableStateFlow<WatchedItem?>(null)

  fun getItemDetails(shortItem: AVPMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.collect { client ->

        if (client is FeedClient) {
          loading.emit(true)
          val showDetail = client.getMediaDetail(shortItem) ?: shortItem

          //update watched season
          watchedSeasons.value =
            (showDetail as AVPMediaItem.ShowItem).show.seasons?.map { it.toMediaItem(showDetail) }
              ?.map {
                it.watchedEpisodeNumber = dataStore.getKeys("$WatchedFolder/${it.id}").count()
                it
              }

          fullMediaItem.value = showDetail
          val favoriteDeferred =
            async { dataStore.getFavoritesData<AVPMediaItem.ShowItem>(fullMediaItem.value?.id?.toString()) }
          val lastWatchedDeferred = async { dataStore.getLastWatched(shortItem) }

          favoriteStatus.value = favoriteDeferred.await()
          lastWatchedEpisode.value = lastWatchedDeferred.await()

          loading.emit(false)
        }

        updateUIFlow.collectLatest { item ->
          when (item) {
            is AVPMediaItem.SeasonItem -> {

            }

            is AVPMediaItem.ActorItem -> TODO()
            is AVPMediaItem.EpisodeItem -> {
              if (item.seasonItem.showItem.id == fullMediaItem.value?.id) {
                watchedSeasons.value =
                  item.seasonItem.showItem.show.seasons?.map { it.toMediaItem(item.seasonItem.showItem) }
                    ?.map {
                      it.watchedEpisodeNumber = dataStore.getKeys("$WatchedFolder/${it.id}").count()
                      it
                    }
                lastWatchedEpisode.value = dataStore.getLastWatched(item.seasonItem.showItem)
              }
            }

            else -> {}
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
