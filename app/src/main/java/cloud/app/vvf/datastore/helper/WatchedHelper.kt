package cloud.app.vvf.datastore.helper

import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.common.models.AVPMediaItem
import kotlinx.serialization.Serializable

const val WatchedFolder = "wtedDir"

@Serializable
data class WatchedItem(
  val item: AVPMediaItem,
  val position: Long,
  val duration: Long? = null,
  val lastUpdated: Long = System.currentTimeMillis()
)


fun DataStore.setWatched(data: WatchedItem): Boolean {
  if (data.item is AVPMediaItem.EpisodeItem || data.item is AVPMediaItem.MovieItem) {
    setKey("$WatchedFolder/${data.item.id}", data)
    return true
  }
  return false
}

fun DataStore.getWatched(item: AVPMediaItem): WatchedItem? {
  if (item is AVPMediaItem.EpisodeItem || item is AVPMediaItem.MovieItem) {
    return getKey<WatchedItem>("$WatchedFolder/${item.id}", null)
  }
  return null
}

fun DataStore.getLastWatched(item: AVPMediaItem): WatchedItem? = when (item) {
  is AVPMediaItem.SeasonItem,
  is AVPMediaItem.ShowItem -> getKeys("$WatchedFolder/${item.id}").mapNotNull {
    getKey<WatchedItem>(
      it,
      null
    )
  }.maxByOrNull { it.lastUpdated }
  else -> null
}

