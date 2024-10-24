package cloud.app.plugger.repos.installedApk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import cloud.app.plugger.PluginLoader
import cloud.app.plugger.PluginRepo
import cloud.app.plugger.loader.AndroidPluginLoader
import cloud.app.plugger.repos.ManifestParser
import cloud.app.plugger.utils.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InstalledApkRepos<TPlugin>(
  private val context: Context,
  private val configuration: InstalledApkConfig,
  private val loader: PluginLoader = AndroidPluginLoader(context),
  private val manifestParser: ManifestParser<ApplicationInfo> = InstalledApkManifestParser(
    configuration
  ),
) : PluginRepo<TPlugin> {


  private val loadedPlugins = MutableStateFlow(getStaticPlugins())
  private val appInstallReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      loadedPlugins.value = getStaticPlugins()
    }
  }

  init {
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_ADDED)
      addAction(Intent.ACTION_PACKAGE_REMOVED)
      addAction(Intent.ACTION_PACKAGE_REPLACED)
      addAction(Intent.ACTION_PACKAGE_CHANGED)
      addDataScheme("package")
    }
    context.registerReceiver(appInstallReceiver, filter)
  }


  private fun getStaticPlugins(): List<Result<TPlugin>> {
    return context.packageManager
      .getInstalledPackages(PACKAGE_FLAGS)
      .filter {
        it.reqFeatures.orEmpty().any { featureInfo ->
          featureInfo.name == configuration.featureName
        }
      }
      .map {
        runCatching { manifestParser.parse(it.applicationInfo) }
      }
      .map {
        runCatching { loader<TPlugin>(it.getOrThrow()) }
      }
      .toList()
  }

  override fun load(): StateFlow<List<Lazy<Result<TPlugin>>>> =
    loadedPlugins.mapState { result: List<Result<TPlugin>> ->
      result.map {
        lazy {
          it
        }
      }
    }


  companion object {

    @Suppress("Deprecation")
    val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
      PackageManager.GET_META_DATA or
      PackageManager.GET_SIGNATURES or
      (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0)
  }

}
