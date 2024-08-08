package cloud.app.avp.network.api.openSubtitle.models

data class DownloadErrorBody(
    val message: String,
    val remaining: Int,
    val requests: Int,
    val reset_time: String,
    val reset_time_utc: String
)
