package cloud.app.common.clients

import android.os.Parcelable
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.settings.Setting
import cloud.app.common.settings.Settings
import kotlinx.parcelize.Parcelize
import okhttp3.OkHttpClient

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
  fun init(settings: Settings, httpHelper: HttpHelper)
  suspend fun onExtensionSelected()
}
