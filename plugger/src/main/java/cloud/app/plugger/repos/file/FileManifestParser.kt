package cloud.app.plugger.repos.file

import android.content.Context
import cloud.app.plugger.PluginMetadata
import cloud.app.plugger.repos.ManifestParser
import cloud.app.plugger.utils.getClassLoader
import cloud.app.plugger.utils.parsed
import kotlinx.serialization.Serializable

class FileManifestParser(private val context: Context) : ManifestParser<String> {
  @Serializable
  data class FilePluginManifest(
    val className: String,
  )

  override fun parse(data: String): PluginMetadata {
    val manifestData = context.getClassLoader(data)
      .getResourceAsStream("manifest.json")
      .readBytes()
      .toString(Charsets.UTF_8)
      .parsed<FilePluginManifest>()

    return PluginMetadata(manifestData.className, data)
  }
}
