package cloud.app.vvf.common.models.movie


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Show(
  @SerialName("ids") val ids: Ids,
  @SerialName("general_info") val generalInfo: GeneralInfo,
  @SerialName("recommendations") var recommendations: List<Show>? = null,
  @SerialName("seasons") var seasons: List<Season>? = null,
  @SerialName("update_time") var updateTime: Long = System.currentTimeMillis(),
  @SerialName("tag_line") var tagLine: String? = null,
  @SerialName("status") var status: String? = "continue", // continue, end
  @SerialName("content_rating") var contentRating: String? = null
)
 {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Show
    return ids == other
  }

  override fun hashCode(): Int {
    var result = ids.hashCode()
    result = 31 * result + generalInfo.hashCode()
    return result
  }
}

