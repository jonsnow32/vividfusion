package cloud.app.vvf.common.models.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalVideo(
  @SerialName("id") val id: String,
  @SerialName("title") val title: String,
  @SerialName("poster") val poster: String,
  @SerialName("uri") val uri: String,
  @SerialName("duration") val duration: Long,
  @SerialName("size") val size: Long,
  @SerialName("date_added") val dateAdded: Long,
  @SerialName("album") val album: String,
)
@Serializable
data class  LocalAlbum(
  @SerialName("id") val id: String,
  @SerialName("title") val title: String,
  @SerialName("poster") val poster: String,
  @SerialName("uri") val uri: String,
  @SerialName("duration") val duration: Long,
  @SerialName("local_videos") val videos: List<LocalVideo>
)
