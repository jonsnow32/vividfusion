package cloud.app.vvf.extension

import android.content.Context
import android.content.SharedPreferences
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.clients.subtitles.SubtitleClient
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.createOrUpdateExtension
import cloud.app.vvf.datastore.helper.getExtension
import cloud.app.vvf.extension.plugger.FileChangeListener
import cloud.app.vvf.extension.plugger.PackageChangeListener
import cloud.app.vvf.extension.builtIn.BuiltInRepo
import cloud.app.vvf.utils.catchWith
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

class ExtensionLoader(
  private val context: Context,
  private val dataStore: DataStore,
  private val httpHelper: HttpHelper,
  private val throwableFlow: MutableSharedFlow<Throwable>,
  private val sharedPreferences: SharedPreferences,

  private val currentDBExtFlow: MutableStateFlow<Extension<DatabaseClient>?>,
  private val currentStreamExtFlow: MutableStateFlow<Extension<StreamClient>?>,
  private val currentSubtitleExtFlow: MutableStateFlow<Extension<SubtitleClient>?>,

  private val extensionsFlow: MutableStateFlow<List<Extension<*>>>,
  private val refresher: MutableSharedFlow<Boolean>,

  ) {
  private val scope = MainScope() + CoroutineName("ExtensionLoader")
  private val appListener = PackageChangeListener(context)
  public val fileListener = FileChangeListener(scope)

  private val extensionRepo = ExtensionRepo(
    context,
    httpHelper,
    appListener,
    fileListener,
    BuiltInRepo()
  )

  fun initialize() {
    scope.launch {
      getAllPlugins(scope)
    }

    // Refresh Extensions
    scope.launch {
      refresher.collect {
        if (it) launch {
          getAllPlugins(scope)
        }
      }
    }
  }

  private suspend fun getAllPlugins(scope: CoroutineScope) {
    val extensions = extensionRepo.getPlugins()
      .map { list ->
        list.mapNotNull { (metadata, client) ->
          val type =
            metadata.types?.map { type -> ExtensionType.entries.first { it.feature == type.feature } }
          if (type != null)
            Extension(type, metadata, client)
          else
            null
        }
      }.first()

    extensionsFlow.value = extensions

    val databaseExtension = extensions.firstOrNull {
      it.metadata.types?.contains(ExtensionType.DATABASE) == true
    }
    currentDBExtFlow.value = databaseExtension as Extension<DatabaseClient>


    val streamExtension = extensions.firstOrNull {
      it.metadata.types?.contains(ExtensionType.STREAM) == true
    }
    currentStreamExtFlow.value = streamExtension as Extension<StreamClient>


    refresher.emit(false)
  }


  private suspend fun List<Extension<*>>.saveExtensions() = coroutineScope {
    map {
      async {
        val extension = dataStore.getExtension(it.id);
        it.metadata.enabled = extension?.enabled ?: false
        dataStore.createOrUpdateExtension(it.metadata)
      }
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

    return pluginFlow.map { list ->
      list.sortedBy { votingMap[it.first.className] }
    }
  }

  private val votingMap = mapOf<String, Int>(); //sort by voting api

  private suspend fun isExtensionEnabled(type: ExtensionType, metadata: ExtensionMetadata) =
    withContext(Dispatchers.IO) {
      dataStore.getExtension(metadata.className)?.enabled
        ?.let { metadata.copy(enabled = it) } ?: metadata
    }
}
