package cloud.app.avp.network.api.realdebrid.model

data class RealDebridDownloadedObjectItem(
    val chunks: Int,
    val download: String,
    val filename: String,
    val filesize: Long,
    val generated: String,
    val host: String,
    val id: String,
    val link: String,
    val mimeType: String,
    val type: String
)
