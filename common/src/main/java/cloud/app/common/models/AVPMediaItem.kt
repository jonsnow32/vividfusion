package cloud.app.common.models

import android.os.Parcelable
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.movie.Show
import cloud.app.common.models.stream.StreamData
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class AVPMediaItem : Parcelable {
  data class MovieItem(val movie: Movie) : AVPMediaItem()
  data class ShowItem(val show: Show) : AVPMediaItem()
  data class EpisodeItem(val episode: Episode, val show: Show? = null) : AVPMediaItem()
  data class ActorItem(val actorData: ActorData) : AVPMediaItem()
  data class StreamItem(val streamData: StreamData) : AVPMediaItem()

  companion object {
    fun ActorData.toMediaItem() = ActorItem(this)
    fun Movie.toMediaItem() = MovieItem(this)
    fun Show.toMediaItem() = ShowItem(this)
    fun Episode.toMediaItem() = EpisodeItem(this)
    fun StreamData.toMediaItem() = StreamItem(this)

    fun toMediaItemsContainer(
      title: String, subtitle: String? = null, more: PagedData<AVPMediaItem>? = null
    ) = MediaItemsContainer.Category(title, subtitle, more)

  }

  fun toMediaItemsContainer() = MediaItemsContainer.Item(
    when (this) {
      is MovieItem -> movie.toMediaItem()
      is ActorItem -> actorData.toMediaItem()
      is ShowItem -> show.toMediaItem()
      is EpisodeItem -> episode.toMediaItem()
      is StreamItem -> streamData.toMediaItem()
    }
  )

  fun sameAs(other: AVPMediaItem) = when (this) {
    is ActorItem -> other is ActorItem && actorData.actor.name == other.actorData.actor.name
    is MovieItem -> other is MovieItem && movie == other.movie
    is ShowItem -> other is ShowItem && show == other.show
    is EpisodeItem -> other is EpisodeItem && episode == other.episode
    is StreamItem -> other is StreamItem && streamData.hashCode() == other.streamData.hashCode()
  }

  val id
    get() = when (this) {
      is ActorItem -> actorData.actor.name
      is MovieItem -> movie.ids.toString()
      is ShowItem -> show.ids.toString()
      is EpisodeItem -> episode.ids.toString()
      is StreamItem -> streamData.fileName
    }

  val title
    get() = when (this) {
      is ActorItem -> actorData.actor.name
      is MovieItem -> movie.generalInfo.title
      is ShowItem -> show.generalInfo.title
      is EpisodeItem -> episode.generalInfo.title
      is StreamItem -> streamData.fileName
    }

  val releaseYear
    get() = when (this) {
      is ActorItem -> actorData.actor.name
      is MovieItem -> movie.generalInfo.getReleaseYear()
      is ShowItem -> show.generalInfo.getReleaseYear()
      is EpisodeItem -> episode.generalInfo.getReleaseYear()
      is StreamItem -> null
    }
  val poster
    get() = when (this) {
      is ActorItem -> actorData.actor.image
      is MovieItem -> movie.generalInfo.poster?.toImageHolder()
      is ShowItem -> show.generalInfo.poster?.toImageHolder()
      is EpisodeItem -> episode.generalInfo.poster?.toImageHolder()
      is StreamItem -> streamData.streamQuality.toImageHolder()
    }

  val backdrop
    get() = when (this) {
      is ActorItem -> actorData.actor.image
      is MovieItem -> movie.generalInfo.backdrop?.toImageHolder()
      is ShowItem -> show.generalInfo.backdrop?.toImageHolder()
      is EpisodeItem -> episode.generalInfo.backdrop?.toImageHolder()
      is StreamItem -> null
    }
  val subtitle
    get() = when (this) {
      is ActorItem -> actorData.roleString
      is MovieItem -> movie.generalInfo.genres?.firstOrNull() ?: ""
      is ShowItem -> show.generalInfo.genres?.firstOrNull() ?: ""
      is EpisodeItem -> episode.generalInfo.genres?.firstOrNull() ?: ""
      is StreamItem -> streamData.providerName
    }

  val overview
    get() = when (this) {
      is MovieItem -> movie.generalInfo.overview?.firstOrNull() ?: ""
      is ShowItem -> show.generalInfo.overview?.firstOrNull() ?: ""
      is EpisodeItem -> episode.generalInfo.overview?.firstOrNull() ?: ""
      else -> null
    }


  val rating
    get() = when (this) {
      is ActorItem -> null
      is MovieItem -> movie.generalInfo.rating
      is ShowItem -> show.generalInfo.rating
      is EpisodeItem -> episode.generalInfo.rating
      is StreamItem -> 0.0
    }
}
