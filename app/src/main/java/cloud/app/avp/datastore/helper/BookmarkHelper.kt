package cloud.app.avp.datastore.helper

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cloud.app.avp.R
import cloud.app.avp.datastore.DataStore
import cloud.app.common.models.AVPMediaItem

const val BOOKMARK_FOLDER = "watchlist"

enum class BookMarkType(val internalId: Int, @StringRes val stringRes: Int, @DrawableRes val iconRes: Int) {
  WATCHING(0, R.string.type_watching, R.drawable.anim_bookmark),
  COMPLETED(1, R.string.type_completed, R.drawable.anim_bookmark),
  ONHOLD(2, R.string.type_on_hold, R.drawable.anim_bookmark),
  DROPPED(3, R.string.type_dropped, R.drawable.anim_bookmark),
  PLANTOWATCH(4, R.string.type_plan_to_watch, R.drawable.anim_bookmark),
  NONE(5, R.string.type_none, R.drawable.ic_add_20dp);

  companion object {
    fun fromInternalId(id: Int?) = entries.find { value -> value.internalId == id } ?: NONE
  }
}
fun DataStore.addToBookmark(data: AVPMediaItem?) {
  if (data == null) return
  setKey("$BOOKMARK_FOLDER/${data.id}", data)
}

fun DataStore.removeBookmark(data: AVPMediaItem?) {
  if (data == null) return
  removeKey(
    "$BOOKMARK_FOLDER/${data.id}"
  )
}

//fun DataStore.getWatchListData(hashCodes: Int?): Boolean {
//  if (hashCodes == null) return false
//  val data = getKey("$BOOKMARK_FOLDER/${hashCodes}", null)
//  return data != null;
//}
