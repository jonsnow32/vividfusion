package cloud.app.vvf.ui.stream

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.extension.run
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<List<Extension<*>>?>,
) :
  CatchingViewModel(throwableFlow) {
  private val _streams = MutableStateFlow<List<StreamData>>(emptyList())
  val streams: StateFlow<List<StreamData>> = _streams.asStateFlow()

  private val _subtitles = MutableStateFlow<List<SubtitleData>>(emptyList())
  val subtitles: StateFlow<List<SubtitleData>> = _subtitles.asStateFlow()

  var mediaItem: AVPMediaItem? = null

  var extension: MutableStateFlow<StreamClient?> = MutableStateFlow(null)
  override fun onInitialize() {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.collect { extensions ->
        loadStream(extensions, mediaItem)
      }
    }
  }

  fun loadStream(extensions: List<Extension<*>>?, avpMediaItem: AVPMediaItem?) {
    val item = avpMediaItem ?: return

    extensions?.forEach {
      viewModelScope.launch(Dispatchers.IO) {
        it.run<StreamClient, Boolean>(throwableFlow) {
          loadLinks(
            item,
            subtitleCallback = ::onSubtitleReceived,
            callback = ::onLinkReceived
          )
        }
      }
    }
  }

  fun onSubtitleReceived(subtitleData: SubtitleData) {
    _subtitles.value = _subtitles.value + subtitleData
  }

  fun onLinkReceived(streamData: StreamData) {
    _streams.value = _streams.value + streamData
  }
}
