package cloud.app.vvf.ui.detail.show.season

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.PlaybackProgress
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.extension.runClient
import cloud.app.vvf.ui.detail.show.episode.EpisodeAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
  var fullSeasonItem = MutableStateFlow<AVPMediaItem.SeasonItem?>(null)
  val favoriteStatus = MutableStateFlow(false)
  val episodeData = MutableStateFlow<List<EpisodeAdapter.EpisodeData>?>(null)

  fun getItemDetails(shortItem: AVPMediaItem, extensionId: String) {
    viewModelScope.launch {
      extensionFlow.collect { extensions ->
        val detail =
          extensions.runClient<DatabaseClient, AVPMediaItem?>(extensionId, throwableFlow) {
            getMediaDetail(shortItem)
          } ?: shortItem

        loading.value = true
        (detail as AVPMediaItem.SeasonItem).watchedEpisodeNumber = dataFlow.value.getWatchedEpisodeCount(detail)

        fullSeasonItem.value = detail
        val favoriteDeferred =
          async { dataFlow.value.getFavoritesData(fullSeasonItem.value?.id?.toString()) }
        favoriteStatus.value = favoriteDeferred.await()
        loading.value = false

        updateUIFlow.collect { item ->

          when (item) {
            is AVPMediaItem.EpisodeItem -> {
              if (item.seasonItem.id == fullSeasonItem.value?.id) {
                episodeData.value = episodeData.value?.map { item ->
                  if (item.episode.seasonNumber == item.episode.seasonNumber && item.episode.episodeNumber == item.episode.episodeNumber) {
                    val playback = dataFlow.value.findPlaybackProgress(
                      AVPMediaItem.EpisodeItem(
                        item.episode,
                        fullSeasonItem.value!!
                      )
                    )
                    EpisodeAdapter.EpisodeData(
                      item.episode,
                      playback?.position ?: 0,
                      playback?.duration
                    )
                  } else item
                }
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
      dataFlow.value.addFavoritesData(fullSeasonItem.value)
    } else {
      dataFlow.value.removeFavoritesData(fullSeasonItem.value)
    }
    favoriteStatus.value = !favoriteStatus.value
    statusChangedCallback.invoke(favoriteStatus.value)
  }

  fun saveHistory(episode: AVPMediaItem.EpisodeItem) {
    viewModelScope.launch {
      dataFlow.value.updateProgress(
        PlaybackProgress(
          episode,
          19921992,
          39843984,
          System.currentTimeMillis()
        )
      )
      episode.seasonItem.watchedEpisodeNumber = dataFlow.value.getWatchedEpisodeCount(episode.seasonItem)
      updateUIFlow.emit(episode)
    }
  }


  fun loadPlayback(episodes: List<EpisodeAdapter.EpisodeData>?) {
    viewModelScope.launch {
      episodeData.value = null
      episodeData.value = episodes?.mapNotNull { data ->
        val playback = dataFlow.value.findPlaybackProgress(
          AVPMediaItem.EpisodeItem(
            data.episode,
            fullSeasonItem.value!!
          )
        )
        EpisodeAdapter.EpisodeData(data.episode, playback?.position ?: 0, playback?.duration)
      }
    }
  }
}
