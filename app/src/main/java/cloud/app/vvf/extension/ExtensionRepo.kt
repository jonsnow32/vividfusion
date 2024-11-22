package cloud.app.vvf.extension

import android.content.Context
import cloud.app.vvf.extension.plugger.AndroidPluginLoader
import cloud.app.vvf.extension.plugger.ApkFileManifestParser
import cloud.app.vvf.extension.plugger.ApkManifestParser
import cloud.app.vvf.extension.plugger.ApkPluginSource
import cloud.app.vvf.extension.plugger.FileChangeListener
import cloud.app.vvf.extension.plugger.FilePluginSource
import cloud.app.vvf.extension.plugger.LazyPluginRepo
import cloud.app.vvf.extension.plugger.LazyPluginRepoImpl
import cloud.app.vvf.extension.plugger.LazyRepoComposer
import cloud.app.vvf.extension.plugger.PackageChangeListener
import cloud.app.vvf.extension.plugger.catchLazy
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.clients.subtitles.SubtitleClient
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.settings.PrefSettings
import tel.jeelpa.plugger.utils.mapState
import java.io.File

sealed class ExtensionRepo<T : BaseClient>(
  private val context: Context,
  private val httpHelper: HttpHelper,
  private val prefSettings: PrefSettings,
  private val listener: PackageChangeListener,
  private val fileChangeListener: FileChangeListener,
  private vararg val repo: LazyPluginRepo<ExtensionMetadata, T>
) : LazyPluginRepo<ExtensionMetadata, T> {
  abstract val type: ExtensionType

  private val composed by lazy {
    val loader = AndroidPluginLoader<T>(context)
    val dir = context.getPluginFileDir(type)
    val apkFilePluginRepo = LazyPluginRepoImpl(
      FilePluginSource(dir, fileChangeListener.scope, fileChangeListener.getFlow(type)),
      ApkFileManifestParser(context.packageManager, ApkManifestParser(ImportType.File)),
      loader,
    )
    val appPluginRepo = LazyPluginRepoImpl(
      ApkPluginSource(listener, context, "$FEATURE${type.feature}"),
      ApkManifestParser(ImportType.App),
      loader
    )
    LazyRepoComposer(*repo, appPluginRepo, apkFilePluginRepo)
  }

  private fun injected() = composed.getAllPlugins().mapState { list ->
    list.map {
      runCatching {
        val plugin = it.getOrThrow()
        val (metadata, resultLazy) = plugin
        metadata to catchLazy {
          val instance = resultLazy.value.getOrThrow()
          instance.init(prefSettings, httpHelper)
          instance
        }
      }
    }
  }

  override fun getAllPlugins() = injected()

  companion object {
    const val FEATURE = "cloud.app.avp.extension."
    fun Context.getPluginFileDir(type: ExtensionType) =
      File(filesDir, type.feature).apply { mkdirs() }
  }
}


class DatabaseExtensionRepo(
  context: Context,
  httpHelper: HttpHelper,
  prefSettings: PrefSettings,
  listener: PackageChangeListener,
  fileChangeListener: FileChangeListener,
  vararg repo: LazyPluginRepo<ExtensionMetadata, DatabaseClient>
) : ExtensionRepo<DatabaseClient>(
  context,
  httpHelper,
  prefSettings,
  listener,
  fileChangeListener,
  *repo
) {
  override val type = ExtensionType.DATABASE
}

class StreamExtensionRepo(
  context: Context,
  httpHelper: HttpHelper,
  prefSettings: PrefSettings,
  listener: PackageChangeListener,
  fileChangeListener: FileChangeListener,
  vararg repo: LazyPluginRepo<ExtensionMetadata, StreamClient>
) : ExtensionRepo<StreamClient>(
  context,
  httpHelper,
  prefSettings,
  listener,
  fileChangeListener,
  *repo
) {
  override val type = ExtensionType.STREAM
}

class SubtitleExtensionRepo(
  context: Context,
  httpHelper: HttpHelper,
  prefSettings: PrefSettings,
  listener: PackageChangeListener,
  fileChangeListener: FileChangeListener,
  vararg repo: LazyPluginRepo<ExtensionMetadata, SubtitleClient>
) : ExtensionRepo<SubtitleClient>(
  context,
  httpHelper,
  prefSettings,
  listener,
  fileChangeListener,
  *repo
) {
  override val type = ExtensionType.SUBTITLE
}
