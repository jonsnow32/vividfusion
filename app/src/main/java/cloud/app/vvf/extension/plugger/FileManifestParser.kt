package cloud.app.vvf.extension.plugger

import android.content.pm.PackageManager
import cloud.app.vvf.common.helpers.ImportType
import tel.jeelpa.plugger.ManifestParser
import cloud.app.vvf.common.models.ExtensionMetadata
import java.io.File
import java.util.jar.JarFile

class FileManifestParser(
  private val packageManager: PackageManager,
) : ManifestParser<File, ExtensionMetadata> {

  private fun parseApkManifest(file: File): ExtensionMetadata {
    val info = packageManager
      .getPackageArchiveInfo(file.path, ApkPluginSource.PACKAGE_FLAGS)!!
      .applicationInfo!!

    fun get(key: String): String = info.metaData.getString(key)
      ?: error("$key not found in Metadata for ${file.path}")

    return ExtensionMetadata(
      path = file.path,
      className = get("class"),
      importType = ImportType.File,
      id = get("id"),
      name = get("name"),
      version = get("version"),
      description = get("description"),
      author = get("author"),
      authorUrl = info.metaData.getString("author_url"),
      iconUrl = info.metaData.getString("icon_url"),
      repoUrl = info.metaData.getString("repo_url"),
      updateUrl = info.metaData.getString("update_url"),
      enabled = info.metaData.getBoolean("enabled", true)
    )
  }

  private fun parseJarManifest(data: File): ExtensionMetadata {
    val manifestAttributes = mutableMapOf<String, String>()

    JarFile(data).use { jar ->
      val manifest = jar.manifest
      if (manifest != null) {
        val attributes = manifest.mainAttributes
        attributes.forEach { key, value ->
          manifestAttributes[key.toString()] = value.toString()
        }
      }
    }

    val vvfFileInfo = if (manifestAttributes.isNotEmpty()) {
      VvfFileInfo(
        extensionId = manifestAttributes["Extension-Id"] ?: "",
        extensionType = manifestAttributes["Extension-Type"] ?: "",
        extensionClass = manifestAttributes["Extension-Class"] ?: "",
        extensionVersionCode = manifestAttributes["Extension-Version-Code"]?.toIntOrNull() ?: 0,
        extensionVersionName = manifestAttributes["Extension-Version-Name"] ?: "",
        extensionIconUrl = manifestAttributes["Extension-Icon-Url"] ?: "",
        extensionName = manifestAttributes["Extension-Name"] ?: "",
        extensionDescription = manifestAttributes["Extension-Description"] ?: "",
        extensionAuthor = manifestAttributes["Extension-Author"] ?: "",
        extensionAuthorUrl = manifestAttributes["Extension-Author-Url"] ?: "",
        extensionRepoUrl = manifestAttributes["Extension-Repo-Url"] ?: "",
        extensionUpdateUrl = manifestAttributes["Extension-Update-Url"] ?: ""
      )
    } else {
      error("not found in Metadata for ${data.absolutePath}")
    }
    return vvfFileInfo.toExtensionMetadata(data.absolutePath)
  }

  override fun parseManifest(data: File): ExtensionMetadata {
    if (data.path.endsWith("apk")) {
      return parseApkManifest(data)
    }
    return parseJarManifest(data)
  }
}
