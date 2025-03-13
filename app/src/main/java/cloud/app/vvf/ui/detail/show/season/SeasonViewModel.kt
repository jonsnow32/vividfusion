package cloud.app.vvf.ui.detail.show.season

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.PlaybackProgressItem
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.datastore.app.PlaybackProgressFolder
import cloud.app.vvf.extension.runClient
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
  val extensionFlow: MutableStateFlow<List<Extension<*>>>,
  val updateUIFlow: MutableStateFlow<AVPMediaItem?>,
  val dataFlow: MutableStateFlow<AppDataStore>,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableStateFlow(false);
  var fullMediaItem = MutableStateFlow<AVPMediaItem.SeasonItem?>(null)
  val favoriteStatus = MutableStateFlow(false)
  fun getItemDetails(shortItem: AVPMediaItem, extensionId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.collect { extensions ->
        val detail = extensions.runClient<DatabaseClient,AVPMediaItem?>(extensionId, throwableFlow) {
          getMediaDetail(shortItem)
        }  ?: shortItem

        loading.value = true
        (detail as AVPMediaItem.SeasonItem).watchedEpisodeNumber =
          dataFlow.value.getKeys("$PlaybackProgressFolder/${detail.id}").count()
        fullMediaItem.value = detail

        val favoriteDeferred =
          async { dataFlow.value.getFavoritesData(fullMediaItem.value?.id?.toString()) }
        favoriteStatus.value = favoriteDeferred.await()
        loading.value = false

        updateUIFlow.collectLatest { item ->
          when (item) {
            is AVPMediaItem.EpisodeItem -> {
              if (item.seasonItem.id == fullMediaItem.value?.id) {
                fullMediaItem.value = item.seasonItem.copy()
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
      dataFlow.value.addFavoritesData(fullMediaItem.value)
    } else {
      dataFlow.value.removeFavoritesData(fullMediaItem.value)
    }
    favoriteStatus.value = !favoriteStatus.value
    statusChangedCallback.invoke(favoriteStatus.value)
  }

  fun saveHistory(episode: AVPMediaItem.EpisodeItem) {
    viewModelScope.launch(Dispatchers.IO) {
      dataFlow.value.savePlaybackProgress(PlaybackProgressItem(episode, 19921992, 39843984, System.currentTimeMillis()))
      episode.seasonItem.watchedEpisodeNumber =
        dataFlow.value.getKeys("$PlaybackProgressFolder/${episode.seasonItem.id}").count()
      updateUIFlow.emit(episode)
    }
  }
}
