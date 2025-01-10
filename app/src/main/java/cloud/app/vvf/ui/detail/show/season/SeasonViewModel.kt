package cloud.app.vvf.ui.detail.show.season

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.PlaybackProgress
import cloud.app.vvf.datastore.helper.PlaybackProgressFolder
import cloud.app.vvf.datastore.helper.addFavoritesData
import cloud.app.vvf.datastore.helper.getFavoritesData
import cloud.app.vvf.datastore.helper.removeFavoritesData
import cloud.app.vvf.datastore.helper.savePlaybackProgress
import cloud.app.vvf.extension.run
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeasonViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val databaseExtensionFlow: MutableStateFlow<Extension<DatabaseClient>?>,
  val updateUIFlow: MutableStateFlow<AVPMediaItem?>,
  val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableStateFlow(false);
  var fullMediaItem = MutableStateFlow<AVPMediaItem.SeasonItem?>(null)
  val favoriteStatus = MutableStateFlow(false)
  fun getItemDetails(shortItem: AVPMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      databaseExtensionFlow.collect { extension ->
        val detail = extension?.run(throwableFlow) {
          getMediaDetail(shortItem)
        }  ?: shortItem
        loading.value = true
        (detail as AVPMediaItem.SeasonItem).watchedEpisodeNumber =
          dataStore.getKeys("$PlaybackProgressFolder/${detail.id}").count()
        fullMediaItem.value = detail
        val favoriteDeferred =
          async { dataStore.getFavoritesData<AVPMediaItem.SeasonItem>(fullMediaItem.value?.id?.toString()) }
        favoriteStatus.value = favoriteDeferred.await()
        loading.value = false

        updateUIFlow.collectLatest { item ->
          when (item) {
            is AVPMediaItem.EpisodeItem -> {
              if (item.seasonItem.id == fullMediaItem.value?.id) {
                fullMediaItem.value = item.seasonItem
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

  fun saveHistory(episode: AVPMediaItem.EpisodeItem) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.savePlaybackProgress(PlaybackProgress(episode, 1000003, 39843984, System.currentTimeMillis()))
      updateUIFlow.emit(episode)
    }
  }
}
