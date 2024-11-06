package cloud.app.avp.plugin.tmdb

import android.annotation.SuppressLint
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.Actor
import cloud.app.common.models.ActorData
import cloud.app.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.movie.GeneralInfo
import cloud.app.common.models.movie.Ids
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.movie.Season
import cloud.app.common.models.movie.Show
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.BasePerson
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.tmdb2.entities.MovieResultsPage
import com.uwetrottmann.tmdb2.entities.PersonResultsPage
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import kotlin.collections.mapNotNull
import kotlin.text.*

const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"


fun MovieResultsPage.toMediaItemsList() = results?.map { it.toMediaItem() } ?: emptyList()
fun TvShowResultsPage.toMediaItemsList() = results?.map { it.toMediaItem() } ?: emptyList()
fun PersonResultsPage.toMediaItemsList() = results?.mapNotNull { it.toMediaItem() } ?: emptyList()

fun BaseMovie.toMediaItem(): AVPMediaItem.MovieItem {

  fun getGenre(baseMovie: BaseMovie): List<String>? {
    if (baseMovie.genres.isNullOrEmpty()) {
      return baseMovie.genre_ids?.map { movieGenres[it] ?: "" }
    }
    return baseMovie.genres.map { it.name }
  }

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
      genres = getGenre(this)
      //to be continued
    )
  )

  val movie = toMovie(this);

  if (this is com.uwetrottmann.tmdb2.entities.Movie) {
    movie.ids.imdbId = this.external_ids?.imdb_id

    movie.recommendations = this.recommendations?.results?.map { toMovie(it) }
    movie.generalInfo.runtime = this.runtime
    movie.generalInfo.actors = this.credits?.cast?.map {
      ActorData(
        actor = Actor(name = it.name ?: "No name", image = it.profile_path?.toImageHolder()),
      )
    }
  }

  return AVPMediaItem.MovieItem(movie)
}

fun BaseTvShow.toMediaItem(): AVPMediaItem.ShowItem {


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
    ),
  )

  val show = toShow(this)
  if (this is com.uwetrottmann.tmdb2.entities.TvShow) {
    show.ids.tvdbId = this.external_ids?.tvdb_id
    show.ids.imdbId = this.external_ids?.imdb_id
    show.recommendations = this.recommendations?.results?.map { toShow(it) }
    show.generalInfo.contentRating = this.content_ratings?.results.orEmpty().firstOrNull()?.rating
    show.generalInfo.genres = this.genres?.map { it.name ?: "unknown" }
    show.status = this.status?.toString() ?: "";
    show.generalInfo.actors = this.credits?.cast?.map {
      ActorData(
        actor = Actor(name = it.name ?: "No name", image = it.profile_path?.toImageHolder()),
      )
    }
    show.seasons = this.seasons?.map { tvSeason ->
      Season(
        title = tvSeason.name,
        number = tvSeason.season_number,
        overview = tvSeason.overview,
        episodeCount = tvSeason.episode_count,
        posterPath = tvSeason.poster_path,
        showIds = show.ids,
        showOriginTitle = show.generalInfo.originalTitle,
        backdrop = show.generalInfo.backdrop,
        releaseDateMsUTC = tvSeason.air_date?.time
      )
    }
    show.tagLine = this.tagline
  }
  return AVPMediaItem.ShowItem(show)
}

fun BasePerson.toMediaItem() = AVPMediaItem.ActorItem(
  ActorData(
    actor = Actor(name = name, image = profile_path?.toImageHolder()),
  )
)

@SuppressLint("DefaultLocale")
fun formatSeasonEpisode(season: Int, episode: Int): String {
  return String.format("S%02dE%02d", season, episode)
}

fun String.iso8601ToMillis(): Long {
  if (this.isBlank()) return 0L
  // Parse the date string using LocalDate
  val localDate = LocalDate.parse(this, DateTimeFormatter.ISO_DATE)

  // Convert the LocalDate to milliseconds since epoch (UTC)
  return localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}


val networks: Map<Int, String> = linkedMapOf( // Use LinkedHashMap to preserve order
  213 to "Netflix", // Most popular first
  1024 to "Amazon",
  4 to "CBS",
  19 to "FOX",
  54 to "NBC",
  2 to "ABC",
  34 to "HBO",
  6 to "The CW",
  30 to "FX",
  1313 to "Showtime" // Least popular last
)

val companies: Map<Int, String> = linkedMapOf( // Use LinkedHashMap to preserve order
  420 to "Marvel Studios", // Most popular first
  174 to "Warner Bros. Pictures",
  33 to "Universal Pictures",
  25 to "20th Century Fox",
  5 to "Walt Disney Pictures",
  2 to "Columbia Pictures",
  4 to "Paramount",
  12 to "New Line Cinema",
  34 to "Legendary Pictures",
  6194 to "Lucasfilm" // Least popular last
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
