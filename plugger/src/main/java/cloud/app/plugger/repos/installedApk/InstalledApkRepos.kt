package cloud.app.plugger.repos.installedApk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import cloud.app.plugger.PluginLoader
import cloud.app.plugger.PluginRepo
import cloud.app.plugger.loader.AndroidPluginLoader
import cloud.app.plugger.repos.ManifestParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InstalledApkRepos<TPlugin>(
  private val context: Context,
  private val configuration: InstalledApkConfig,
  private val loader: PluginLoader = AndroidPluginLoader(context),
  private val manifestParser: ManifestParser<ApplicationInfo> = InstalledApkManifestParser(configuration),
) : PluginRepo<TPlugin> {

  companion object {

    @Suppress("Deprecation")
    val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
      PackageManager.GET_META_DATA or
      PackageManager.GET_SIGNATURES or
      (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0)

  }

  private fun getStaticPlugins(): List<TPlugin> {
    return context.packageManager
      .getInstalledPackages(PACKAGE_FLAGS)
      .filter {
        it.reqFeatures.orEmpty().any { featureInfo ->
          featureInfo.name == configuration.featureName
        }
      }
      .map { manifestParser.parse(it.applicationInfo)  }
      .map { loader<TPlugin>(it) }
      .toList()
  }

  // TODO: Listen for app installation broadcasts and update flow on change
  override fun load(): StateFlow<List<TPlugin>> =
    MutableStateFlow(getStaticPlugins())
}
