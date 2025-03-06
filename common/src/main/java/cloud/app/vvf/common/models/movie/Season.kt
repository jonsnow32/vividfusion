package cloud.app.vvf.common.models.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Season(
  @SerialName("number") val number: Int,
  @SerialName("general_info") val generalInfo: GeneralInfo,
  @SerialName("episode_count") val episodeCount: Int = 0,
  @SerialName("episodes") val episodes: List<Episode>? = null,
  @SerialName("show_ids") var showIds: Ids,
  @SerialName("show_origin_title") var showOriginTitle: String?,
  @SerialName("release_date_ms_utc") var releaseDateMsUTC: Long?
)
