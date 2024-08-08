package cloud.app.plugger.repos.installedApk

import android.content.pm.ApplicationInfo
import cloud.app.plugger.PluginMetadata
import cloud.app.plugger.repos.ManifestParser

class InstalledApkManifestParser(private val pluginConfig: InstalledApkConfig) : ManifestParser<ApplicationInfo> {
  override fun parse(data: ApplicationInfo): PluginMetadata {
    return PluginMetadata(
      path = data.sourceDir,
      className = data.metaData?.getString("class")
        ?: error("ClassName not found in Metadata"),
    )
  }
}
