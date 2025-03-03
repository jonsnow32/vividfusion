package cloud.app.vvf.common.models.movie


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Movie(
  @SerialName("ids") val ids: Ids,
  @SerialName("general_info") val generalInfo: GeneralInfo,
  @SerialName("tagline") var tagline: String? = null,
  @SerialName("recommendations") var recommendations: List<Movie>? = null,
  @SerialName("position") var position: Long = 0,
  @SerialName("update_time") var updateTime: Long = System.currentTimeMillis(),
  @SerialName("status") var status: String? = null

)
{
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
