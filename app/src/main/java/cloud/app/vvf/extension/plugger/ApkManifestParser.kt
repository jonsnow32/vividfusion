package cloud.app.vvf.extension.plugger

import cloud.app.vvf.common.helpers.ImportType
import tel.jeelpa.plugger.ManifestParser
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType

class ApkManifestParser(
  private val importType: ImportType
) : ManifestParser<ApkFileInfo, ExtensionMetadata> {
  override fun parseManifest(data: ApkFileInfo): ExtensionMetadata = with(data.appInfo.metaData) {
    fun get(key: String): String = getString(key)
      ?: error("$key not found in Metadata for ${data.appInfo.packageName}")

    val types = getString("types")?.split(",")
      ?.map { type -> ExtensionType.entries.first { it.feature == type } }

    if(types.isNullOrEmpty()) error("types not found in Metadata for ${data.appInfo.packageName}")

    ExtensionMetadata(
      path = data.path,
      className = get("class"),
      importType = importType,
      name = get("name"),
      version = get("version"),
      description = get("description"),
      author = get("author"),
      authorUrl = getString("author_url"),
      iconUrl = getString("icon_url"),
      repoUrl = getString("repo_url"),
      updateUrl = getString("update_url"),
      types = types
    )
  }
}
