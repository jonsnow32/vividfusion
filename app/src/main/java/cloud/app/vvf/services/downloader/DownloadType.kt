package cloud.app.vvf.services.downloader

enum class DownloadType {
    HTTP,
    HLS,
    TORRENT,
    MAGNET;

    companion object {
        fun fromUrl(url: String): DownloadType {
            return when {
                url.startsWith("magnet:") -> MAGNET
                url.contains(".m3u8") || url.contains("hls") -> HLS
                url.endsWith(".torrent") -> TORRENT
                else -> HTTP
            }
        }
    }
}

data class DownloadRequest(
    val id: String,
    val url: String,
    val fileName: String,
    val type: DownloadType,
    val quality: String = "default",
    val magnetLink: String? = null,
    val headers: Map<String, String> = emptyMap()
)
