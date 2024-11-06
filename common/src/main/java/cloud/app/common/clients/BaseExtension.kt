package cloud.app.common.clients

import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.settings.PrefSettings
import cloud.app.common.settings.Setting
import kotlinx.serialization.Serializable


@Serializable
data class ExtensionMetadata(
  val name: String,
  val type: ExtensionType,
  val description: String,
  val author: String,
  val version: String,
  val icon: String,
  val loginType: LoginType = LoginType.NONE
)

interface BaseExtension {
  val defaultSettings: List<Setting>
  val metadata: ExtensionMetadata
  fun init(prefSettings: PrefSettings, httpHelper: HttpHelper)
  suspend fun onExtensionSelected()
}
