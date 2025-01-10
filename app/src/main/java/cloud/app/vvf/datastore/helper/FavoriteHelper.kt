package cloud.app.vvf.datastore.helper

import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType

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

fun DataStore.getFavorites(): List<AVPMediaItem>? {
  return getKeys<AVPMediaItem>(FAVORITE_FOLDER, null)
}

inline fun <reified T: AVPMediaItem> DataStore.getFavoritesData(slug: String?): Boolean {
  if (slug == null) return false
  val data = getKey<T>("$FAVORITE_FOLDER/${slug}", null)
  return data != null;
}


