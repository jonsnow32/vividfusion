package cloud.app.avp.ui.detail.show

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.ui.main.FeedViewModel
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.clients.mvdatabase.ShowClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.movie.Season
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>
) : CatchingViewModel(throwableFlow) {

  var loading = MutableStateFlow(false);
  var fullMediaItem = MutableStateFlow<AVPMediaItem?>(null)
  var seasons = MutableStateFlow<List<Season>?>(null)

  fun getFullShowItem(shortShowItem : AVPMediaItem.ShowItem) {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.collect { client ->
        if (client is FeedClient) {
          loading.value = true
          val showDetail = client.getMediaDetail(shortShowItem) ?: shortShowItem
          fullMediaItem.value = showDetail
          if(client is ShowClient) {
            tryWith {
              seasons.value = client.getEpisodes((showDetail as AVPMediaItem.ShowItem).show)
            }
          }
          loading.value = false
        }
      }
    }
  }
}
