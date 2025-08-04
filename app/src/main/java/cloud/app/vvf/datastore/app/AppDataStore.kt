package cloud.app.vvf.datastore.app

import android.content.Context
import android.graphics.Color
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.PlaybackProgress
import cloud.app.vvf.common.models.DownloadItem
import cloud.app.vvf.common.models.DownloadStatus
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.user.User
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.datastore.app.helper.BOOKMARK_FOLDER
import cloud.app.vvf.datastore.app.helper.BookmarkItem
import cloud.app.vvf.datastore.app.helper.PlayerSettingItem
import cloud.app.vvf.datastore.app.helper.UriHistoryItem
import cloud.app.vvf.features.player.subtitle.DEF_SUBS_ELEVATION
import cloud.app.vvf.features.player.subtitle.SubtitleStyle


const val ExtensionFolder = "extensionDir"
const val FAVORITE_FOLDER = "favorites"
const val SEARCH_HISTORY_FOLDER = "search_history"
const val URI_HISTORY_FOLDER = "uri_history"
const val PLAYER_SETTING_FOLDER = "player_setting"
const val USERS_FOLDER = "users"
const val PlaybackProgressFolder = "history_progress"
const val DOWNLOAD_FOLDER = "downloads"

class AppDataStore(val context: Context, val account: Account) :
  DataStore(context.getSharedPreferences("account_${account.getSlug()}", Context.MODE_PRIVATE)) {

  fun getAllBookmarks(): List<BookmarkItem>? {
    return getAll<BookmarkItem>("$BOOKMARK_FOLDER/")?.sortedByDescending { it.lastUpdated }
  }

  fun addToBookmark(data: BookmarkItem?) {
    if (data == null) return
    set("$BOOKMARK_FOLDER/${data.item.id}", data)
  }

  fun addToBookmark(avpMediaItem: AVPMediaItem?, type: String) {
    if (avpMediaItem == null) return
    when (type) {
      "Watching" -> addToBookmark(BookmarkItem.Watching(0, null, avpMediaItem))
      "Completed" -> addToBookmark(BookmarkItem.Completed(null, avpMediaItem))
      "OnHold" -> addToBookmark(BookmarkItem.OnHold(avpMediaItem))
      "Dropped" -> addToBookmark(BookmarkItem.Dropped(avpMediaItem))
      "PlanToWatch" -> addToBookmark(BookmarkItem.PlanToWatch(avpMediaItem))
      else -> removeBookmark(avpMediaItem)
    }
  }

  fun findBookmark(avpMediaItem: AVPMediaItem?): BookmarkItem? {
    return get<BookmarkItem>("$BOOKMARK_FOLDER/${avpMediaItem?.id}")
  }

  fun removeBookmark(avpMediaItem: AVPMediaItem?) {
    if (avpMediaItem == null) return
    removeKey(
      "$BOOKMARK_FOLDER/${avpMediaItem.id}"
    )
  }


  fun getExtension(className: String): ExtensionMetadata? {
    return getExtensions()?.firstOrNull { it.className == className }
  }

  fun getExtensions(): List<ExtensionMetadata>? {
    return getAll<ExtensionMetadata>("$ExtensionFolder/")
  }

  fun saveExtensions(extensions: List<ExtensionMetadata>) {
    for (extension in extensions) {
      set("$ExtensionFolder/${extension.className}", extension)
    }
  }

  fun saveExtension(extension: ExtensionMetadata) {
    extension.lastUpdated = System.currentTimeMillis()
    return set("$ExtensionFolder/${extension.className}", extension)
  }

  fun getCurrentDBExtension(): ExtensionMetadata? {
    return get<ExtensionMetadata>("$ExtensionFolder/defaultDB/")
  }

  fun setCurrentDBExtension(extension: ExtensionMetadata): Boolean {
    set("$ExtensionFolder/defaultDB/", extension)
    return true
  }


  fun addFavoritesData(data: AVPMediaItem?) {
    if (data == null) return
    set("$FAVORITE_FOLDER/${data.id}", data)
  }

  fun removeFavoritesData(data: AVPMediaItem?) {
    if (data == null) return
    removeKey(
      "$FAVORITE_FOLDER/${data.id}"
    )
  }

  fun getFavorites(): List<AVPMediaItem>? {
    return getAll<AVPMediaItem>(FAVORITE_FOLDER)
  }

  fun getFavoritesData(slug: String?): Boolean {
    if (slug == null) return false
    val data = get<AVPMediaItem>("$FAVORITE_FOLDER/${slug}")
    return data != null;
  }

  fun updateProgress(data: PlaybackProgress): Boolean {
    data.lastUpdated = System.currentTimeMillis()
    if (data.item is AVPMediaItem.EpisodeItem || data.item is AVPMediaItem.MovieItem || data.item is AVPMediaItem.VideoItem) {
      set("$PlaybackProgressFolder/${data.item.id}", data)
      return true
    }
    return false
  }

  fun findPlaybackProgress(seasonItem: AVPMediaItem.SeasonItem?): List<PlaybackProgress>? {
    if (seasonItem == null) return null
    return getAll<PlaybackProgress>(
      "$PlaybackProgressFolder/"
    )?.mapNotNull { item ->
      when (item.item) {
        is AVPMediaItem.EpisodeItem -> {
          if ((item.item as AVPMediaItem.EpisodeItem).seasonItem.id == seasonItem.id)
            item
          else
            null
        }

        else -> null
      }
    }
    return null
  }

  fun findPlaybackProgress(mediaItem: AVPMediaItem): PlaybackProgress? =
    when (mediaItem) {
      is AVPMediaItem.EpisodeItem,
      is AVPMediaItem.MovieItem,
      is AVPMediaItem.VideoItem -> getAll<PlaybackProgress>("$PlaybackProgressFolder/${mediaItem.id}")?.maxByOrNull { it.lastUpdated }

      else -> null
    }

  fun findPlaybackProgress(slug: String): PlaybackProgress? =
    getAll<PlaybackProgress>("$PlaybackProgressFolder/$slug")?.maxByOrNull { it.lastUpdated }

  fun getWatchedEpisodeCount(seasonItem: AVPMediaItem.SeasonItem): Int {
    return count("$PlaybackProgressFolder/${seasonItem.id}")
  }

  fun getLatestPlaybackProgress(mediaItem: AVPMediaItem): PlaybackProgress? = when (mediaItem) {
    is AVPMediaItem.SeasonItem,
    is AVPMediaItem.ShowItem -> getAll<PlaybackProgress>("$PlaybackProgressFolder/${mediaItem.id}")?.maxByOrNull { it.lastUpdated }
    else -> null
  }

  fun getALlPlayback(): List<PlaybackProgress>? {
    val data = getAll<PlaybackProgress>("$PlaybackProgressFolder/")

    val episodePlayback = data?.filter { it.item is AVPMediaItem.EpisodeItem }
      ?.groupBy { (it.item as AVPMediaItem.EpisodeItem).seasonItem.showItem.getSlug() }
      ?.map { entry -> entry.value.first() }
    //?.maxBy { entry -> entry.value.maxBy { it.lastUpdated }.lastUpdated }

    val moviePlayback = data?.filter { it.item is AVPMediaItem.MovieItem }
    return episodePlayback?.plus(moviePlayback ?: emptyList())
      ?.sortedBy { item -> item.lastUpdated }
  }

  fun getSearchHistory(): List<SearchItem>? {
    return getAll<SearchItem>(
      "$SEARCH_HISTORY_FOLDER/"
    )?.sortedByDescending { it.searchedAt }
  }

  fun deleteHistorySearch(item: SearchItem) {
    return removeKey("$SEARCH_HISTORY_FOLDER/${item.id}")
  }

  fun clearHistorySearch() {
    return removeKey("$SEARCH_HISTORY_FOLDER/")
  }

  fun saveSearchHistory(item: SearchItem) {
    return set("$SEARCH_HISTORY_FOLDER/${item.id}", item)
  }

  fun saveUriHistory(item: UriHistoryItem) {
    return set("$URI_HISTORY_FOLDER/${item.id}", item)
  }
  fun getUriHistory(): List<UriHistoryItem>? {
    return getAll<UriHistoryItem>(
      "$URI_HISTORY_FOLDER/"
    )?.sortedByDescending { it.lastUpdated }
  }
  fun deleteUriHistory(item: UriHistoryItem) {
    return removeKey("$URI_HISTORY_FOLDER/${item.id}")
  }
  fun cleanUriHistory() {
    return removeKey("$URI_HISTORY_FOLDER/")
  }

  fun getCurrentUser(id: String?): User? {
    return get<User>("$USERS_FOLDER/${id}")
  }

  fun getAllUsers(id: String?): List<User>? {
    return getAll<User>("$USERS_FOLDER/")
  }

  @UnstableApi
  fun getPlayerSetting(): PlayerSettingItem {
    return get<PlayerSettingItem>("$PLAYER_SETTING_FOLDER/") ?: PlayerSettingItem(
      subtitleStyle = SubtitleStyle(
        foregroundColor = getDefColor(0),
        backgroundColor = getDefColor(2),
        windowColor = getDefColor(3),
        edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        edgeColor = getDefColor(1),
        typeface = null,
        typefaceFilePath = null,
        elevation = DEF_SUBS_ELEVATION,
        fixedTextSize = null,
      )
    )
  }

  private fun getDefColor(id: Int): Int {
    return when (id) {
      0 -> Color.WHITE
      1 -> Color.BLACK
      2 -> Color.TRANSPARENT
      3 -> Color.TRANSPARENT
      else -> Color.TRANSPARENT
    }
  }

  // Download functionality methods
  fun getAllDownloads(): List<DownloadItem>? {
    return getAll<DownloadItem>("$DOWNLOAD_FOLDER/")?.sortedByDescending { it.updatedAt }
  }

  fun saveDownload(downloadItem: DownloadItem) {
    val updatedItem = downloadItem.copy(updatedAt = System.currentTimeMillis())
    set("$DOWNLOAD_FOLDER/${downloadItem.id}", updatedItem)
  }

  fun getDownload(downloadId: String): DownloadItem? {
    return get<DownloadItem>("$DOWNLOAD_FOLDER/$downloadId")
  }

  fun removeDownload(downloadId: String) {
    removeKey("$DOWNLOAD_FOLDER/$downloadId")
  }

  fun removeDownload(downloadItem: DownloadItem) {
    removeKey("$DOWNLOAD_FOLDER/${downloadItem.id}")
  }

  fun updateDownloadProgress(downloadId: String, progress: Int, downloadedBytes: Long) {
    val downloadItem = getDownload(downloadId)
    downloadItem?.let { item ->
      val updatedItem = item.copy(
        progress = progress,
        downloadedBytes = downloadedBytes,
        updatedAt = System.currentTimeMillis()
      )
      saveDownload(updatedItem)
    }
  }

  fun updateDownloadStatus(downloadId: String, status: DownloadStatus) {
    val downloadItem = getDownload(downloadId)
    downloadItem?.let { item ->
      val updatedItem = item.copy(
        status = status,
        updatedAt = System.currentTimeMillis()
      )
      saveDownload(updatedItem)
    }
  }

  fun getDownloadsByStatus(status: DownloadStatus): List<DownloadItem>? {
    return getAllDownloads()?.filter { it.status == status }
  }

  fun getActiveDownloads(): List<DownloadItem>? {
    return getAllDownloads()?.filter { it.isActive() }
  }

  fun getCompletedDownloads(): List<DownloadItem>? {
    return getAllDownloads()?.filter { it.isCompleted() }
  }

  fun clearAllDownloads() {
    removeKey("$DOWNLOAD_FOLDER/")
  }

  fun getDownloadByMediaItem(mediaItem: AVPMediaItem): DownloadItem? {
    return getAllDownloads()?.firstOrNull { it.mediaItem.id == mediaItem.id }
  }
}
