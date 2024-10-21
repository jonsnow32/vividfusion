package cloud.app.common.clients.mvdatabase

import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Ids
import cloud.app.common.models.movie.Season

interface ShowClient {
  suspend fun getSeasons(showIds: Ids): List<Season>
  suspend fun getEpisodes(showIds: Ids, seasonNumber: Int? = null): List<Episode>
}
