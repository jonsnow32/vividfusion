package cloud.app.vvf.common.models

import cloud.app.vvf.common.models.ImageHolder.Companion.toUriImageHolder
import cloud.app.vvf.common.models.music.Track
import cloud.app.vvf.common.models.video.VideoCollection
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.common.utils.getYear
import cloud.app.vvf.common.utils.toLocalMonthYear
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed class AVPMediaItem {
  @Serializable
  data class TrackItem(val track: Track) : AVPMediaItem() {
    fun getSlug(): String {
      return track.uri
        .trim()
        .lowercase()
        .replace("[^a-z0-9\\s]".toRegex(), "")
        .replace("\\s+".toRegex(), "-")
    }
  }

  @Serializable
  data class VideoItem(val video: Video) : AVPMediaItem() {
    fun getSlug(): String {
      val formattedName = video.uri
        .trim()
        .lowercase()
        .replace("[^a-z0-9\\s]".toRegex(), "")
        .replace("\\s+".toRegex(), "-")
      return "$formattedName-${releaseYear}"
    }
  }

  @Serializable
  data class VideoCollectionItem(val album: VideoCollection) : AVPMediaItem() {
    fun getSlug(): String {
      val formattedName = album.uri
        .trim()
        .lowercase()
        .replace("[^a-z0-9\\s]".toRegex(), "")
        .replace("\\s+".toRegex(), "-")
      return "$formattedName-${releaseYear}"
    }
  }

  @Serializable
  data class PlaybackProgress(
    val item: AVPMediaItem,
    var position: Long,
    var duration: Long = 0L,
    var lastUpdated: Long = System.currentTimeMillis()
  ) : AVPMediaItem() {

    fun getSlug() = when (item) {
      is VideoItem -> item.getSlug()
      is TrackItem -> item.getSlug()
      else -> null
    }

    fun getName() = when (item) {
      is VideoItem -> item.video.title
      is TrackItem -> item.track.title
      else -> ""
    }

    fun getItemPoster() = when (item) {
      is VideoItem -> item.video.thumbnailUri?.toUriImageHolder()
      is TrackItem -> item.track.cover?.toUriImageHolder()
      else -> null
    }

    fun getPercent(): Int {
      if (duration <= 0) return 0
      return ((position.toFloat() / duration.toFloat()) * 100).toInt()
    }
  }

  companion object {
    fun List<AVPMediaItem>.toPaged() = this
  }

  fun sameAs(other: AVPMediaItem) = id == other.id

  val id
    get() = when (this) {
      is VideoCollectionItem -> getSlug()
      is VideoItem -> getSlug()
      is TrackItem -> getSlug()
      is PlaybackProgress -> getSlug()
    }

  val title
    get() = when (this) {
      is VideoCollectionItem -> album.title
      is VideoItem -> video.title
      is TrackItem -> track.title
      is PlaybackProgress -> getName()
    }

  val releaseYear
    get() = when (this) {
      is VideoItem -> video.addedTime?.getYear()
      is TrackItem -> track.releaseDate?.getYear()
      else -> null
    }

  val releaseMonthYear
    get() = when (this) {
      is VideoItem -> video.addedTime?.toLocalMonthYear()
      is TrackItem -> track.releaseDate?.toLocalMonthYear()
      else -> null
    }

  val poster
    get() = when (this) {
      is VideoItem -> video.thumbnailUri?.toUriImageHolder()
      is VideoCollectionItem -> album.poster.toUriImageHolder()
      is TrackItem -> track.cover?.toUriImageHolder()
      is PlaybackProgress -> getItemPoster()
    }
}
