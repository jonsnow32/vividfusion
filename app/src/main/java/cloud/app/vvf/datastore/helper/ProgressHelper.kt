package cloud.app.vvf.datastore.helper

import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.common.models.AVPMediaItem
import kotlinx.serialization.Serializable

const val PlaybackProgressFolder = "progress"

@Serializable
data class PlaybackProgress(
  val item: AVPMediaItem,
  val position: Long,
  val duration: Long? = null,
  val lastUpdated: Long = System.currentTimeMillis()
) {
  fun getEpisode(): AVPMediaItem.EpisodeItem? {
    return item as? AVPMediaItem.EpisodeItem
  }
}


fun DataStore.savePlaybackProgress(data: PlaybackProgress): Boolean {
  if (data.item is AVPMediaItem.EpisodeItem || data.item is AVPMediaItem.MovieItem) {
    setKey("$PlaybackProgressFolder/${data.item.id}", data)
    return true
  }
  return false
}

fun DataStore.getPlaybackProgress(mediaItem: AVPMediaItem): PlaybackProgress? {
  if (mediaItem is AVPMediaItem.EpisodeItem || mediaItem is AVPMediaItem.MovieItem) {
    return getKey<PlaybackProgress>("$PlaybackProgressFolder/${mediaItem.id}", null)
  }
  return null
}

fun DataStore.getLatestPlaybackProgress(mediaItem: AVPMediaItem): PlaybackProgress? = when (mediaItem) {
  is AVPMediaItem.SeasonItem,
  is AVPMediaItem.ShowItem -> getKeys("$PlaybackProgressFolder/${mediaItem.id}").mapNotNull {
    getKey<PlaybackProgress>(
      it,
      null
    )
  }.maxByOrNull { it.lastUpdated }
  else -> null
}
