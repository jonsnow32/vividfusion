package cloud.app.avp.datastore.helper

import cloud.app.avp.datastore.DataStore
import cloud.app.common.models.AVPMediaItem

const val FAVORITE_FOLDER = "favorites"
fun DataStore.addFavoritesData(data: AVPMediaItem?) {
  if (data == null) return
  setKey("$FAVORITE_FOLDER/${data.id}", data)
}

fun DataStore.removeFavoritesData(data: AVPMediaItem?) {
  if (data == null) return
  removeKey(
    "$FAVORITE_FOLDER/${data.id}"
  )
}

fun DataStore.getFavoritesData(slug: String?): Boolean {
  if (slug == null) return false
  val data = getKey("$FAVORITE_FOLDER/${slug}", null)
  return data != null;
}
