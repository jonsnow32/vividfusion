package cloud.app.avp.network.api.alldebrid.model

data class ADInstanceTorrent(
    val `data`: DataX,
    val status: String
)

data class DataX(
    val magnets: List<Magnet>
)

data class Magnet(
    val files: List<File>,
    val hash: String,
    val instant: Boolean,
    val magnet: String
)

data class File(
    val e: List<File>?,
    val n: String,
    val s: Long?
)
