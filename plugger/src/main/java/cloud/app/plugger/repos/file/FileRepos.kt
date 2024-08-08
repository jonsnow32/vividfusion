package cloud.app.plugger.repos.file

import android.content.Context
import cloud.app.plugger.PluginLoader
import cloud.app.plugger.PluginRepo
import cloud.app.plugger.loader.AndroidPluginLoader
import cloud.app.plugger.repos.ManifestParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class FileRepos<TPlugin>(
  private val context: Context,
  private val config: FileConfig,
  private val loader: PluginLoader = AndroidPluginLoader(context),
  private val manifestParser: ManifestParser<String> = FileManifestParser(context)
) : PluginRepo<TPlugin> {

  override fun load(): StateFlow<List<TPlugin>> {
    val list : List<TPlugin> = (File(config.path, "plugins").listFiles() ?: emptyArray<File>())
      .map { it.path }
      .filter { it.endsWith(config.extension) }
      .map { manifestParser.parse(it) }
      .map { loader(it) }

    return MutableStateFlow(list)
  }
}
