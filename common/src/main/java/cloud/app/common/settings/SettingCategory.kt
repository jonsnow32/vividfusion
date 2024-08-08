package cloud.app.common.settings

data class SettingCategory(
    override val title: String,
    override val key: String,
    val items: List<Setting>
) : Setting
