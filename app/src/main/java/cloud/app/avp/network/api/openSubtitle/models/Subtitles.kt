package cloud.app.avp.network.api.openSubtitle.models

data class Subtitles(
    val `data`: List<Data>,
    val page: Int,
    val per_page: Int,
    val total_count: Int,
    val total_pages: Int
)
