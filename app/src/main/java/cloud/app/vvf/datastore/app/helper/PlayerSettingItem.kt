package cloud.app.vvf.datastore.app.helper

import androidx.media3.ui.CaptionStyleCompat
import cloud.app.vvf.features.player.utils.subtitle.SubtitleStyle
import kotlinx.serialization.Serializable

@Serializable
data class PlayerSettingItem(val subtitleStyle: SubtitleStyle) {
}
