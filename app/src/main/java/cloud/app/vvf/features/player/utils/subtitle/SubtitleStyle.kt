package cloud.app.vvf.features.player.utils.subtitle

import androidx.annotation.FontRes
import androidx.annotation.OptIn
import androidx.annotation.Px
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val DEF_SUBS_ELEVATION = 20

@Serializable
data class SubtitleStyle @OptIn(UnstableApi::class) constructor(
  @SerialName("foregroundColor") var foregroundColor: Int,
  @SerialName("backgroundColor") var backgroundColor: Int,
  @SerialName("windowColor") var windowColor: Int,
  @SerialName("edgeType") var edgeType: @CaptionStyleCompat.EdgeType Int,
  @SerialName("edgeColor") var edgeColor: Int,
  @FontRes
  @SerialName("typeface") var typeface: Int?,
  @SerialName("typefaceFilePath") var typefaceFilePath: String?,
  /**in dp**/
  @SerialName("elevation") var elevation: Int,
  /**in sp**/
  @SerialName("fixedTextSize") var fixedTextSize: Float?,
  @Px
  @SerialName("edgeSize") var edgeSize: Float? = null,
  @SerialName("removeCaptions") var removeCaptions: Boolean = false,
  @SerialName("removeBloat") var removeBloat: Boolean = true,
  /** Apply caps lock to the text **/
  @SerialName("upperCase") var upperCase: Boolean = false,
)

