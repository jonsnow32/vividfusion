package cloud.app.vvf.common.helpers

data class Page<T : Any, P>(
    val data: List<T>,
    val continuation: P?
)
