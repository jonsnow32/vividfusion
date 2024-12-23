package cloud.app.vvf.common.models.movie


import kotlinx.serialization.Serializable

@Serializable
data class Ids(
  var tmdbId: Int? = null,
  var imdbId: String? = null,
  var traktId: Int? = null,
  var tvdbId: Int? = null
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

