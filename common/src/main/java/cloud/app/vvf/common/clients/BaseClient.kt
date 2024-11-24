package cloud.app.vvf.common.clients

import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.clients.subtitles.SubtitleClient
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting


interface BaseClient {
  val defaultSettings: List<Setting>
  fun init(prefSettings: PrefSettings, httpHelper: HttpHelper)
  suspend fun onExtensionSelected()
}

sealed class Extension<T : BaseClient>(
  val type: ExtensionType,
  open val metadata: ExtensionMetadata,
  open val instance: Lazy<Result<T>>
) {
  val id : String get() = metadata.id
  val name : String get() = metadata.name
}

data class DatabaseExtension(
  override val metadata: ExtensionMetadata,
  override val instance: Lazy<Result<DatabaseClient>>,
) : Extension<DatabaseClient>(ExtensionType.DATABASE, metadata, instance)

data class StreamExtension(
  override val metadata: ExtensionMetadata,
  override val instance: Lazy<Result<StreamClient>>
) : Extension<StreamClient>(ExtensionType.STREAM,metadata,  instance)

data class SubtitleExtension(
  override val metadata: ExtensionMetadata,
  override val instance: Lazy<Result<SubtitleClient>>
) : Extension<SubtitleClient>(ExtensionType.SUBTITLE,metadata,  instance)
