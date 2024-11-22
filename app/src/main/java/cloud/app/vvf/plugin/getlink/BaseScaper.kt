package cloud.app.vvf.plugin.getlink

import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.SubtitleData
import cloud.app.vvf.common.models.movie.Episode
import cloud.app.vvf.common.models.movie.Movie
import cloud.app.vvf.common.models.stream.StreamData

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
