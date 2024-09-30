package cloud.app.plugger.loader

import android.content.Context
import cloud.app.plugger.PluginLoader
import cloud.app.plugger.PluginMetadata
import cloud.app.plugger.utils.getClassLoader
import dalvik.system.DexClassLoader

class AndroidPluginLoader(private val context: Context) : PluginLoader{
  override fun <TPlugin> invoke(pluginMetadata: PluginMetadata): TPlugin {
    return context.getClassLoader(pluginMetadata.path)
      .loadClass(pluginMetadata.className)
      .getConstructor()
      .newInstance() as TPlugin
  }
}
