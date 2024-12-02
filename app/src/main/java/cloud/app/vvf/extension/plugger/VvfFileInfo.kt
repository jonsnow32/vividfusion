package cloud.app.vvf.extension.plugger

import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.models.ExtensionMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VvfFileInfo(
  @SerialName("Extension-Id") val extensionId: String,
  @SerialName("Extension-Type") val extensionType: String,
  @SerialName("Extension-Class") val extensionClass: String,
  @SerialName("Extension-Version-Code") val extensionVersionCode: Int,
  @SerialName("Extension-Version-Name") val extensionVersionName: String,
  @SerialName("Extension-Icon-Url") val extensionIconUrl: String,
  @SerialName("Extension-Name") val extensionName: String,
  @SerialName("Extension-Description") val extensionDescription: String,
  @SerialName("Extension-Author") val extensionAuthor: String,
  @SerialName("Extension-Author-Url") val extensionAuthorUrl: String,
  @SerialName("Extension-Repo-Url") val extensionRepoUrl: String,
  @SerialName("Extension-Update-Url") val extensionUpdateUrl: String
) {
  fun toExtensionMetadata(path: String): ExtensionMetadata {
    return ExtensionMetadata(
      className = extensionClass,
      path = path, // Pass the path as an argument
      importType = ImportType.File,
      id = extensionId,
      name = extensionName,
      version = extensionVersionName, // Use versionName for version
      description = extensionDescription,
      author = extensionAuthor,
      authorUrl = extensionAuthorUrl,
      iconUrl = extensionIconUrl,
      repoUrl = extensionRepoUrl,
      updateUrl = extensionUpdateUrl,
      type = extensionType
    )
  }
}
