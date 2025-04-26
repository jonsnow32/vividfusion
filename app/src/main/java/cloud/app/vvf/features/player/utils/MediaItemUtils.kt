package cloud.app.vvf.features.player.utils

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.music.Track
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.extension.builtIn.MediaUtils


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
        val metadata = video.toMetaData()
        item.setMediaMetadata(metadata)
        item.setMediaId(video.uri)
        item.setUri(video.uri)
        item.build()
      }

      is AVPMediaItem.TrackItem -> {
        val item = MediaItem.Builder()
        val metadata = track.toMetaData()
        item.setMediaMetadata(metadata)
        item.setMediaId(track.uri)
        item.setUri(track.uri)
        item.build()
      }

      else -> null
    }
  }

  private fun Video.toMetaData(): MediaMetadata {
    return MediaMetadata.Builder()
      .setArtworkUri(thumbnailUri?.toUri())
      .setTitle(title)
      .build()
  }

  private fun Track.toMetaData(): MediaMetadata {
    return MediaMetadata.Builder()
      .setArtworkUri(cover?.toUri() ?: MediaUtils.getPlaylistThumbnail(album)?.toUri())
      .setTitle(title)
      .build()
  }

}
