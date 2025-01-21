package cloud.app.vvf.common.models.subtitle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubtitleData(
  @SerialName("name") val name: String,
  @SerialName("url") val url: String,
  @SerialName("origin") val origin: SubtitleOrigin,
  @SerialName("mime_type") val mimeType: String,
  @SerialName("headers") val headers: Map<String, String>,
  @SerialName("language_code") val languageCode: String?
) {
  /** Internal ID for exoplayer, unique for each link*/
  fun getId(): String {
    return if (origin == SubtitleOrigin.EMBEDDED_IN_VIDEO) url
    else "$url|$name"
  }
}

enum class SubtitleOrigin {
  URL,
  DOWNLOADED_FILE,
  EMBEDDED_IN_VIDEO
}
