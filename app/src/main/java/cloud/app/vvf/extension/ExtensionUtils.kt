package cloud.app.vvf.extension

import cloud.app.vvf.ui.exception.AppException.Companion.toAppException
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.ExtensionType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow


suspend fun <T : BaseClient, R> Extension<T>.run(
  throwableFlow: MutableSharedFlow<Throwable>,
  block: suspend T.() -> R
): R? = runCatching {
  block(instance.value.getOrThrow())
}.getOrElse {
  throwableFlow.emit(it.toAppException(this))
  it.printStackTrace()
  null
}

suspend inline fun <reified C, R> Extension<*>.get(
  throwableFlow: MutableSharedFlow<Throwable>,
  block: C.() -> R
): R? = runCatching {
  val client = instance.value.getOrThrow() as? C ?: return@runCatching null
  block(client)
}.getOrElse {
  throwableFlow.emit(it.toAppException(this))
  it.printStackTrace()
  null
}

inline fun <reified T> Extension<*>.isClient() = instance.value.getOrNull() is T

fun StateFlow<List<Extension<*>>?>.getExtension(id: String?) =
  value?.find { it.metadata.id == id }

fun StateFlow<List<Extension<*>>?>.getExtensions(type: ExtensionType) =
  value?.filter { it.metadata.types?.contains(type) == true}
