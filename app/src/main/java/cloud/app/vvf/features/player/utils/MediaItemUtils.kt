package cloud.app.vvf.features.player.utils

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.movie.LocalVideo
import cloud.app.vvf.common.models.stream.StreamData


object MediaItemUtils {

  fun Uri.toMediaItem(): MediaItem {
    val item = MediaItem.Builder()
    item.setMediaId(this.toString())
    item.setUri(this)
    return item.build()
  }

  fun List<AVPMediaItem>.toMediaItems() = mapNotNull { item -> item.toMediaItem() }
  fun AVPMediaItem.toMediaItem(): MediaItem? {
    return when (this) {
      is AVPMediaItem.LocalVideoItem -> {
        val item = MediaItem.Builder()
        val metadata = video.toMetaData()
        item.setMediaMetadata(metadata)
        item.setMediaId(video.uri)
        item.setUri(video.uri)
        item.build()
      }

      is AVPMediaItem.StreamItem -> {
        val item = MediaItem.Builder()
        val metadata = streamData.toMetaData()
        item.setMediaMetadata(metadata)
        item.setMediaId(id.toString())
        item.setUri(streamData.resolvedUrl.toString())
        item.build()
      }
      else -> null
    }
  }

  private fun LocalVideo.toMetaData(): MediaMetadata {
    return MediaMetadata.Builder()
      .build()
  }

  private fun StreamData.toMetaData(): MediaMetadata {
    return MediaMetadata.Builder().build()
  }
}
