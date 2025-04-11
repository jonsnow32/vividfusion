package cloud.app.vvf.features.player.utils

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.video.VVFVideo
import cloud.app.vvf.common.models.video.VVFVideo.LocalVideo


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
      is AVPMediaItem.VideoItem -> {
        val item = MediaItem.Builder()
        val metadata = vvfVideo.toMetaData()
        item.setMediaMetadata(metadata)
        item.setMediaId(vvfVideo.uri)
        item.setUri(vvfVideo.uri)
        item.build()
      }
      else -> null
    }
  }

  private fun VVFVideo.toMetaData(): MediaMetadata {
    return MediaMetadata.Builder()
      .build()
  }

}
