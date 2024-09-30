package cloud.app.avp.plugin.tmdb

import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.Actor
import cloud.app.common.models.ActorData
import cloud.app.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.movie.GeneralInfo
import cloud.app.common.models.movie.Ids
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.movie.Show
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.BasePerson
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.tmdb2.entities.MovieResultsPage
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage

const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"


fun MovieResultsPage.toMediaItemsList() = results?.map { it.toMediaItem() } ?: emptyList()
fun TvShowResultsPage.toMediaItemsList() = results?.map { it.toMediaItem() } ?: emptyList()

fun BaseMovie.toMediaItem(): AVPMediaItem.MovieItem {
  fun toMovie(baseMovie: BaseMovie) = Movie(
    ids = Ids(tmdbId = baseMovie.id),
    generalInfo = GeneralInfo(
      title = baseMovie.title ?: "No title",
      originalTitle = baseMovie.original_title ?: "No title",
      overview = baseMovie.overview,
      releaseDateMsUTC = baseMovie.release_date?.time ?: 0,
      poster = baseMovie.poster_path,
      backdrop = baseMovie.backdrop_path,
      rating = baseMovie.vote_average,
      genres = baseMovie.genre_ids?.map { movieGenres[it] ?: "" },
      //to be continued
    )
  )
  val movie = toMovie(this);

  if (this is com.uwetrottmann.tmdb2.entities.Movie) {
    movie.recommendations = this.recommendations?.results?.map { toMovie(it) }
    movie.generalInfo.runtime = this.runtime
    movie.generalInfo.actors = this.credits?.cast?.map { ActorData(
      actor = Actor(name = it.name ?: "No name", image = it.profile_path?.toImageHolder()),
    ) }
  }

  return AVPMediaItem.MovieItem(movie)
}
fun BaseTvShow.toMediaItem() : AVPMediaItem.ShowItem {
  fun toShow(baseShow: BaseTvShow) = Show(
    ids = Ids(tmdbId = baseShow.id),
    generalInfo = GeneralInfo(
      title = baseShow.name ?: "No title",
      originalTitle = baseShow.original_name ?: "No title",
      overview = baseShow.overview,
      releaseDateMsUTC = baseShow.first_air_date?.time ?: 0,
      poster = baseShow.poster_path,
      backdrop = baseShow.backdrop_path,
      rating = baseShow.vote_average,
      genres = baseShow.genre_ids?.map { showGenres[it] ?: "" },
      //to be continued
    )
  )
  val show = toShow(this)
  if (this is com.uwetrottmann.tmdb2.entities.TvShow) {
    show.recommendations = this.recommendations?.results?.map { toShow(it) }
    show.generalInfo.contentRating = this.content_ratings?.results.orEmpty().firstOrNull()?.rating
  }
  return AVPMediaItem.ShowItem(show)
}

fun BasePerson.toMediaItem() = AVPMediaItem.ActorItem(
  ActorData(
    actor = Actor(name = name, image = profile_path?.toImageHolder()),
  )
)

val showGenres: Map<Int, String> = mapOf(
  10759 to "Action & Adventure",
  16 to "Animation",
  35 to "Comedy",
  80 to "Crime",
  99 to "Documentary",
  18 to "Drama",
  10751 to "Family",
  10762 to "Kids",
  9648 to "Mystery",
  10763 to "News",
  10764 to "Reality",
  10765 to "Sci-Fi & Fantasy",
  10766 to "Soap",
  10767 to "Talk",
  10768 to "War & Politics",
  37 to "Western"
)

val movieGenres: Map<Int, String> = mapOf(
  28 to "Action",
  12 to "Adventure",
  16 to "Animation",
  35 to "Comedy",
  80 to "Crime",
  99 to "Documentary",
  18 to "Drama",
  10751 to "Family",
  14 to "Fantasy",
  36 to "History",
  27 to "Horror",
  10402 to "Music",
  9648 to "Mystery",
  10749 to "Romance",
  878 to "Science Fiction",
  10770 to "TV Movie",
  53 to "Thriller",
  10752 to "War",
  37 to "Western"
)
