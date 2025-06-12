package cloud.app.vvf.ui.stream

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.EpisodeItem
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.common.models.movie.Episode
import cloud.app.vvf.common.models.movie.GeneralInfo
import cloud.app.vvf.common.models.movie.Ids
import cloud.app.vvf.common.models.movie.Season
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.extension.run
import cloud.app.vvf.extension.tmdb.services.tmdb.popularCountriesIsoToEnglishName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<List<Extension<*>>?>,
  val settingsPreference: SharedPreferences,
  val dataFlow: MutableStateFlow<AppDataStore>,
) : CatchingViewModel(throwableFlow) {
  private val _streams = MutableStateFlow<List<Video>?>(null)
  val streams: StateFlow<List<Video>?> = _streams.asStateFlow()

  private val _subtitles = MutableStateFlow<List<SubtitleData>>(emptyList())
  val subtitles: StateFlow<List<SubtitleData>> = _subtitles.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  var mediaItem: AVPMediaItem? = null

  var extension: MutableStateFlow<StreamClient?> = MutableStateFlow(null)

  var region: MutableStateFlow<String?> = MutableStateFlow(null)

  private var loadStreamJob: Job? = null

  init {
    region.value = settingsPreference.getString("region", "us")
  }

  fun loadStream(avpMediaItem: AVPMediaItem?) {
    // Cancel any existing loading job
    loadStreamJob?.cancel()

    // Start new loading job
    loadStreamJob = viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      try {
        var item = avpMediaItem
        if (avpMediaItem is AVPMediaItem.ShowItem) {
          val lastEpisode = dataFlow.value.getLatestPlaybackProgress(avpMediaItem)
          if (lastEpisode == null) {
            item = EpisodeItem(
              episode = Episode(
                episodeNumber = 1,
                seasonNumber = 1,
                generalInfo = GeneralInfo(
                  title = "S01E01",
                  originalTitle = "S01E01"
                ),
                showOriginTitle = avpMediaItem.generalInfo?.originalTitle
                  ?: avpMediaItem.title ?: "",
                showIds = avpMediaItem.show.ids,
                ids = Ids()
              ),
              seasonItem = AVPMediaItem.SeasonItem(
                season = avpMediaItem.show.seasons?.first() ?: Season(
                  number = 1,
                  generalInfo = GeneralInfo(
                    title = "Season 1",
                    originalTitle = "Season 1"
                  ),
                  episodes = null,
                  showIds = avpMediaItem.show.ids,
                  showOriginTitle = avpMediaItem.generalInfo?.originalTitle
                    ?: avpMediaItem.title,
                  releaseDateMsUTC = null
                ),
                showItem = avpMediaItem
              )
            )
          } else {
            item = lastEpisode
          }
        }

        if (item == null) return@launch

        val extensions = extensionFlow.first() ?: emptyList()
        val subtitleExtensions = extensions.filter {
          it.metadata.types.contains(ExtensionType.STREAM)
        }

        if (subtitleExtensions.isEmpty()) {
          return@launch
        }

        supervisorScope {
          subtitleExtensions.forEach { ext ->
            launch(Dispatchers.IO) {
              ext.run<StreamClient, Boolean>(throwableFlow) {
                loadLinks(
                  item,
                  subtitleCallback = ::onSubtitleReceived,
                  callback = ::onLinkReceived
                )
              }
            }
          }
        }
      } finally {
        _isLoading.value = false
        if(_streams.value == null)
          _streams.value = emptyList()
      }
    }
  }

  fun onSubtitleReceived(subtitleData: SubtitleData) {
    _subtitles.value += subtitleData
  }

  fun onLinkReceived(streamData: Video) {
    _streams.value = (_streams.value ?: emptyList()) + streamData
  }

  fun getSupportRegion(): Map<String, String> {
    return popularCountriesIsoToEnglishName
  }
}
