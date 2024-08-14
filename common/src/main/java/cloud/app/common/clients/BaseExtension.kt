package cloud.app.common.clients

import android.os.Parcelable
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.settings.Setting
import cloud.app.common.settings.Settings
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExtensionMetadata(
  val name: String,
  val type: ExtensionType,
  val description: String,
  val author: String,
  val version: String,
  val icon: String,
  val loginType: LoginType = LoginType.NONE
) : Parcelable

interface BaseExtension {
  val defaultSettings: List<Setting>
  val metadata: ExtensionMetadata
  fun setSettings(settings: Settings)
  suspend fun onExtensionSelected()
}
