package cloud.app.vvf.extension.builtIn

import android.content.Context
import cloud.app.vvf.R
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.provider.MessageFlowProvider
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.helpers.Page
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.Companion.toMediaItemsContainer
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.Message
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.Tab
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting
import cloud.app.vvf.common.settings.SettingSwitch
import cloud.app.vvf.extension.builtIn.MediaUtils.getOldestVideoYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BuiltInClient(val context: Context) : DatabaseClient,
  MessageFlowProvider {
  override suspend fun getHomeTabs(): List<Tab> {
    return listOf(Tab("All", "All"), Tab("Albums", "Albums"))
  }

  private val pageSize = 4
  override fun getHomeFeed(tab: Tab?) = PagedData.Continuous<MediaItemsContainer> {
    val page = it?.toInt() ?: 1

    val items = when (tab?.id) {
      "All" -> getVideoCategories(context, page)
      "Albums" -> getAlbumCategories(context, page)
      else -> TODO()
    }

    val continuation = if (items.size < pageSize) null else (page + 1).toString()
    Page(items, continuation)
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
            20
          ).map { video ->
            AVPMediaItem.LocalVideoItem(video)
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

  suspend fun getAlbumCategories(context: Context, page: Int = 1): List<MediaItemsContainer> {
    if (page != 1) return emptyList()
    val result = mutableListOf<MediaItemsContainer>()

    withContext(Dispatchers.IO) {
      val albums = MediaUtils.getAllAlbums(context)
      albums.forEachIndexed { index, album ->
        val data = PagedData.Single<AVPMediaItem> {
          album.videos.map { video ->
            AVPMediaItem.LocalVideoItem(video)
          }.sortedByDescending { item -> item.video.dateAdded }
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
    TODO("Not yet implemented")
  }

  override fun searchFeed(
    query: String?,
    tab: Tab?
  ): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }

  override val defaultSettings: List<Setting>
    get() = listOf(
      SettingSwitch(
        context.getString(R.string.refresh_library_on_reload),
        "refresh_library",
        context.getString(R.string.refresh_library_on_reload_summary),
        false
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
      types = listOf(ExtensionType.DATABASE, ExtensionType.STREAM)
    )
  }
}
