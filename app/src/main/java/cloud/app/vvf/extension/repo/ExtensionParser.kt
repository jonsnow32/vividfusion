package cloud.app.vvf.extension.repo

import android.content.Context
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import android.os.Build
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.extension.exception.ExtensionLoadingException
import cloud.app.vvf.utils.ShaUtils.getSha256
import java.io.File
import java.lang.reflect.Array.getInt
import java.util.WeakHashMap

class ExtensionParser(
  private val context: Context,
) {

  fun getAllDynamically(
    type: ImportType,
    map: WeakHashMap<String, Pair<String, Result<Pair<ExtensionMetadata, Lazy<BaseClient>>>>>,
    files: List<File>
  ): List<Result<Pair<ExtensionMetadata, Lazy<BaseClient>>>> {
    val new = files.associate {
      val sha = runCatching { getSha256(it) }.getOrNull().orEmpty()
      val entry = map[it.absolutePath]
      val value = if (entry != null && entry.first == sha) entry
      else sha to parse(it, type)
      it.absolutePath to value
    }
    map.clear()
    map.putAll(new)
    return map.values.map { it.second }
  }

  private fun parse(source: File, importType: ImportType) = runCatching {
    runCatching {
      val metadata = parseManifest(source, importType)
      val lazy = lazy { loadFrom(metadata) }
      metadata to lazy
    }.getOrElse {
      throw ExtensionLoadingException(null, it)
    }
  }


  fun parseManifest(file: File, importType: ImportType): ExtensionMetadata {
    val packageInfo =
      context.packageManager.getPackageArchiveInfo(file.absolutePath, PACKAGE_FLAGS)
        ?: error("Failed to get package info for ${file.absolutePath}")
    val metadata = packageInfo.applicationInfo!!.metaData!!
    val types = packageInfo.reqFeatures!!.toExtensionType()
    fun getOrNull(key: String) = metadata.getString(key)?.takeIf { it.isNotBlank() }
    fun get(key: String) = getOrNull(key)
      ?: error("$key not found in Metadata for ${packageInfo.packageName}")
    return ExtensionMetadata(
      path = file.absolutePath,
      preservedPackages = getOrNull("preserved_packages")
        .orEmpty().split(",").mapNotNull { it.trim().ifEmpty { null } },
      className = get("class"),
      importType = importType,
      types = types,
      version = get("version"),
      iconUrl = getOrNull("icon_url"),
      iconRes = getInt("icon_res", 0).takeIf { it > 0 },
      name = get("name"),
      description = get("description"),
      author = get("author"),
      authorUrl = getOrNull("author_url"),
      repoUrl = getOrNull("repo_url"),
      updateUrl = getOrNull("update_url"),
      enabled = metadata.getBoolean("enabled", true)
    )
  }

  private fun loadFrom(metadata: ExtensionMetadata): BaseClient {
    val dexLoader = DexLoader(metadata, context)
    val clazz = dexLoader.loadClass(metadata.className)
    return clazz.getConstructor().newInstance() as BaseClient
  }

  companion object {

    @Suppress("Deprecation")
    val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
      PackageManager.GET_META_DATA or
      PackageManager.GET_SIGNATURES or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0

    const val FEATURE = "cloud.app.vvf."
    private fun Array<FeatureInfo>.toExtensionType(): List<ExtensionType> {
      val feature = first { it.name.startsWith(FEATURE) }
      val type = feature.name.substringAfter(FEATURE)
      return ExtensionType.entries.filter { it.feature == type }
    }


  }
}
