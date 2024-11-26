package cloud.app.vvf.ui.stream

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.extension.run
import cloud.app.vvf.common.clients.StreamExtension
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.SubtitleData
import cloud.app.vvf.common.models.stream.StreamData
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
          loadStream(it, mediaItem)
        }
      }
    }
  }

  fun loadStream(extension: StreamExtension, avpMediaItem: AVPMediaItem?) {
    val item = avpMediaItem ?: return
    viewModelScope.launch {
      extension.run(throwableFlow) {
        loadLinks(
          item,
          subtitleCallback = ::onSubtitleReceived,
          callback = ::onLinkReceived
        )
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
