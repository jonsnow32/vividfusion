package cloud.app.vvf.extension.plugger

import android.content.Context
import android.os.Build
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import dalvik.system.DexClassLoader
import tel.jeelpa.plugger.PluginLoader
import java.io.File
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

class AndroidPluginLoader<TPlugin>(
  private val context: Context
) : PluginLoader<ExtensionMetadata, TPlugin> {

  @Suppress("UNCHECKED_CAST")
  override fun loadPlugin(pluginMetadata: ExtensionMetadata): TPlugin {
    val libFolder = extractLibraries(pluginMetadata)
    val classLoader = createClassLoader(
      pluginMetadata.path,
      pluginMetadata.preservedPackages,
      libFolder.absolutePath
    )
    val clazz = classLoader.loadClass(pluginMetadata.className)
    return clazz.getConstructor().newInstance() as TPlugin
  }

  private fun createClassLoader(
    path: String,
    preservedPackages: List<String>,
    libFolder: String
  ): ClassLoader {
    return PreservedPackageClassLoader(
      preservedPackages = preservedPackages,
      dexPath = path,
      optimizedDirectory = context.cacheDir.absolutePath,
      librarySearchPath = libFolder,
      parent = context.classLoader
    )
  }

  private fun extractLibraries(metadata: ExtensionMetadata): File {
    val libFolder = File(context.cacheDir, "libs/${metadata.className}")
    if (libFolder.exists() && libFolder.listFiles()?.isNotEmpty() == true) {
      return libFolder
    }
    libFolder.mkdirs()
    extractNativeLibs(metadata.path, Build.SUPPORTED_ABIS.first(), libFolder)
    return libFolder
  }

  private fun extractNativeLibs(apkPath: String, targetAbi: String, outputFolder: File) {
    ZipFile(apkPath).use { apkFile ->
      apkFile.entries().asSequence()
        .filter { it.name.startsWith("lib/$targetAbi/") && it.name.endsWith(".so") }
        .forEach { entry ->
          val fileName = entry.name.substringAfterLast("/")
          val outputFile = File(outputFolder, fileName)
          apkFile.getInputStream(entry).use { input ->
            outputFile.outputStream().use { output ->
              input.copyTo(output)
            }
          }
        }
    }
  }

  private class PreservedPackageClassLoader(
    private val preservedPackages: List<String>,
    dexPath: String,
    optimizedDirectory: String?,
    librarySearchPath: String,
    parent: ClassLoader
  ) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent) {

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
      if (name == null) {
        return super.loadClass(name, resolve)
      }
      if (preservedPackages.any { name.startsWith(it) }) {
        classCache[name]?.let { return@let it }
        val clazz = super.loadClass(name, resolve)
        classCache[name] = WeakReference(clazz)
        return clazz
      }
      return super.loadClass(name, resolve)
    }

    companion object {
      private val classCache = mutableMapOf<String, WeakReference<Class<*>>>()
    }
  }


}
