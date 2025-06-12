package cloud.app.vvf.extension

import android.content.Context
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.provider.HttpHelperProvider
import cloud.app.vvf.common.clients.provider.MessageFlowProvider
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.common.models.extension.Message
import cloud.app.vvf.extension.plugger.FileChangeListener
import cloud.app.vvf.extension.plugger.LazyPluginRepo
import cloud.app.vvf.extension.plugger.LazyRepoComposer
import cloud.app.vvf.extension.plugger.PackageChangeListener
import cloud.app.vvf.extension.plugger.catchLazy
import cloud.app.vvf.utils.getSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import tel.jeelpa.plugger.utils.mapState
import java.io.File

class ExtensionRepo<T : BaseClient>(
  private val context: Context,
  private val httpHelper: HttpHelper,
  private val messageFlow: MutableSharedFlow<Message>,
  private val listener: PackageChangeListener,
  private val fileChangeListener: FileChangeListener,
  private vararg val repo: LazyPluginRepo<ExtensionMetadata, T>
) : LazyPluginRepo<ExtensionMetadata, T> {
  private val composed by lazy {
//    val loader = AndroidPluginLoader<T>(context)
//    val dir = context.getPluginFileDir()
//    val filePluginRepo = LazyPluginRepoImpl(
//      FilePluginSource(dir, fileChangeListener.scope, fileChangeListener.flow),
//      FileManifestParser(context.packageManager),
//      loader,
//    )
//    val appPluginRepo = LazyPluginRepoImpl(
//      ApkPluginSource(listener, context, FEATURE),
//      ApkManifestParser(ImportType.App),
//      loader
//    )
//    LazyRepoComposer(*repo, appPluginRepo, filePluginRepo)
    LazyRepoComposer(*repo)
  }

  private fun injected() = composed.getAllPlugins().mapState { list ->
    list.map {
      runCatching {
        val plugin = it.getOrThrow()
        val (metadata, resultLazy) = plugin
        metadata to catchLazy {
          val instance = resultLazy.value.getOrThrow()
          instance.init(getSettings(context, metadata))
          if (instance is HttpHelperProvider) instance.setHttpHelper(httpHelper)
          if (instance is MessageFlowProvider) instance.setMessageFlow(messageFlow)
          instance
        }
      }
    }
  }

  override fun getAllPlugins() = injected()

  companion object {
    const val FEATURE = "cloud.app.vvf.extension"
    fun Context.getPluginFileDir() =
      File(filesDir, "extensions").apply { mkdirs() }
  }
}
