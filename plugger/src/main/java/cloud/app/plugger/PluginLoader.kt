package cloud.app.plugger

interface PluginLoader {
  operator fun <TPlugin> invoke(pluginMetadata: PluginMetadata): TPlugin
}
