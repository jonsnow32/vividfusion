package cloud.app.common.models.movie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ids(
  val tmdbId: Int? = null,
  val imdbId: String? = null,
  val traktId: Int? = null,
  val tvdbId: Int? = null
) : Parcelable {
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

