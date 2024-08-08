package cloud.app.common.settings

data class SettingSwitch(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val defaultValue: Boolean
) : Setting
