package cloud.app.vvf.datastore.app

import android.content.Context
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.PlaybackProgressItem
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.User
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.datastore.app.helper.BOOKMARK_FOLDER
import cloud.app.vvf.datastore.app.helper.BookmarkItem


const val ExtensionFolder = "extensionDir"
const val FAVORITE_FOLDER = "favorites"
const val SEARCH_HISTORY_FOLDER = "search_history"
const val USERS_FOLDER = "users"
const val PlaybackProgressFolder = "progress"

class AppDataStore(val context: Context, val account: Account) :
  DataStore(context.getSharedPreferences("account_${account.getSlug()}", Context.MODE_PRIVATE)) {

  fun getAllBookmarks(): List<BookmarkItem>? {
    return getKeys<BookmarkItem>("$BOOKMARK_FOLDER/", null)?.sortedByDescending { it.lastUpdated }
  }

  fun addToBookmark(data: BookmarkItem?) {
    if (data == null) return
    setKey("$BOOKMARK_FOLDER/${data.item.id}", data)
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
    return getKey<BookmarkItem>("$BOOKMARK_FOLDER/${avpMediaItem?.id}", null)
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
    return getKeys<ExtensionMetadata>("$ExtensionFolder/", null)
  }

  fun saveExtensions(extensions: List<ExtensionMetadata>) {
    for (extension in extensions) {
      setKey("$ExtensionFolder/${extension.className}", extension)
    }
  }

  fun saveExtension(extension: ExtensionMetadata) {
    extension.lastUpdated = System.currentTimeMillis()
    return setKey("$ExtensionFolder/${extension.className}", extension)
  }

  fun getCurrentDBExtension(): ExtensionMetadata? {
    return getKey<ExtensionMetadata>("$ExtensionFolder/defaultDB/", null)
  }

  fun setCurrentDBExtension(extension: ExtensionMetadata): Boolean {
    setKey("$ExtensionFolder/defaultDB/", extension)
    return true
  }


  fun addFavoritesData(data: AVPMediaItem?) {
    if (data == null) return
    setKey("$FAVORITE_FOLDER/${data.id}", data)
  }

  fun removeFavoritesData(data: AVPMediaItem?) {
    if (data == null) return
    removeKey(
      "$FAVORITE_FOLDER/${data.id}"
    )
  }

  fun getFavorites(): List<AVPMediaItem>? {
    return getKeys<AVPMediaItem>(FAVORITE_FOLDER, null)
  }

  fun getFavoritesData(slug: String?): Boolean {
    if (slug == null) return false
    val data = getKey<AVPMediaItem>("$FAVORITE_FOLDER/${slug}", null)
    return data != null;
  }

  fun savePlaybackProgress(data: PlaybackProgressItem): Boolean {
    if (data.item is AVPMediaItem.EpisodeItem || data.item is AVPMediaItem.MovieItem) {
      setKey("$PlaybackProgressFolder/${data.item.id}", data)
      return true
    }
    return false
  }

  fun getPlaybackProgressOfSeason(seasonItem: AVPMediaItem.SeasonItem?): List<PlaybackProgressItem>? {
    if (seasonItem == null) return null
    return getKeys<PlaybackProgressItem>(
      "$PlaybackProgressFolder/",
      null
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


  fun getPlaybackProgress(mediaItem: AVPMediaItem): PlaybackProgressItem? {
    if (mediaItem is AVPMediaItem.EpisodeItem || mediaItem is AVPMediaItem.MovieItem) {
      return getKey<PlaybackProgressItem>("$PlaybackProgressFolder/${mediaItem.id}", null)
    }
    return null
  }

  fun getLatestPlaybackProgress(mediaItem: AVPMediaItem): PlaybackProgressItem? = when (mediaItem) {
    is AVPMediaItem.SeasonItem,
    is AVPMediaItem.ShowItem -> getKeys("$PlaybackProgressFolder/${mediaItem.id}").mapNotNull {
      getKey<PlaybackProgressItem>(
        it,
        null
      )
    }.maxByOrNull { it.lastUpdated }

    else -> null
  }

  fun getALlPlayback(): List<PlaybackProgressItem>? {
    val data = getKeys<PlaybackProgressItem>("$PlaybackProgressFolder/", null)

    val episodePlayback = data?.filter { it.item is AVPMediaItem.EpisodeItem }
      ?.groupBy { (it.item as AVPMediaItem.EpisodeItem).seasonItem.showItem.getSlug() }
      ?.map { entry -> entry.value.first() }
    //?.maxBy { entry -> entry.value.maxBy { it.lastUpdated }.lastUpdated }

    val moviePlayback = data?.filter { it.item is AVPMediaItem.MovieItem }
    return episodePlayback?.plus(moviePlayback ?: emptyList())
      ?.sortedBy { item -> item.lastUpdated }
  }

  fun getSearchHistory(): List<SearchItem>? {
    return getKeys<SearchItem>(
      "$SEARCH_HISTORY_FOLDER/",
      null
    )?.sortedByDescending { it.searchedAt }
  }

  fun deleteHistorySearch(item: SearchItem) {
    return removeKey("$SEARCH_HISTORY_FOLDER/${item.id}")
  }

  fun clearHistorySearch() {
    return removeKey("$SEARCH_HISTORY_FOLDER/")
  }

  fun saveSearchHistory(item: SearchItem) {
    return setKey("$SEARCH_HISTORY_FOLDER/${item.id}", item)
  }


  fun getCurrentUser(id: String?): User? {
    return getKey<User>("$USERS_FOLDER/${id}", null)
  }

  fun getAllUsers(id: String?): List<User>? {
    return getKeys<User>("$USERS_FOLDER/", null)
  }

}
