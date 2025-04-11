package cloud.app.vvf.extension.plugger

import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.common.models.extension.ExtensionType
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
  val status: Int,
) {
  fun toExtensionMetadata(path: String): ExtensionMetadata {
    val types = types?.map { type -> ExtensionType.entries.first { it.feature == type } }
    if (types.isNullOrEmpty()) error("types not found in Metadata for vvf file ${path}")

    return ExtensionMetadata(
      className = className,
      path = path, // Pass the path as an argument
      importType = ImportType.File,
      name = name,
      version = version, // Use versionName for version
      description = description ?: "",
      author = author.toString(),
      authorUrl = repoUrl,
      iconUrl = iconUrl,
      repoUrl = repoUrl,
      updateUrl = repoUrl,
      types = types
    )
  }
}
