package cloud.app.vvf.datastore.app.helper

import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.datastore.app.AppDataStore

const val FAVORITE_FOLDER = "favorites"
fun AppDataStore.addFavoritesData(data: AVPMediaItem?) {
  if (data == null) return
  setKey("$FAVORITE_FOLDER/${data.id}", data)
}

fun AppDataStore.removeFavoritesData(data: AVPMediaItem?) {
  if (data == null) return
  removeKey(
    "$FAVORITE_FOLDER/${data.id}"
  )
}

fun AppDataStore.getFavorites(): List<AVPMediaItem>? {
  return getKeys<AVPMediaItem>(FAVORITE_FOLDER, null)
}

inline fun <reified T: AVPMediaItem> AppDataStore.getFavoritesData(slug: String?): Boolean {
  if (slug == null) return false
  val data = getKey<T>("$FAVORITE_FOLDER/${slug}", null)
  return data != null;
}


