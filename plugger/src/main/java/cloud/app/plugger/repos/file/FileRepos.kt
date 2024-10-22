package cloud.app.plugger.repos.file

import android.content.Context
import cloud.app.plugger.PluginLoader
import cloud.app.plugger.PluginRepo
import cloud.app.plugger.loader.AndroidPluginLoader
import cloud.app.plugger.repos.ManifestParser
import cloud.app.plugger.repos.installedApk.InstalledApkRepos.Companion.PACKAGE_FLAGS
import cloud.app.plugger.utils.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class FileRepos<TPlugin>(
  private val context: Context,
  private val config: FileConfig,
  private val loader: PluginLoader = AndroidPluginLoader(context),
  private val manifestParser: ManifestParser<String> = FileManifestParser(context)
) : PluginRepo<TPlugin> {

  private fun getStaticPlugins(): List<Result<TPlugin>> {
    val list = (File(config.path, "plugins").listFiles() ?: emptyArray<File>())
      .map { it.path }
      .filter { it.endsWith(config.extension) }
      .map {
        runCatching { manifestParser.parse(it) }
      }
      .map { runCatching { loader<TPlugin>(it.getOrThrow()) } }
    return list;
  }

  override fun load(): StateFlow<List<Lazy<Result<TPlugin>>>> =
    MutableStateFlow(getStaticPlugins()).mapState { result: List<Result<TPlugin>> ->
      result.map {
        lazy {
          it
        }
      }
    }

}
