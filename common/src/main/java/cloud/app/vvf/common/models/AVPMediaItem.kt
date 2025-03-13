package cloud.app.vvf.common.models


import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.movie.Episode
import cloud.app.vvf.common.models.movie.Movie
import cloud.app.vvf.common.models.movie.Season
import cloud.app.vvf.common.models.movie.Show
import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.common.utils.getYear
import cloud.app.vvf.common.utils.toLocalMonthYear
import kotlinx.serialization.Serializable

@Serializable
sealed class AVPMediaItem {

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
  data class EpisodeItem(val episode: Episode, val seasonItem: SeasonItem) : AVPMediaItem() {
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
  data class StreamItem(val streamData: StreamData) : AVPMediaItem()

  @Serializable
  data class TrailerItem(val streamData: StreamData) : AVPMediaItem()

  @Serializable
  data class PlaybackProgressItem(
    val item: AVPMediaItem,
    val position: Long,
    val duration: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
  ): AVPMediaItem() {
    fun getEpisode(): AVPMediaItem.EpisodeItem? {
      return item as? AVPMediaItem.EpisodeItem
    }
    fun getSlug() = when(item) {
      is EpisodeItem -> item.getSlug()
      is MovieItem -> item.getSlug()
      else -> null
    }
    fun getName() = when(item) {
      is MovieItem -> item.movie.generalInfo.title
      is EpisodeItem -> if(item.episode.generalInfo.title.isEmpty())  "Episode ${item.episode.episodeNumber}" else item.episode.generalInfo.title
      else -> ""
    }
    fun getPoster() = when(item) {
      is MovieItem -> item.movie.generalInfo.poster?.toImageHolder()
      is EpisodeItem -> item.seasonItem.showItem.generalInfo?.poster?.toImageHolder()
      else -> null
    }
    fun getBackdrop() = when(item) {
      is MovieItem -> item.movie.generalInfo.backdrop?.toImageHolder()
      is EpisodeItem -> item.seasonItem.showItem.generalInfo?.backdrop?.toImageHolder()
      else -> null
    }
  }


  companion object {
    fun Actor.toMediaItem() = ActorItem(this)
    fun Movie.toMediaItem() = MovieItem(this)
    fun Show.toMediaItem() = ShowItem(this)
    fun Episode.toMediaItem(season: SeasonItem) = EpisodeItem(this, season)
    fun StreamData.toMediaItem() = StreamItem(this)
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
      is StreamItem -> streamData.fileName.hashCode()
      is SeasonItem -> getSlug()
      is TrailerItem -> streamData.originalUrl
      is PlaybackProgressItem -> getSlug()
    }

  val title
    get() = when (this) {
      is ActorItem -> actor.name
      is MovieItem -> movie.generalInfo.title
      is ShowItem -> show.generalInfo.title
      is EpisodeItem -> if(episode.generalInfo.title.isEmpty())  "Episode ${episode.episodeNumber}" else episode.generalInfo.title
      is StreamItem -> streamData.fileName
      is SeasonItem -> if(season.generalInfo.title.isEmpty()) "Season ${season.number}" else season.generalInfo.title
      is TrailerItem -> streamData.originalUrl
      is PlaybackProgressItem -> getName()
    }

  val releaseYear
    get() = when (this) {
      is ActorItem -> actor.name
      is MovieItem -> movie.generalInfo.getReleaseYear()
      is ShowItem -> show.generalInfo.getReleaseYear()
      is EpisodeItem -> episode.generalInfo.getReleaseYear()
      is SeasonItem -> season.releaseDateMsUTC?.getYear()
      else  -> null
    }

  val releaseMonthYear
    get() = when (this) {
      is MovieItem -> movie.generalInfo.releaseDateMsUTC?.toLocalMonthYear()
      is ShowItem -> show.generalInfo.releaseDateMsUTC?.toLocalMonthYear()
      is EpisodeItem -> episode.generalInfo.releaseDateMsUTC?.toLocalMonthYear()
      is SeasonItem -> season.releaseDateMsUTC?.toLocalMonthYear()
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
      is StreamItem -> streamData.streamQuality.toImageHolder()
      is SeasonItem -> season.generalInfo.poster?.toImageHolder()
      is PlaybackProgressItem -> getPoster()
      else -> null
    }

  val backdrop
    get() = when (this) {
      is ActorItem -> actor.image
      is MovieItem -> movie.generalInfo.backdrop?.toImageHolder()
      is ShowItem -> show.generalInfo.backdrop?.toImageHolder()
      is EpisodeItem -> episode.generalInfo.backdrop?.toImageHolder() ?: seasonItem.showItem.generalInfo?.backdrop?.toImageHolder()
      is SeasonItem -> season.generalInfo.backdrop?.toImageHolder() ?: showItem.generalInfo?.backdrop?.toImageHolder()
      is PlaybackProgressItem -> getBackdrop()
      else -> null
    }

  val subtitle
    get() = when (this) {
      is ActorItem -> actor.role
      is MovieItem -> movie.generalInfo.genres?.firstOrNull() ?: ""
      is ShowItem -> show.generalInfo.genres?.firstOrNull() ?: ""
      is EpisodeItem -> seasonItem.showItem.title
      is StreamItem -> streamData.providerName
      is PlaybackProgressItem -> when(item)  {
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
      is PlaybackProgressItem -> item.generalInfo?.overview ?: ""
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
    get() = when(this) {
      is MovieItem -> movie.tagline
      is ShowItem -> show.tagLine
      else -> null

    }

}
