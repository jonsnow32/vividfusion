package cloud.app.common.settings

data class SettingItem(
    override val title: String,
    override val key: String,
    val summary: String? = null,
) : Setting
