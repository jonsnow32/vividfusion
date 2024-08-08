package cloud.app.common.clients

import cloud.app.common.settings.Setting
import cloud.app.common.settings.Settings

interface BaseExtension {
    val settingItems: List<Setting>
    fun setSettings(settings: Settings)
    suspend fun onExtensionSelected()
}
