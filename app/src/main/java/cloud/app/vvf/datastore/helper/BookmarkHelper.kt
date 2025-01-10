package cloud.app.vvf.datastore.helper

import android.provider.ContactsContract.Data
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cloud.app.vvf.R
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.common.models.AVPMediaItem
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
  }
}

fun DataStore.getAllBookmarks(): List<BookmarkItem>? {
  return getKeys<BookmarkItem>("$BOOKMARK_FOLDER/", null)
}


fun DataStore.addToBookmark(data: BookmarkItem?) {
  if (data == null) return
  setKey("$BOOKMARK_FOLDER/${data.item.id}", data)
}

fun DataStore.addToBookmark(avpMediaItem: AVPMediaItem?, type: String) {
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

fun DataStore.findBookmark(avpMediaItem: AVPMediaItem?): BookmarkItem? {
  return getKey<BookmarkItem>("$BOOKMARK_FOLDER/${avpMediaItem?.id}", null)
}

fun DataStore.removeBookmark(avpMediaItem: AVPMediaItem?) {
  if (avpMediaItem == null) return
  removeKey(
    "$BOOKMARK_FOLDER/${avpMediaItem.id}"
  )
}



