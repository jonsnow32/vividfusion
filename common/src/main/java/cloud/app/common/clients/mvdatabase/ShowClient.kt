package cloud.app.common.clients.mvdatabase

import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Season
import cloud.app.common.models.movie.Show

interface ShowClient {
  suspend fun getEpisodes(show: Show): List<Season>
}
