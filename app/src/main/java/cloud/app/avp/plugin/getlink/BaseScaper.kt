package cloud.app.avp.plugin.getlink

import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.SubtitleData
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.stream.StreamData

abstract class BaseScaper {

  abstract val name: String

  abstract suspend fun getMovieStream(
    movie: Movie,
    subtitleCallback: (SubtitleData) -> Unit,
    linkCallback: (StreamData) -> Unit
  )

  abstract suspend fun getEpisodeStream(
    episode: Episode,
    subtitleCallback: (SubtitleData) -> Unit,
    linkCallback: (StreamData) -> Unit
  )

  suspend operator fun invoke(
    mediaItem: AVPMediaItem,
    subtitleCallback: (SubtitleData) -> Unit,
    linkCallback: (StreamData) -> Unit
  ) {
    return when (mediaItem) {
      is AVPMediaItem.MovieItem -> getMovieStream(mediaItem.movie, subtitleCallback, linkCallback)
      is AVPMediaItem.EpisodeItem -> getEpisodeStream(
        mediaItem.episode,
        subtitleCallback,
        linkCallback
      )
      else -> throw NotImplementedError()
    }
  }

}
