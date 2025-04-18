package cloud.app.vvf.common.clients

import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting


/**
 * Represents a base client for interacting with an extension.
 */
interface BaseClient {
  val defaultSettings: List<Setting>
  fun init(prefSettings: PrefSettings)
  fun onSettingsChanged(key: String, value: Any)
  suspend fun onExtensionSelected()
}

/**
 * Represents an extension associated with a [BaseClient].
 *
 * Extensions provide additional functionalities or data to the client.
 * They are identified by a unique [id] and have a [name] for display.
 *
 * @param T The type of the client this extension is associated with, must be a subclass of [BaseClient].
 * @param type The [ExtensionType] of this extension.
 * @param metadata The [ExtensionMetadata] containing information about the extension.
 * @param instance A lazy-loaded [Result] containing the actual instance of the extension client.
 */
open class Extension<T : BaseClient>(
  open val metadata: ExtensionMetadata,
  open val instance: Lazy<Result<T>>
) {

  val id : String get() = metadata.className
  val name : String get() = metadata.name
  val icon : String? get() = metadata.iconUrl
  val types: List<ExtensionType> get() = metadata.types

}

