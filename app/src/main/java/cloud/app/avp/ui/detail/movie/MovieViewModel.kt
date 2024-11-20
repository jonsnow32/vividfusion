package cloud.app.avp.ui.detail.movie

import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.helper.addFavoritesData
import cloud.app.avp.datastore.helper.getFavoritesData
import cloud.app.avp.datastore.helper.removeFavoritesData
import cloud.app.avp.extension.run
import cloud.app.common.clients.BaseClient
import cloud.app.common.clients.DatabaseExtension
import cloud.app.common.clients.mvdatabase.DatabaseClient
import cloud.app.common.models.AVPMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val databaseExtensionFlow: MutableStateFlow<DatabaseExtension?>,
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
        }  ?: shortItem
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
}
