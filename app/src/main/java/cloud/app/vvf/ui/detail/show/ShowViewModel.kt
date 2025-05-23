package cloud.app.vvf.ui.detail.show

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.Companion.toMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.PlaybackProgress
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.extension.runClient
import cloud.app.vvf.ui.paging.toFlow
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
  val extensionFlow: MutableStateFlow<List<Extension<*>>?>,
  val updateUIFlow: MutableStateFlow<AVPMediaItem?>,
  private val dataFlow: MutableStateFlow<AppDataStore>,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableSharedFlow<Boolean>();
  var fullMediaItem = MutableStateFlow<AVPMediaItem.ShowItem?>(null)
  val watchedSeasons = MutableStateFlow<List<AVPMediaItem.SeasonItem>?>(emptyList())
  val recommendations = MutableStateFlow<PagingData<AVPMediaItem>?>(null)
  val trailers = MutableStateFlow<PagingData<AVPMediaItem>?>(null)

  val favoriteStatus = MutableStateFlow(false)
  val lastWatchedEpisode = MutableStateFlow<PlaybackProgress?>(null)


  fun getItemDetails(shortItem: AVPMediaItem, extensionId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.collect { extensions ->
        loading.emit(true)
        val showDetail = extensions?.runClient<DatabaseClient, AVPMediaItem?>(extensionId, throwableFlow) {
          getMediaDetail(shortItem)
        } ?: shortItem


        //update watched season
        watchedSeasons.value =
          (showDetail as AVPMediaItem.ShowItem).show.seasons?.map { it.toMediaItem(showDetail) }
            ?.map {
              it.watchedEpisodeNumber = dataFlow.value.getWatchedEpisodeCount(it)
              it
            }

        fullMediaItem.value = showDetail
        val favoriteDeferred =
          async { dataFlow.value.getFavoritesData(fullMediaItem.value?.id?.toString()) }
        val lastWatchedDeferred = async { dataFlow.value.getLatestPlaybackProgress(shortItem) }

        favoriteStatus.value = favoriteDeferred.await()
        lastWatchedEpisode.value = lastWatchedDeferred.await()

        loading.emit(false)

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
                      it.watchedEpisodeNumber = dataFlow.value.getWatchedEpisodeCount(it)
                      it
                    }
                lastWatchedEpisode.value = dataFlow.value.getLatestPlaybackProgress(item.seasonItem.showItem)
              }
            }
            else -> {}
          }
        }
      }
    }
  }

  fun loadRecommended(extensionId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.value?.runClient<DatabaseClient, PagedData<AVPMediaItem>?>(extensionId, throwableFlow) {
        fullMediaItem.value?.let { getRecommended(it) }
      }?.toFlow()?.collectTo(recommendations)
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

}
