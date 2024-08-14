package cloud.app.avp.ui.stream

import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.utils.catchWith
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.stream.StreamData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension?>,
  val extensionFlowList: MutableStateFlow<List<BaseExtension>>,
) :
  CatchingViewModel(throwableFlow) {
  val streams = MutableStateFlow<List<StreamData>>(emptyList())

  var mediaItem: AVPMediaItem? = null

  override fun onInitialize() {
    viewModelScope.launch {
      extensionFlowList.collect { extensions ->
        extensions.forEach {
          if (it is StreamClient)
            loadStream(it, mediaItem)
        }
      }
    }
  }

  fun loadStream(client: StreamClient, avpMediaItem: AVPMediaItem?) {
    val item = avpMediaItem ?: return
    viewModelScope.launch {
      client.searchStreams(item).flowOn(Dispatchers.IO).catchWith(throwableFlow).collectLatest {
        streams.value = it
      }
    }
  }
}
