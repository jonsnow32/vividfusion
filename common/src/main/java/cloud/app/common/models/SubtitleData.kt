package cloud.app.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubtitleData(
  val name: String,
  val url: String,
  val origin: SubtitleOrigin,
  val mimeType: String,
  val headers: Map<String, String>,
  val languageCode: String?
) : Parcelable {
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
