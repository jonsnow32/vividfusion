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
import cloud.app.vvf.common.models.video.VVFVideo
import cloud.app.vvf.datastore.app.AppDataStore
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
  val settingsPreference: SharedPreferences,
  val dataFlow: MutableStateFlow<AppDataStore>,
) :
  CatchingViewModel(throwableFlow) {
  private val _streams = MutableStateFlow<List<VVFVideo>>(emptyList())
  val streams: StateFlow<List<VVFVideo>> = _streams.asStateFlow()

  private val _subtitles = MutableStateFlow<List<SubtitleData>>(emptyList())
  val subtitles: StateFlow<List<SubtitleData>> = _subtitles.asStateFlow()

  var mediaItem: AVPMediaItem? = null

  var extension: MutableStateFlow<StreamClient?> = MutableStateFlow(null)


  fun loadStream(avpMediaItem: AVPMediaItem?) {
    viewModelScope.launch(Dispatchers.IO) {
      extensionFlow.collect { extensions ->
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
                showOriginTitle = avpMediaItem.generalInfo?.originalTitle ?: avpMediaItem.title ?: "",
                showIds = avpMediaItem.show.ids,
                ids = Ids()
              ), seasonItem = AVPMediaItem.SeasonItem(
                season = avpMediaItem.show.seasons?.first() ?: Season(
                  number = 1,
                  generalInfo = GeneralInfo(title = "Season 1", originalTitle = "Season 1"),
                  episodes = null,
                  showIds = avpMediaItem.show.ids,
                  showOriginTitle = avpMediaItem.generalInfo?.originalTitle ?: avpMediaItem.title,
                  releaseDateMsUTC = null
                ),
                showItem = avpMediaItem
              )
            )
          } else
            item = lastEpisode
        }

        item ?: return@collect

        extensions?.forEach {
          viewModelScope.launch(Dispatchers.IO) {
            if(it.metadata.types.contains(ExtensionType.STREAM)) {
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
      }
    }

  }

  fun onSubtitleReceived(subtitleData: SubtitleData) {
    _subtitles.value = _subtitles.value + subtitleData
  }

  fun onLinkReceived(streamData: VVFVideo) {
    _streams.value = _streams.value + streamData
  }
}
