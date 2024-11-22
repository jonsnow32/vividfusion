package cloud.app.vvf.common.settings

data class SettingItem(
    override val title: String,
    override val key: String,
    val summary: String? = null,
) : Setting
