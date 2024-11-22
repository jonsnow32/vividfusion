package cloud.app.vvf.network.api.openSubtitle.models

data class User(
    val allowed_downloads: Int,
    val allowed_translations: Int,
    val ext_installed: Boolean,
    val level: String,
    val user_id: Int,
    val vip: Boolean,
    val remaining_downloads: Int = 0,
    val downloads_count: Int = 0
)
