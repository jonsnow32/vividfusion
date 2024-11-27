package cloud.app.vvf.common.clients

import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.clients.subtitles.SubtitleClient
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting


/**
 * Represents a base client for interacting with an extension.
 */
interface BaseClient {
  val defaultSettings: List<Setting>
  fun init(prefSettings: PrefSettings, httpHelper: HttpHelper)
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
sealed class Extension<T : BaseClient>(
  val type: ExtensionType,
  open val metadata: ExtensionMetadata,
  open val instance: Lazy<Result<T>>
) {
  val id : String get() = metadata.id
  val name : String get() = metadata.name
}

/**
 * Represents a database extension in the application.
 *
 * Database extensions provide access to a database client, allowing interactions with a database.
 *
 * @property metadata Metadata associated with the extension, such as name, version, and description.
 * @property instance A lazy-initialized instance of the database client. The instance is wrapped in a `Result`
 * to indicate success or failure during initialization.
 */
data class DatabaseExtension(
  override val metadata: ExtensionMetadata,
  override val instance: Lazy<Result<DatabaseClient>>,
) : Extension<DatabaseClient>(ExtensionType.DATABASE, metadata, instance)

/**
 * Represents the Stream extension in the extensions registry.
 *
 * This extension provides access to the Stream Client, which allows interaction with the Stream API.
 *
 * @property metadata The metadata associated with the extension, containing information like name and version.
 * @property instance A lazy instance of the StreamClient, which is initialized when first accessed.
 *                  The instance can either be a successful `StreamClient` or a `Result.failure` if initialization failed.
 */
data class StreamExtension(
  override val metadata: ExtensionMetadata,
  override val instance: Lazy<Result<StreamClient>>
) : Extension<StreamClient>(ExtensionType.STREAM,metadata,  instance)

/**
 * Represents a subtitle extension in the application.
 *
 * Subtitle extensions provide functionality related to subtitles, such as fetching, displaying, and manipulating them.
 *
 * @property metadata Metadata associated with the extension, such as name, version, and author.
 * @property instance A lazy instance of the [SubtitleClient], which provides the actual subtitle functionality.
 *                     This is wrapped in a [Result] to handle potential errors during initialization.
 */
data class SubtitleExtension(
  override val metadata: ExtensionMetadata,
  override val instance: Lazy<Result<SubtitleClient>>
) : Extension<SubtitleClient>(ExtensionType.SUBTITLE,metadata,  instance)
