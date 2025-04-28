package cloud.app.vvf.extension.builtIn

import android.content.Context
import cloud.app.vvf.R
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.provider.MessageFlowProvider
import cloud.app.vvf.common.clients.subtitles.SubtitleClient
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.helpers.Page
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.Companion.toMediaItemsContainer
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.common.models.extension.Message
import cloud.app.vvf.common.models.extension.Tab
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.subtitle.SubtitleOrigin
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting
import cloud.app.vvf.common.settings.SettingSlider
import cloud.app.vvf.common.settings.SettingSwitch
import cloud.app.vvf.extension.builtIn.MediaUtils.getOldestVideoYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BuiltInClient(val context: Context) : DatabaseClient, SubtitleClient,
  MessageFlowProvider {
  override suspend fun getHomeTabs(): List<Tab> {
    return listOf(
      //Tab("AllVideos", "All Videos"),
      Tab("VideoCollections", "Video Collections"),
      Tab("Playlist", "Music Playlist")
    )
  }

  val minDuration by lazy<Long> {
    (prefSettings.getInt("pref_min_media_duration")?.toLong() ?: 30L) * 1000L
  }
  private val pageSize = 4

  override fun getHomeFeed(tab: Tab?) = PagedData.Continuous<MediaItemsContainer> {
    val page = it?.toInt() ?: 1
    val items = when (tab?.id) {
      "AllVideos" -> {
        MediaUtils.checkVideoPermission(context)
        getVideoCategories(context, page)
      }

      "VideoCollections" -> {
        MediaUtils.checkVideoPermission(context)
        getVideoCollections(context, page)
      }

      "Playlist" -> {
        MediaUtils.checkAudioPermission(context)
        getMusicCollections(context, page)
      }

      else -> TODO()
    }

    val continuation = if (items.size < pageSize) null else (page + 1).toString()
    Page(items, continuation)
  }

  private suspend fun getMusicCollections(
    context: Context,
    page: Int
  ): List<MediaItemsContainer> {
    if (page != 1) return emptyList()
    val result = mutableListOf<MediaItemsContainer>()

    withContext(Dispatchers.IO) {
      val albums = MediaUtils.getMusicCollections(context, minDuration)
      albums.forEachIndexed { index, playlist ->
        val data = PagedData.Single<AVPMediaItem> {
          playlist.tracks?.mapNotNull { track ->
            AVPMediaItem.TrackItem(track)
          }?.sortedByDescending { item -> item.track.releaseDate ?: 0L }
            ?: emptyList()
        }
        result.add(MediaItemsContainer.Category(playlist.title, null, data))
      }
    }

    return result
  }

  fun getTimeRanges(context: Context): List<Pair<String, Long>> {
    val currentTime = System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
    val currentYear = calendar.get(Calendar.YEAR)
    val oldestYear = getOldestVideoYear(context) ?: currentYear

    // Adjust "Today" to start at midnight
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis

    val baseRanges = listOf(
      context.getString(R.string.time_range_today) to todayStart, // Start of today
      context.getString(R.string.time_range_yesterday) to currentTime - TimeUnit.DAYS.toMillis(1),
      context.getString(R.string.time_range_one_week_ago) to currentTime - TimeUnit.DAYS.toMillis(7),
      context.getString(R.string.time_range_one_month_ago) to currentTime - TimeUnit.DAYS.toMillis(
        30
      ),
      context.getString(R.string.time_range_one_year_ago) to currentTime - TimeUnit.DAYS.toMillis(
        365
      )
    )

    val previousYears = mutableListOf<Pair<String, Long>>()
    for (year in (currentYear - 1) downTo oldestYear) {
      calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      val yearStart = calendar.timeInMillis
      val yearLabel = context.getString(R.string.time_range_previous_year, year)
      previousYears.add(yearLabel to yearStart)
    }

    return baseRanges + previousYears
  }

  suspend fun getVideoCategories(context: Context, page: Int = 1): List<MediaItemsContainer> {


    if (page != 1) return emptyList()
    val result = mutableListOf<MediaItemsContainer>()

    val list = getTimeRanges(context)
    list.forEachIndexed { index, value ->
      val data = PagedData.Continuous<AVPMediaItem> {
        val continuation = it?.toInt() ?: 1
        val items = withContext(Dispatchers.IO) {
          MediaUtils.getVideoByRangeOfDays(
            context,
            value.second,
            if (index == 0) System.currentTimeMillis() else list[index - 1].second - 1, // Adjust end to avoid overlap
            continuation,
            20,
            minDuration
          ).map { video ->
            AVPMediaItem.VideoItem(video)
          }
        }
        Page(
          items,
          if (items.isEmpty()) null else (continuation + 1).toString()
        )
      }
      data.loadFirst().apply {
        if (isNotEmpty()) {
          result.add(
            toMediaItemsContainer(
              value.first,
              value.first,
              data
            )
          )
        }
      }
    }
    return result
  }

  suspend fun getVideoCollections(context: Context, page: Int = 1): List<MediaItemsContainer> {
    if (page != 1) return emptyList()
    val result = mutableListOf<MediaItemsContainer>()

    withContext(Dispatchers.IO) {
      val albums = MediaUtils.getVideoCollections(context, minDuration)
      albums.forEachIndexed { index, album ->
        val data = PagedData.Single<AVPMediaItem> {
          album.videos.map { video ->
            AVPMediaItem.VideoItem(video)
          }.sortedByDescending { item -> item.video.addedTime }
        }
        result.add(MediaItemsContainer.Category(album.title, null, data))
      }
    }
    return result
  }

  override suspend fun getMediaDetail(avpMediaItem: AVPMediaItem): AVPMediaItem? {
    return null
  }

  override fun getKnowFor(actor: AVPMediaItem.ActorItem): PagedData<AVPMediaItem> {
    TODO("Not yet implemented")
  }

  override fun getRecommended(avpMediaItem: AVPMediaItem): PagedData<AVPMediaItem> {
    TODO("Not yet implemented")
  }

  override suspend fun quickSearch(query: String): List<SearchItem> {
    TODO("Not yet implemented")
  }

  override suspend fun searchTabs(query: String?): List<Tab> {
    return listOf(Tab("Videos", "Videos"), Tab("Musics", "Musics"))
  }

  override fun searchFeed(
    query: String?,
    tab: Tab?
  ): PagedData<MediaItemsContainer> {
    return PagedData.Continuous<MediaItemsContainer> { continuation ->
      val page = continuation?.toInt() ?: 1
      val pageSize = 20

      val items = mutableListOf<MediaItemsContainer>()

      withContext(Dispatchers.IO) {
        when (tab?.id) {
          "Videos" -> {
            val videos = if (query.isNullOrBlank()) {
              MediaUtils.getAllVideos(context, page, pageSize, minDuration)
            } else {
              MediaUtils.searchVideos(context, query, page, pageSize)
            }
            val data = PagedData.Single<AVPMediaItem> {
              videos.map { video -> AVPMediaItem.VideoItem(video) }
            }
            if (videos.isNotEmpty()) {
              items.add(
                MediaItemsContainer.Category(
                  title = "Videos",
                  subtitle = null,
                  more = data
                )
              )
            }
          }

          "Musics" -> {
            val tracks = if (query.isNullOrBlank()) {
              MediaUtils.getAllTracks(context, page, pageSize, minDuration)
            } else {
              MediaUtils.searchTracks(context, query, page, pageSize)
            }
            val data = PagedData.Single<AVPMediaItem> {
              tracks.map { track -> AVPMediaItem.TrackItem(track) }
            }
            if (tracks.isNotEmpty()) {
              items.add(
                MediaItemsContainer.Category(
                  title = "Songs",
                  subtitle = null,
                  more = data
                )
              )
            }
          }

          else -> {
            // Trả về rỗng nếu không có tab hợp lệ
          }
        }
      }

      val newContinuation = if (items.isEmpty()) null else (page + 1).toString()
      Page(items, newContinuation)
    }
  }

  override suspend fun loadSubtitles(
    mediaItem: AVPMediaItem,
    callback: (SubtitleData) -> Unit
  ): Boolean {

    val subtitles = listOf(
      SubtitleData(
        name = "TTML positioning",
        mimeType = "application/ttml+xml",
        languageCode = "en",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ttml/netflix_ttml_sample.xml",
        headers = mapOf()
      ),
      SubtitleData(
        name = "TTML Japanese features",
        mimeType = "application/ttml+xml",
        languageCode = "ja",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ttml/japanese-ttml.xml",
        headers = mapOf()
      ),
      SubtitleData(
        name = "TTML Netflix Japanese examples (IMSC1.1)",
        mimeType = "application/ttml+xml",
        languageCode = "ja",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ttml/netflix_japanese_ttml.xml",
        headers = mapOf()
      ),
      SubtitleData(
        name = "WebVTT positioning",
        mimeType = "text/vtt",
        languageCode = "en",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/numeric-lines.vtt",
        headers = mapOf()
      ),
      SubtitleData(
        name = "WebVTT Japanese features",
        mimeType = "text/vtt",
        languageCode = "ja",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/japanese.vtt",
        headers = mapOf()
      ),
      SubtitleData(
        name = "SubStation Alpha positioning",
        mimeType = "text/x-ssa",
        languageCode = "en",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ssa/test-subs-position.ass",
        headers = mapOf()
      ),
      SubtitleData(
        name = "SubStation Alpha styling",
        mimeType = "text/x-ssa",
        languageCode = "en",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ssa/test-subs-styling.ass",
        headers = mapOf()
      )
    )


    for (i in 1..subtitles.size) {
      delay(10)
      callback.invoke(subtitles.get(i - 1))
    }
    return true
  }


  override val defaultSettings: List<Setting>
    get() = listOf(
      SettingSwitch(
        context.getString(R.string.refresh_library_on_reload),
        "refresh_library",
        context.getString(R.string.refresh_library_on_reload_summary),
        false
      ),
      SettingSlider(
        title = context.getString(R.string.min_media_duration),
        key = "pref_min_media_duration",
        summary = context.getString(R.string.pref_min_media_duration_summary),
        defaultValue = 30,
        from = 0,
        to = 3600,
      )
    )

  private lateinit var prefSettings: PrefSettings
  private lateinit var messageFlow: MutableSharedFlow<Message>

  override fun init(prefSettings: PrefSettings) {
    this.prefSettings = prefSettings
  }

  override fun onSettingsChanged(key: String, value: Any) {
    TODO("Not yet implemented")
  }

  override suspend fun onExtensionSelected() {
    TODO("Not yet implemented")
  }


  override fun setMessageFlow(messageFlow: MutableSharedFlow<Message>) {
    this.messageFlow = messageFlow
  }

  companion object {

    val metadata = ExtensionMetadata(
      className = BuiltInClient.javaClass.toString(),
      path = "",
      importType = ImportType.BuiltIn,
      name = "The MediaStore Wrapper",
      description = "",
      version = "1.0.0",
      author = "Avp",
      iconUrl = "https://www.themoviedb.org/assets/2/v4/marketing/logos/infuse_600-a28d709ee5137f75b31c4184643a22fe83ee8f64d3317509c33090922b66dbb6.png",
      types = listOf(ExtensionType.DATABASE, ExtensionType.SUBTITLE)
    )
  }
}
