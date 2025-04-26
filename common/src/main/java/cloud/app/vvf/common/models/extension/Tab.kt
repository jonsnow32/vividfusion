package cloud.app.vvf.common.models.extension

data class Tab (
    val id: String,
    val name: String,
    val resId: Int = -1,
    val extras: Map<String, String> = emptyMap(),
)
