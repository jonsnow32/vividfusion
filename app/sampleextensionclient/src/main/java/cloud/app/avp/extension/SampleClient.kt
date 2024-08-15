package cloud.app.avp.extension

import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.stream.StreamData
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient

class SampleClient : BaseExtension, StreamClient {
  private lateinit var okhttpClient: OkHttpClient

  override val metadata: ExtensionMetadata
    get() = ExtensionMetadata(
      name = "SampleClient",
      ExtensionType.STREAM,
      description = "A sample extension that does nothing",
      author = "avp",
      version = "v001",
      icon = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png",
      loginType = LoginType.NONE
    )

  override fun init(settings: Settings, okhttpClient: OkHttpClient) {
    this.okhttpClient = okhttpClient;
  }

  override val defaultSettings: List<Setting> = listOf(
    SettingSwitch(
      "High Thumbnail Quality",
      "high_quality",
      "Use high quality thumbnails, will cause more data usage.",
      false
    ), SettingSwitch(
      "Use MP4 Format",
      "use_mp4_format",
      "Use MP4 formats for audio streams, turning it on may cause source errors.",
      false
    )
  )


  override suspend fun onExtensionSelected() {

  }

  override suspend fun searchStreams(mediaItem: AVPMediaItem): Flow<List<StreamData>> {
    return when (mediaItem) {
      is AVPMediaItem.MovieItem -> getMovieStream(mediaItem.movie)
      is AVPMediaItem.EpisodeItem -> getEpisodeStream(mediaItem.episode)
      is AVPMediaItem.ActorItem,
      is AVPMediaItem.ShowItem -> TODO()

      is AVPMediaItem.StreamItem -> TODO()
    }.flowOn(Dispatchers.IO)
  }

  private suspend fun getMovieStream(movie: Movie) = flow<List<StreamData>> {
    var i = 0;
    while (i < 100) {
      delay(1000)
      emit(fakeList(i))
      i++;
    }
  }

  private suspend fun getEpisodeStream(episode: Episode) = flow<List<StreamData>> {
    var i = 0;
    while (i < 100) {
      delay(1000)
      emit(fakeList(i))
      i++;
    }
  }

  private fun fakeList(i: Int): List<StreamData> {
    return listOf(
      StreamData(
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample${i}/BigBuckBunny.mp4",
        fileName = "BigBuckBunny.mp4"
      )
    )
  }
}
