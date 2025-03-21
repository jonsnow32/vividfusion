package cloud.app.vvf.common.models.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Episode(
  @SerialName("ids") val ids: Ids,
  @SerialName("general_info") val generalInfo: GeneralInfo,
  @SerialName("season_number") var seasonNumber: Int,
  @SerialName("episode_number") var episodeNumber: Int,
  @SerialName("show_ids") var showIds: Ids,
  @SerialName("show_origin_title") var showOriginTitle: String,
  @SerialName("update_time") var updateTime: Long = System.currentTimeMillis()
)
