package cloud.app.avp.plugin.tmdb

import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.movie.GeneralInfo
import cloud.app.common.models.movie.Ids
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.movie.Show
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.MovieResultsPage
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage

const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"


fun MovieResultsPage.toMediaItemsList() = results?.map { it.toMediaItem() } ?: emptyList()
fun TvShowResultsPage.toMediaItemsList() = results?.map { it.toMediaItem() } ?: emptyList()

fun BaseMovie.toMediaItem() = AVPMediaItem.MovieItem(
  Movie(
    ids = Ids(tmdbId = this.id),
    generalInfo = GeneralInfo(
      title = this.title ?: "No title",
      originalTitle = original_title ?: "No title",
      overview = overview,
      releaseDateMsUTC = release_date?.time ?: 0,
      poster = poster_path,
      backdrop = backdrop_path,
      rating = vote_average
      //to be continued
    )
  )
)


fun BaseTvShow.toMediaItem() = AVPMediaItem.ShowItem(
  Show(
    ids = Ids(tmdbId = this.id),
    generalInfo = GeneralInfo(
      title = name ?: "No title",
      originalTitle = original_name ?: "No title",
      overview = overview,
      releaseDateMsUTC = first_air_date?.time ?: 0,
      poster = poster_path,
      backdrop = backdrop_path,
      rating = vote_average
      //to be continued
    )
  )
)
