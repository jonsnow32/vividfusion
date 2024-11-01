package cloud.app.common.clients.mvdatabase

import cloud.app.common.helpers.PagedData
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Season
import cloud.app.common.models.movie.Show

interface ShowClient {
  suspend fun getSeason(show: Show): List<Season>?
}
