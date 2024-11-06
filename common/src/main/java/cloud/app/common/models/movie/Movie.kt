package cloud.app.common.models.movie


import kotlinx.serialization.Serializable

@Serializable
data class Movie(
  val ids: Ids,
  val generalInfo: GeneralInfo,
  var recommendations: List<Movie>? = null,
  var position: Long = 0,
  var updateTime : Long = System.currentTimeMillis(),

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Movie
    return ids == other
  }

  override fun hashCode(): Int {
    var result = ids.hashCode()
    result = 31 * result + generalInfo.hashCode()
    return result
  }

  override fun toString(): String {
    return super.toString()
  }
}
