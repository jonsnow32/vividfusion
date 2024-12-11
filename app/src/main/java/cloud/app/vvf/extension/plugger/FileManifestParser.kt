package cloud.app.vvf.extension.plugger

import android.content.pm.PackageManager
import cloud.app.vvf.common.helpers.ImportType
import tel.jeelpa.plugger.ManifestParser
import cloud.app.vvf.common.models.ExtensionMetadata
import com.google.gson.Gson
import java.io.File
import java.io.InputStreamReader
import java.util.jar.JarFile
import java.util.zip.ZipFile

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

  private fun parseJarManifest(zipFilePath: File): ExtensionMetadata {
    val zipFile = ZipFile(zipFilePath)
    zipFile.use { zip ->
      // Look for metadata.json in the ZIP file
      val metadataEntry = zip.getEntry("metadata.json")
      if (metadataEntry != null) {
        // Open the metadata.json file and parse it
        zip.getInputStream(metadataEntry).use { inputStream ->
          val reader = InputStreamReader(inputStream)
          val gson = Gson()
          val vvfInfo = gson.fromJson(reader, VvfFileInfo::class.java)
          if(vvfInfo != null)
            return vvfInfo.toExtensionMetadata(zipFilePath.absolutePath)
        }
      }
    }
    error("metadata.json not found in ZIP file.")
  }

  override fun parseManifest(data: File): ExtensionMetadata {
    if (data.path.endsWith("apk")) {
      return parseApkManifest(data)
    }
    return parseJarManifest(data)
  }
}
