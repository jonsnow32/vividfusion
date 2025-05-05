package cloud.app.vvf.features.dialogs

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.subtitles.SubtitleClient
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.extension.run
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SubtitleViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  private val extensionsFlow: MutableStateFlow<List<Extension<*>>?>
) : CatchingViewModel(throwableFlow) {

  val subtitles = MutableStateFlow<List<SubtitleData>?>(null)
  val loading = MutableStateFlow(false)

  private var subtitleJob: Job? = null

  fun findSubtitle(searchItem: SearchItem) {
    viewModelScope.launch {
      subtitleJob?.cancelAndJoin()
      subtitleJob = viewModelScope.launch {
        loading.value = true
        subtitles.value = null
        try {
          val extensions = extensionsFlow.first() ?: emptyList()
          val subtitleExtensions = extensions.filter {
            it.metadata.types.contains(ExtensionType.SUBTITLE)
          }

          if (subtitleExtensions.isEmpty()) {
            return@launch
          }

          // Use supervisorScope to handle individual extension failures
          supervisorScope {
            subtitleExtensions.forEach { extension ->
              launch(Dispatchers.IO) {
                try {
                  extension.run<SubtitleClient, Boolean>(throwableFlow) {
                    loadSubtitles(
                      searchItem,
                      callback = ::onSubtitleData
                    )
                  }
                } catch (e: Exception) {
                  // Exceptions are caught by throwableFlow in run(); log if needed
                }
              }
            }
          }
        } finally {
          loading.value = !loading.value
          if (subtitles.value == null)
            subtitles.value = emptyList()
        }
      }
    }
  }

  private suspend fun onSubtitleData(subtitleData: SubtitleData) {
    withContext(Dispatchers.Main) {
      subtitles.update { current -> (current ?: emptyList()).plus(subtitleData) }
    }
  }

  override fun onCleared() {
    subtitleJob?.cancel()
    super.onCleared()
  }
}
