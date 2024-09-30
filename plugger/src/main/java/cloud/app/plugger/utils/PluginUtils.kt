package cloud.app.plugger.utils

import android.content.Context
import cloud.app.plugger.PluginMetadata
import dalvik.system.DexClassLoader
import kotlinx.serialization.json.Json

fun Context.getClassLoader(path: String): DexClassLoader {
  return DexClassLoader(
    path,
    cacheDir.absolutePath,
    null,
    classLoader
  )
}

inline fun <reified T> String.parsed(): T {
  return Json.decodeFromString<T>(this)
}

inline fun <reified TPlugin> verifyImplementation(classLoader: ClassLoader, pluginMetadata : PluginMetadata): Boolean {
  return try {
    // Load the class
    val loadedClass = classLoader.loadClass(pluginMetadata.className)

    // Get the TPlugin interface
    val pluginInterface = TPlugin::class.java

    // Get all declared methods from the interface
    val pluginMethods: List<String> = pluginInterface.declaredMethods.map { it.name }

    // Get all declared methods from the loaded class
    val implementedMethods: List<String> = loadedClass.declaredMethods.map { it.name }

    // Check if all interface methods are implemented in the loaded class
    pluginMethods.all { it in implementedMethods }
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }
}
