package cloud.app.vvf.common.models.movie


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ids(
  @SerialName("tmdb_id") var tmdbId: Int? = null,
  @SerialName("imdb_id") var imdbId: String? = null,
  @SerialName("trakt_id") var traktId: Int? = null,
  @SerialName("tvdb_id") var tvdbId: Int? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Ids

    if (tmdbId != other.tmdbId) return false
    if (imdbId != other.imdbId) return false
    if (traktId != other.traktId) return false
    if (tvdbId != other.tvdbId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = tmdbId ?: 0
    result = 31 * result + (imdbId?.hashCode() ?: 0)
    result = 31 * result + (traktId ?: 0)
    result = 31 * result + (tvdbId ?: 0)
    return result
  }
}

