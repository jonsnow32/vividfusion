package cloud.app.vvf.features.dialogs

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.subtitles.SubtitleClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.extension.run
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubtitleViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  private val extensionsFlow: MutableStateFlow<List<Extension<*>>?>
) : CatchingViewModel(throwableFlow) {
  val subtitles = MutableStateFlow<List<SubtitleData>>(emptyList())

  fun findSubtitle(avpMediaItem: AVPMediaItem) {
    viewModelScope.launch {
      extensionsFlow.collect { extensions ->
        extensions?.forEach {
          viewModelScope.launch(Dispatchers.IO) {
            if (it.metadata.types.contains(ExtensionType.SUBTITLE)) {
              it.run<SubtitleClient, Boolean>(throwableFlow) {
                loadSubtitles(
                  avpMediaItem,
                  callback = ::onSubtitleData
                )
              }
            }
          }
        }
      }
    }
  }
  private fun onSubtitleData(subtitleData: SubtitleData) {
    subtitles.value += subtitleData
  }
}
