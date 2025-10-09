package cloud.app.vvf.common.clients.provider

import cloud.app.vvf.common.models.extension.Message
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting
import kotlinx.coroutines.flow.MutableSharedFlow

interface SettingProvider {
  fun setSetting(prefSettings: PrefSettings)
  suspend fun getSettingItems() : List<Setting>

  suspend fun onSettingsChanged(key: String, value: Any?)
}
