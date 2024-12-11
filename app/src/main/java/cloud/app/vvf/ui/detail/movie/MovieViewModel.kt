package cloud.app.vvf.ui.detail.movie

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.addFavoritesData
import cloud.app.vvf.datastore.helper.getFavoritesData
import cloud.app.vvf.datastore.helper.removeFavoritesData
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
class MovieViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val databaseExtensionFlow: MutableStateFlow<Extension<DatabaseClient>?>,
  val streamExtensionFlow: MutableStateFlow<Extension<StreamClient>?>,
  val dataStore: DataStore,
) : CatchingViewModel(throwableFlow) {

  var loading = MutableStateFlow(false);
  var fullMediaItem = MutableStateFlow<AVPMediaItem?>(null)
  val favoriteStatus = MutableStateFlow(false)

  fun getItemDetails(shortItem: AVPMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      databaseExtensionFlow.collect { extension ->
        loading.value = true
        val showDetail = extension?.run(throwableFlow) {
          getMediaDetail(shortItem)
        } ?: shortItem
        fullMediaItem.value = showDetail
        val favoriteDeferred =
          async { dataStore.getFavoritesData<AVPMediaItem.MovieItem>(fullMediaItem.value?.id?.toString()) }
        favoriteStatus.value = favoriteDeferred.await()
        loading.value = false
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

  fun loadLink(loadLink: (List<StreamData>?) -> Unit) {
    val avpItem = fullMediaItem.value ?: return
    viewModelScope.launch(Dispatchers.IO) {
      streamExtensionFlow.collectLatest { extension ->

        if (extension == null)
          throwableFlow.emit(Throwable("No stream extension found"))

//        extension?.run(throwableFlow) { loadLink(avpItem) }?.collectLatest {
//          loadLink.invoke(it)
//        }
      }
    }
  }
}
