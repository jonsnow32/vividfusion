package cloud.app.vvf.extension.plugger

import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VvfFileInfo(
  val url: String,
  val types: List<String>?,
  val className: String,
  val version: String,
  val iconUrl: String?,
  val name: String,
  val description: String?,
  val author: List<String>,
  val repoUrl: String?,
  val fileSize: Long?,
  val status: Int
) {
  fun toExtensionMetadata(path: String): ExtensionMetadata {
    return ExtensionMetadata(
      className = className,
      path = path, // Pass the path as an argument
      importType = ImportType.File,
      id = className,
      name = name,
      version = version, // Use versionName for version
      description = description ?: "",
      author = author.toString(),
      authorUrl = repoUrl,
      iconUrl = iconUrl,
      repoUrl = repoUrl,
      updateUrl = repoUrl,
      types = types?.map { type -> ExtensionType.entries.first { it.feature == type } }
    )
  }
}
