package cloud.app.avp.network.api.openSubtitle.models

data class DownloadRequest(
    val file_id: Int,
    val file_name: String? = null,
    val force_download: Boolean? = null,
    val in_fps: Int? = null,
    val out_fps: Int? = null,
    val sub_format: String? = null,
    val timeshift: Int? = null,
)
