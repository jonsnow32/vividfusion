package cloud.app.vvf.extension.plugger

import android.content.Context
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import tel.jeelpa.plugger.PluginLoader
import tel.jeelpa.plugger.pluginloader.GetClassLoaderWithPathUseCase

class AndroidPluginLoader<TPlugin>(
  private val getClassLoader: GetClassLoaderWithPathUseCase,
) : PluginLoader<ExtensionMetadata, TPlugin> {
  constructor(context: Context) : this(GetClassLoaderWithPathUseCase(context.classLoader))

  @Suppress("UNCHECKED_CAST")
  override fun loadPlugin(pluginMetadata: ExtensionMetadata): TPlugin {
    return getClassLoader.getWithPath(pluginMetadata.path)
      .loadClass(pluginMetadata.className)
      .getConstructor()
      .newInstance() as TPlugin
  }
}
