package cloud.app.common.models

import android.os.Parcelable
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.movie.Show
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class AVPMediaItem : Parcelable {
  data class MovieItem(val movie: Movie) : AVPMediaItem()
  data class ShowItem(val show: Show) : AVPMediaItem()
  data class EpisodeItem(val episode: Episode) : AVPMediaItem()
  data class ActorItem(val actorData: ActorData) : AVPMediaItem()

  companion object {
    fun ActorData.toMediaItem() = ActorItem(this)
    fun Movie.toMediaItem() = MovieItem(this)
    fun Show.toMediaItem() = ShowItem(this)
    fun Episode.toMediaItem() = EpisodeItem(this)

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
    }
  )

  fun sameAs(other: AVPMediaItem) = when (this) {
    is ActorItem -> other is ActorItem && actorData.actor.name == other.actorData.actor.name
    is MovieItem -> other is MovieItem && movie == other.movie
    is ShowItem -> other is ShowItem && show == other.show
    is EpisodeItem -> other is EpisodeItem && episode == other.episode
  }

  val id
    get() = when (this) {
      is ActorItem -> actorData.actor.name
      is MovieItem -> movie.ids.toString()
      is ShowItem -> show.ids.toString()
      is EpisodeItem -> episode.ids.toString()
    }

  val title
    get() = when (this) {
      is ActorItem -> actorData.actor.name
      is MovieItem -> movie.generalInfo.title
      is ShowItem -> show.generalInfo.title
      is EpisodeItem -> episode.generalInfo.title
    }

  val poster
    get() = when (this) {
      is ActorItem -> actorData.actor.image
      is MovieItem -> movie.generalInfo.poster?.toImageHolder()
      is ShowItem -> show.generalInfo.poster?.toImageHolder()
      is EpisodeItem -> episode.generalInfo.poster?.toImageHolder()
    }

  val subtitle
    get() = when(this) {
      is ActorItem -> actorData.roleString
      is MovieItem -> movie.generalInfo.genres.toString()
      is ShowItem -> show.generalInfo.genres.toString()
      is EpisodeItem -> episode.generalInfo.genres.toString()
    }
  val rating
    get() = when(this) {
      is ActorItem -> null
      is MovieItem -> movie.generalInfo.rating
      is ShowItem -> show.generalInfo.rating
      is EpisodeItem -> episode.generalInfo.rating
    }
}
