package cloud.app.vvf.datastore.app.helper

import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.datastore.app.AppDataStore
import kotlinx.serialization.Serializable

const val BOOKMARK_FOLDER = "bookmarks"

@Serializable
sealed class BookmarkItem {
  abstract val item: AVPMediaItem
  abstract val lastUpdated: Long

  @Serializable
  data class Watching(
    val position: Long,
    val duration: Long? = null,
    override val item: AVPMediaItem,
    override val lastUpdated: Long = System.currentTimeMillis()
  ) : BookmarkItem()

  @Serializable
  data class Completed(
    val duration: Long? = null,
    override val item: AVPMediaItem,
    override val lastUpdated: Long = System.currentTimeMillis()
  ) : BookmarkItem()

  @Serializable
  data class OnHold(
    override val item: AVPMediaItem,
    override val lastUpdated: Long = System.currentTimeMillis()
  ) : BookmarkItem()

  @Serializable
  data class Dropped(
    override val item: AVPMediaItem,
    override val lastUpdated: Long = System.currentTimeMillis()
  ) : BookmarkItem()

  @Serializable
  data class PlanToWatch(
    override val item: AVPMediaItem,
    override val lastUpdated: Long = System.currentTimeMillis()
  ) : BookmarkItem()

  companion object {
    fun getBookmarkItemSubclasses(): List<String> {
      return BookmarkItem::class.sealedSubclasses.map { it.simpleName ?: "Unnamed" }
    }
    fun getStringIds(bookmarkItem: BookmarkItem?) : Int{
      return when(bookmarkItem){
        is Completed -> R.string.type_completed
        is Dropped -> R.string.type_dropped
        is OnHold -> R.string.type_on_hold
        is PlanToWatch -> R.string.type_plan_to_watch
        is Watching -> R.string.type_watching
        else -> R.string.type_none
      }
    }
    fun getStringIds(type: String) : Int{
      return when(type) {
        "Watching" -> R.string.type_watching
        "Completed" -> R.string.type_completed
        "OnHold" -> R.string.type_on_hold
        "Dropped" -> R.string.type_dropped
        "PlanToWatch" -> R.string.type_plan_to_watch
        else -> R.string.type_none
      }
    }
  }
}

fun AppDataStore.getAllBookmarks(): List<BookmarkItem>? {
  return getKeys<BookmarkItem>("$BOOKMARK_FOLDER/", null)?.sortedByDescending { it.lastUpdated }
}

fun AppDataStore.addToBookmark(data: BookmarkItem?) {
  if (data == null) return
  setKey("$BOOKMARK_FOLDER/${data.item.id}", data)
}

fun AppDataStore.addToBookmark(avpMediaItem: AVPMediaItem?, type: String) {
  if (avpMediaItem == null) return
  when(type) {
    "Watching" -> addToBookmark( BookmarkItem.Watching(0, null, avpMediaItem))
    "Completed" -> addToBookmark( BookmarkItem.Completed(null, avpMediaItem))
    "OnHold" -> addToBookmark( BookmarkItem.OnHold(avpMediaItem))
    "Dropped" -> addToBookmark( BookmarkItem.Dropped(avpMediaItem))
    "PlanToWatch" -> addToBookmark( BookmarkItem.PlanToWatch(avpMediaItem))
    else -> removeBookmark(avpMediaItem)
  }
}

fun AppDataStore.findBookmark(avpMediaItem: AVPMediaItem?): BookmarkItem? {
  return getKey<BookmarkItem>("$BOOKMARK_FOLDER/${avpMediaItem?.id}", null)
}

fun AppDataStore.removeBookmark(avpMediaItem: AVPMediaItem?) {
  if (avpMediaItem == null) return
  removeKey(
    "$BOOKMARK_FOLDER/${avpMediaItem.id}"
  )
}



