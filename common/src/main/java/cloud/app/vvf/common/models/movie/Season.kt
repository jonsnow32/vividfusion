package cloud.app.vvf.common.models.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Season(
  @SerialName("title") val title: String?,
  @SerialName("number") val number: Int,
  @SerialName("overview") val overview: String?,
  @SerialName("episode_count") val episodeCount: Int = 0,
  @SerialName("poster_path") val posterPath: String?,
  @SerialName("backdrop") val backdrop: String?,
  @SerialName("episodes") val episodes: List<Episode>? = null,
  @SerialName("show_ids") var showIds: Ids,
  @SerialName("show_origin_title") var showOriginTitle: String?,
  @SerialName("release_date_ms_utc") var releaseDateMsUTC: Long?
)
