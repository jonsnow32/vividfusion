package cloud.app.avp.plugin.getlink

import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.stream.StreamData
import kotlinx.coroutines.channels.ProducerScope

abstract class BaseScaper {
    abstract val name: String
    abstract suspend fun getMovieStream(movie: Movie, producerScope: ProducerScope<List<StreamData>>)

    abstract suspend fun getEpisodeStream(episode: Episode, producerScope: ProducerScope<List<StreamData>>)

    suspend operator fun invoke(mediaItem: AVPMediaItem, producerScope: ProducerScope<List<StreamData>>) {
        return when (mediaItem) {
            is AVPMediaItem.MovieItem -> getMovieStream(mediaItem.movie, producerScope)
            is AVPMediaItem.EpisodeItem -> getEpisodeStream(mediaItem.episode, producerScope)
            else -> throw NotImplementedError()
        }
    }

}
