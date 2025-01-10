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
    }

  val title
    get() = when (this) {
      is ActorItem -> actor.name
      is MovieItem -> movie.generalInfo.title
      is ShowItem -> show.generalInfo.title
      is EpisodeItem -> episode.generalInfo.title
      is StreamItem -> streamData.fileName
      is SeasonItem -> season.title ?: "S${season.number}"
    }

  val releaseYear
    get() = when (this) {
      is ActorItem -> actor.name
      is MovieItem -> movie.generalInfo.getReleaseYear()
      is ShowItem -> show.generalInfo.getReleaseYear()
      is EpisodeItem -> episode.generalInfo.getReleaseYear()
      is SeasonItem -> season.releaseDateMsUTC?.getYear()
      is StreamItem -> null
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
      is EpisodeItem -> episode.generalInfo.poster?.toImageHolder()
      is StreamItem -> streamData.streamQuality.toImageHolder()
      is SeasonItem -> season.posterPath?.toImageHolder()
    }

  val backdrop
    get() = when (this) {
      is ActorItem -> actor.image
      is MovieItem -> movie.generalInfo.backdrop?.toImageHolder()
      is ShowItem -> show.generalInfo.backdrop?.toImageHolder()
      is EpisodeItem -> episode.generalInfo.backdrop?.toImageHolder()
      is SeasonItem -> season.posterPath?.toImageHolder()
      else -> null
    }

  val subtitle
    get() = when (this) {
      is ActorItem -> actor.role
      is MovieItem -> movie.generalInfo.genres?.firstOrNull() ?: ""
      is ShowItem -> show.generalInfo.genres?.firstOrNull() ?: ""
      is EpisodeItem -> episode.generalInfo.genres?.firstOrNull() ?: ""
      is StreamItem -> streamData.providerName
      else -> null
    }

  val overview
    get() = when (this) {
      is MovieItem -> movie.generalInfo.overview ?: ""
      is ShowItem -> show.generalInfo.overview ?: ""
      is EpisodeItem -> episode.generalInfo.overview ?: ""
      is SeasonItem -> season.overview ?: ""
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
      is StreamItem -> null
    }
}
