package cloud.app.vvf.common.clients

import cloud.app.vvf.common.clients.provider.SettingProvider
import cloud.app.vvf.common.helpers.Injectable
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.common.models.extension.ExtensionMetadata


/**
 * Represents a base client for interacting with an extension.
 */
interface BaseClient : SettingProvider{

  /**
   * Only called when an extension is selected by the user, not when the extension is loaded
   * Use the `onInitialize` for doing stuff to initialize the extension
   *
   * can be called multiple times, if the user re-selects the extension
   */
  suspend fun onExtensionSelected() {}

  /**
   * Called when the extension is loaded, called after all the injections are done.
   * Only called once
   */
  suspend fun onInitialize() {}
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
  open val instance: Injectable<T>
) {

  val id : String get() = metadata.className
  val name : String get() = metadata.name
  val iconUrl : String? get() = metadata.iconUrl
  val iconRes : Int? get() = metadata.iconRes
  val types: List<ExtensionType> get() = metadata.types

}

