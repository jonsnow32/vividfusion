package cloud.app.avp.ui.stream

import androidx.lifecycle.viewModelScope
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.extension.run
import cloud.app.common.clients.StreamExtension
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.SubtitleData
import cloud.app.common.models.stream.StreamData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionFlowList: MutableStateFlow<List<StreamExtension>>,
) :
  CatchingViewModel(throwableFlow) {
  val streams = MutableStateFlow<StreamData?>(null)
  var mediaItem: AVPMediaItem? = null

  override fun onInitialize() {
    viewModelScope.launch {
      extensionFlowList.collect { extensions ->
        extensions.forEach {
         it.run(throwableFlow) {
           loadStream(this, mediaItem)
         }
        }
      }
    }
  }

  fun loadStream(client: StreamClient, avpMediaItem: AVPMediaItem?) {
    val item = avpMediaItem ?: return
    viewModelScope.launch {
      client.loadLinks(item, subtitleCallback = ::onSubtitleReceived, callback =  ::onLinkReceived)
    }

  }
  fun onSubtitleReceived(subtitleData: SubtitleData) {

  }
  fun onLinkReceived(streamData: StreamData) {
    streams.value = streamData
  }
}
