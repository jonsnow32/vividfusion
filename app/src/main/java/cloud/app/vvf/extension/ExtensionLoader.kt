package cloud.app.vvf.extension

import android.content.Context
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.Message
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.extension.builtIn.BuiltInRepo
import cloud.app.vvf.extension.plugger.FileChangeListener
import cloud.app.vvf.extension.plugger.PackageChangeListener
import cloud.app.vvf.utils.catchWith
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class ExtensionLoader(
  private val context: Context,
  private val httpHelper: HttpHelper,
  private val throwableFlow: MutableSharedFlow<Throwable>,
  private val extensionsFlow: MutableStateFlow<List<Extension<*>>>,
  private val messageFlow: MutableSharedFlow<Message>,
  private val refresher: MutableSharedFlow<Boolean>,

  ) {

  private val scope = MainScope() + CoroutineName("ExtensionLoader")
  private val appListener = PackageChangeListener(context)
  public val fileListener = FileChangeListener(scope)

  private val extensionRepo = ExtensionRepo(
    context,
    httpHelper,
    messageFlow,
    appListener,
    fileListener,
    BuiltInRepo(context)
  )

  fun initialize() {
    scope.launch {
      load(scope)
    }

    // Refresh Extensions
    scope.launch {
      refresher.collect {
        if (it) launch {
          load(scope)
        }
      }
    }


  }

  private suspend fun load(scope: CoroutineScope) {
    extensionRepo.getPlugins()
      .map { list ->
        list.map { (metadata, client) ->
          Extension(metadata, client)
        }
      }.collectLatest { extensions ->
        extensionsFlow.value = extensions
        refresher.emit(false)
      }
  }


  private suspend fun <T : BaseClient> ExtensionRepo<T>.getPlugins(): Flow<List<Pair<ExtensionMetadata, Lazy<Result<T>>>>> {
    val pluginFlow = getAllPlugins().catchWith(throwableFlow).map { list ->
      list.mapNotNull { result ->
        val metadata = result.getOrNull()?.first
        result.getOrElse {
          throwableFlow.emit(ExtensionLoadingException(metadata, it.cause ?: it))
          null
        }
      }
    }
    return pluginFlow
  }
}
