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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlowList: MutableStateFlow<List<StreamExtension>>,
) :
  CatchingViewModel(throwableFlow) {
    
  private val _streams = MutableStateFlow<List<StreamData>>(emptyList())
  val streams: StateFlow<List<StreamData>> = _streams.asStateFlow()

  private val _subtitles = MutableStateFlow<List<SubtitleData>>(emptyList())
  val subtitles: StateFlow<List<SubtitleData>> = _subtitles.asStateFlow()

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
      client.loadLinks(item, subtitleCallback = ::onSubtitleReceived, callback = ::onLinkReceived)
    }
  }

  fun onSubtitleReceived(subtitleData: SubtitleData) {
    _subtitles.value = _subtitles.value + subtitleData
  }

  fun onLinkReceived(streamData: StreamData) {
    _streams.value = _streams.value + streamData
  }
}
