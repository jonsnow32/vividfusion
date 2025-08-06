package cloud.app.vvf.common.models


import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.ImageHolder.Companion.toUriImageHolder
import cloud.app.vvf.common.models.actor.Actor
import cloud.app.vvf.common.models.movie.Episode
import cloud.app.vvf.common.models.movie.Movie
import cloud.app.vvf.common.models.movie.Season
import cloud.app.vvf.common.models.movie.Show
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
  data class DownloadItem(
    val mediaItem: AVPMediaItem,
    val url: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val localPath: String? = null,
    val downloadSpeed: Long = 0L, // bytes per second
    val connections: Int = 1, // number of download connections
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
  ) : AVPMediaItem(){

    fun getSlug(): String{
      return when (mediaItem) {
        is MovieItem -> "${mediaItem.getSlug()}-download-${url.hashCode()}"
        is ShowItem -> "${mediaItem.getSlug()}-download-${url.hashCode()}"
        is EpisodeItem -> "${mediaItem.getSlug()}-download-${url.hashCode()}"
        is VideoItem -> "${mediaItem.getSlug()}-download-${url.hashCode()}"
        is TrackItem -> "${mediaItem.getSlug()}-download-${url.hashCode()}"
        is VideoCollectionItem -> "${mediaItem.getSlug()}-download-${url.hashCode()}"
        else -> "download-${fileName.replace("[^a-z0-9\\s]".toRegex(), "").replace("\\s+".toRegex(), "-")}-${url.hashCode()}"
      }
    }

    fun getProgressPercentage(): Int {
      return if (fileSize > 0) {
        ((downloadedBytes.toFloat() / fileSize.toFloat()) * 100).toInt()
      } else progress
    }

    fun isCompleted(): Boolean = status == DownloadStatus.COMPLETED

    fun canRetry(): Boolean = status in listOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED)

    fun isActive(): Boolean = status in listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PENDING)

    fun getFormattedSpeed(): String {
      if (downloadSpeed <= 0) return "0 B/s"

      val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
      val digitGroups = (kotlin.math.log10(downloadSpeed.toDouble()) / kotlin.math.log10(1024.0)).toInt()

      return String.format(
        java.util.Locale.getDefault(),
        "%.1f %s",
        downloadSpeed / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
      )
    }

    fun getEstimatedTimeRemaining(): String {
      if (downloadSpeed <= 0 || fileSize <= downloadedBytes) return "Unknown"

      val remainingBytes = fileSize - downloadedBytes
      val remainingSeconds = remainingBytes / downloadSpeed

      return when {
        remainingSeconds < 60 -> "${remainingSeconds}s"
        remainingSeconds < 3600 -> "${remainingSeconds / 60}m ${remainingSeconds % 60}s"
        else -> "${remainingSeconds / 3600}h ${(remainingSeconds % 3600) / 60}m"
      }
    }
  }

  @Serializable
  enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
  }

  @Serializable
  data class TrackItem(val track: Track) : AVPMediaItem() {
    fun getSlug(): String {
      val formattedName = track.uri
        .trim()
        .lowercase()
        .replace("[^a-z0-9\\s]".toRegex(), "") // Remove special characters
        .replace("\\s+".toRegex(), "-")
      return formattedName
    }
  }

  @Serializable
  data class VideoItem(val video: Video) : AVPMediaItem() {
    fun getSlug(): String {
      val formattedName = video.uri
        .trim()
        .lowercase()
        .replace("[^a-z0-9\\s]".toRegex(), "") // Remove special characters
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
        .replace("[^a-z0-9\\s]".toRegex(), "") // Remove special characters
        .replace("\\s+".toRegex(), "-")
      return "$formattedName-${releaseYear}"
    }
  }

  @Serializable
  data class MovieItem(val movie: Movie) : AVPMediaItem() {
    fun getSlug(): String {
      val formattedName = movie.generalInfo.originalTitle
        .trim()
        .lowercase()
        .replace("[^a-z0-9\\s]".toRegex(), "") // Remove special characters
        .replace("\\s+".toRegex(), "-")
      return "$formattedName-${releaseYear}"
    }
  }

  @Serializable
  data class ShowItem(val show: Show) : AVPMediaItem() {
    fun getSlug(): String {
      val formattedName = show.generalInfo.originalTitle
        .trim()
        .lowercase()
        .replace("[^a-z0-9\\s]".toRegex(), "") // Remove special characters
        .replace("\\s+".toRegex(), "-")
      return "tv-$formattedName-${releaseYear}"
    }
  }

  @Serializable
  data class EpisodeItem(
    val episode: Episode,
    val seasonItem: SeasonItem,
    val nextEpisode: Episode? = null
  ) : AVPMediaItem() {
    fun getSlug() = "${seasonItem.getSlug()}/${episode.episodeNumber}"
  }


  @Serializable
  data class SeasonItem(
    val season: Season,
    val showItem: ShowItem,
    var watchedEpisodeNumber: Int? = 0
  ) : AVPMediaItem() {
    fun getSlug() = "${showItem.getSlug()}/${season.number}"
  }


  @Serializable
  data class ActorItem(val actor: Actor) : AVPMediaItem()

  @Serializable
  data class TrailerItem(val video: Video) : AVPMediaItem()

  @Serializable
  data class PlaybackProgress(
    val item: AVPMediaItem,
    var position: Long,
    var duration: Long = 0L,
    var lastUpdated: Long = System.currentTimeMillis()
  ) : AVPMediaItem() {

    fun getSlug() = when (item) {
      is EpisodeItem -> item.getSlug()
      is MovieItem -> item.getSlug()
      is VideoItem -> item.getSlug()
      is TrackItem -> item.getSlug()
      else -> null
    }

    fun getName() = when (item) {
      is MovieItem -> item.movie.generalInfo.title
      is EpisodeItem -> if (item.episode.generalInfo.title.isEmpty()) "Episode ${item.episode.episodeNumber}" else item.episode.generalInfo.title
      is VideoItem -> item.video.title
      is TrackItem -> item.track.title
      else -> ""
    }

    fun getItemPoster() = when (item) {
      is MovieItem -> item.movie.generalInfo.poster?.toImageHolder()
      is EpisodeItem -> item.seasonItem.showItem.generalInfo?.poster?.toImageHolder()
      is VideoItem -> item.video.thumbnailUri?.toUriImageHolder()
      is TrackItem -> item.track.cover?.toImageHolder()
      else -> null
    }

    fun getItemBackdrop() = when (item) {
      is MovieItem -> item.movie.generalInfo.backdrop?.toImageHolder()
      is EpisodeItem -> item.seasonItem.showItem.generalInfo?.backdrop?.toImageHolder()
      else -> null
    }

    fun getPercent(): Int {
      if (duration <= 0) return 0
      return ((position.toFloat() / duration.toFloat()) * 100).toInt()
    }

    fun getEpisode(): EpisodeItem? {
      if (item !is EpisodeItem) return null
      return item
    }
  }


  companion object {
    fun Actor.toMediaItem() = ActorItem(this)
    fun Movie.toMediaItem() = MovieItem(this)
    fun Show.toMediaItem() = ShowItem(this)
    fun Episode.toMediaItem(season: SeasonItem) = EpisodeItem(this, season)
    fun Season.toMediaItem(showItem: ShowItem) = SeasonItem(this, showItem)

    fun toMediaItemsContainer(
      title: String, subtitle: String? = null, more: PagedData<AVPMediaItem>? = null
    ) = MediaItemsContainer.Category(title, subtitle, more)

    fun List<AVPMediaItem>.toPaged() = PagedData.Single { this }

  }


  fun sameAs(other: AVPMediaItem) = id == other.id

  val id
    get() = when (this) {
      is ActorItem -> actor.name.hashCode()
      is MovieItem -> getSlug()
      is ShowItem -> getSlug()
      is EpisodeItem -> getSlug()
      is SeasonItem -> getSlug()
      is TrailerItem -> video.uri
      is PlaybackProgress -> getSlug()
      is VideoCollectionItem -> getSlug()
      is VideoItem -> getSlug()
      is TrackItem -> getSlug()
      is DownloadItem -> getSlug()
    }

  val title
    get() = when (this) {
      is ActorItem -> actor.name
      is MovieItem -> movie.generalInfo.title
      is ShowItem -> show.generalInfo.title
      is EpisodeItem -> if (episode.generalInfo.title.isEmpty()) "Episode ${episode.episodeNumber}" else episode.generalInfo.title
      is SeasonItem -> if (season.generalInfo.title.isEmpty()) "Season ${season.number}" else season.generalInfo.title
      is TrailerItem -> video.title
      is PlaybackProgress -> getName()
      is VideoCollectionItem -> album.title
      is VideoItem -> video.title
      is TrackItem -> track.title
      is DownloadItem -> fileName
    }

  val releaseYear
    get() = when (this) {
      is ActorItem -> actor.name
      is MovieItem -> movie.generalInfo.getReleaseYear()
      is ShowItem -> show.generalInfo.getReleaseYear()
      is EpisodeItem -> episode.generalInfo.getReleaseYear()
      is SeasonItem -> season.releaseDateMsUTC?.getYear()
      is VideoItem -> video.addedTime?.getYear()
      is TrackItem -> track.releaseDate?.getYear()

      else -> null
    }

  val releaseMonthYear
    get() = when (this) {
      is MovieItem -> movie.generalInfo.releaseDateMsUTC?.toLocalMonthYear()
      is ShowItem -> show.generalInfo.releaseDateMsUTC?.toLocalMonthYear()
      is EpisodeItem -> episode.generalInfo.releaseDateMsUTC?.toLocalMonthYear()
      is SeasonItem -> season.releaseDateMsUTC?.toLocalMonthYear()
      is VideoItem -> video.addedTime?.toLocalMonthYear()
      is TrackItem -> track.releaseDate?.toLocalMonthYear()
      else -> null
    }

  val generalInfo
    get() = when (this) {
      is MovieItem -> movie.generalInfo
      is ShowItem -> show.generalInfo
      is EpisodeItem -> episode.generalInfo
      is SeasonItem -> season.generalInfo
      else -> null
    }

  val recommendations
    get() = when (this) {
      is ShowItem -> show.recommendations
      is MovieItem -> movie.recommendations
      else -> null
    }

  val poster
    get() = when (this) {
      is ActorItem -> actor.image
      is MovieItem -> movie.generalInfo.poster?.toImageHolder()
      is ShowItem -> show.generalInfo.poster?.toImageHolder()
      is EpisodeItem -> seasonItem.showItem.generalInfo?.poster?.toImageHolder()
      is SeasonItem -> season.generalInfo.poster?.toImageHolder()
      is PlaybackProgress -> getItemPoster()
      is VideoItem -> video.thumbnailUri?.toUriImageHolder()
      is VideoCollectionItem -> album.poster.toUriImageHolder()
      is TrackItem -> track.cover?.toUriImageHolder()
      else -> null
    }

  val backdrop
    get() = when (this) {
      is ActorItem -> actor.image
      is MovieItem -> movie.generalInfo.backdrop?.toImageHolder()
      is ShowItem -> show.generalInfo.backdrop?.toImageHolder()
      is EpisodeItem -> episode.generalInfo.backdrop?.toImageHolder()
        ?: seasonItem.showItem.generalInfo?.backdrop?.toImageHolder()

      is SeasonItem -> season.generalInfo.backdrop?.toImageHolder()
        ?: showItem.generalInfo?.backdrop?.toImageHolder()

      is PlaybackProgress -> getItemBackdrop()
      else -> null
    }

  val subtitle
    get() = when (this) {
      is ActorItem -> actor.role
      is MovieItem -> movie.generalInfo.genres?.firstOrNull() ?: ""
      is ShowItem -> show.generalInfo.genres?.firstOrNull() ?: ""
      is EpisodeItem -> seasonItem.showItem.title
      is PlaybackProgress -> when (item) {
        is MovieItem -> item.movie.generalInfo.genres?.firstOrNull() ?: ""
        is EpisodeItem -> item.seasonItem.showItem.title
        else -> ""
      }

      else -> null
    }

  val overview
    get() = when (this) {
      is MovieItem -> movie.generalInfo.overview ?: ""
      is ShowItem -> show.generalInfo.overview ?: ""
      is EpisodeItem -> episode.generalInfo.overview ?: ""
      is SeasonItem -> season.generalInfo.overview ?: ""
      is PlaybackProgress -> item.generalInfo?.overview ?: ""
      else -> ""
    }


  val rating
    get() = when (this) {
      is MovieItem -> movie.generalInfo.rating
      is ShowItem -> show.generalInfo.rating
      is EpisodeItem -> episode.generalInfo.rating
      else -> null
    }

  val homePage
    get() = when (this) {
      is ActorItem -> null
      is EpisodeItem -> seasonItem.showItem.show.generalInfo.homepage
      is MovieItem -> movie.generalInfo.homepage
      is SeasonItem -> showItem.show.generalInfo.homepage
      is ShowItem -> show.generalInfo.homepage
      else -> null
    }

  val tagline
    get() = when (this) {
      is MovieItem -> movie.tagline
      is ShowItem -> show.tagLine
      else -> null

    }
}
