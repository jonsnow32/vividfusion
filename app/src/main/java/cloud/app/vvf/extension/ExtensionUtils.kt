package cloud.app.vvf.extension

import cloud.app.vvf.BuildConfig
import cloud.app.vvf.ui.exception.AppException.Companion.toAppException
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.extension.ExtensionType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow


/**
 * Checks if the client of this extension is of the specified type.
 */
inline fun <reified T> Extension<*>.isClient(): Boolean = instance.value.getOrNull() is T

/**
 * Finds an extension by its ID in the stored list.
 */
fun StateFlow<List<Extension<*>>?>.getExtension(id: String?): Extension<*>? =
  value?.find { it.id == id }

/**
 * Filters extensions by type in the stored list.
 */
fun StateFlow<List<Extension<*>>?>.getExtensions(type: ExtensionType): List<Extension<*>>? =
  value?.filter { it.metadata.types.contains(type) }


suspend inline fun <reified T : BaseClient, R> Extension<*>.run(
  throwableFlow: MutableSharedFlow<Throwable>,
  noinline block: suspend T.() -> R
): R? {
  return try {
    val client = instance.value.getOrThrow() as T
    block(client)
  } catch (e: Throwable) {
    throwableFlow.emit(e.toAppException(this))
    if (BuildConfig.DEBUG) e.printStackTrace()
    null
  }
}

suspend inline fun <reified T : BaseClient, R> List<Extension<*>>.runClient(
  extensionId: String,
  throwableFlow: MutableSharedFlow<Throwable>,
  noinline block: suspend T.() -> R
): R? {
  val extension = find { it.id == extensionId } ?: return null
  return extension.run(throwableFlow, block)
}
