package cloud.app.vvf.common.models.video

import cloud.app.vvf.common.models.video.Video.LocalVideo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoCollection(
  @SerialName("id") val id: String,
  @SerialName("title") val title: String,
  @SerialName("poster") val poster: String,
  @SerialName("uri") val uri: String,
  @SerialName("duration") val duration: Long,
  @SerialName("local_videos") val videos: List<LocalVideo>
)
