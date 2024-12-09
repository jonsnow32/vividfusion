package cloud.app.vvf.extension

import android.content.Context
import android.content.SharedPreferences
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.DatabaseExtension
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.StreamExtension
import cloud.app.vvf.common.clients.SubtitleExtension
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.priorityKey
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.createOrUpdateExtension
import cloud.app.vvf.datastore.helper.getExtension
import cloud.app.vvf.datastore.helper.getExtensions
import cloud.app.vvf.extension.plugger.FileChangeListener
import cloud.app.vvf.extension.plugger.PackageChangeListener
import cloud.app.vvf.plugin.DatabaseBuiltInExtensionRepo
import cloud.app.vvf.plugin.StreamBuiltInExtensionRepo
import cloud.app.vvf.utils.catchWith
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

class ExtensionLoader(
  private val context: Context,
  private val dataStore: DataStore,
  private val httpHelper: HttpHelper,
  private val throwableFlow: MutableSharedFlow<Throwable>,
  private val sharedPreferences: SharedPreferences,
  private val databaseExtensionListFlow: MutableStateFlow<List<DatabaseExtension>>,
  private val databaseExtensionFlow: MutableStateFlow<DatabaseExtension?>,
  private val streamExtensionListFlow: MutableStateFlow<List<StreamExtension>>,
  private val streamExtensionFlow: MutableStateFlow<StreamExtension?>,
  private val subtitleExtensionListFlow: MutableStateFlow<List<SubtitleExtension>>,
  private val subtitleExtensionFlow: MutableStateFlow<SubtitleExtension?>,
  private val extensionsFlow: MutableStateFlow<List<Extension<*>>>,
  private val refresher: MutableSharedFlow<Boolean>,

) {
  private val scope = MainScope() + CoroutineName("ExtensionLoader")
  private val appListener = PackageChangeListener(context)
  public val fileListener = FileChangeListener(scope)
  private val databaseEtxBuiltin = DatabaseBuiltInExtensionRepo()
  private val streamEtxBuiltin = StreamBuiltInExtensionRepo()

  private val databaseExtensionRepo = DatabaseExtensionRepo(
    context,
    httpHelper,
    appListener,
    fileListener,
    databaseEtxBuiltin
  )
  private val streamExtensionRepo =
    StreamExtensionRepo(
      context,
      httpHelper,
      appListener,
      fileListener,
      streamEtxBuiltin
    )
  private val subtitleExtensionRepo =
    SubtitleExtensionRepo(
      context,
      httpHelper,
      appListener,
      fileListener
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
    val databaseJob = scope.async {
      val databaseExtensions = databaseExtensionRepo.getPlugins()
        .map { list -> list.map { (metadata, client) -> DatabaseExtension(metadata, client) } }
        .first() // Ensure flow completes
      databaseExtensionListFlow.value = databaseExtensions
      databaseExtensionFlow.value = databaseExtensions.firstOrNull()
      databaseExtensions.saveExtensions()
    }

    val streamJob = scope.async {
      val streamExtensions = streamExtensionRepo.getPlugins()
        .map { list -> list.map { (metadata, client) -> StreamExtension(metadata, client) } }
        .first() // Ensure flow completes
      streamExtensionListFlow.value = streamExtensions
      streamExtensionFlow.value = streamExtensions.firstOrNull()
      streamExtensions.saveExtensions()
    }

    val subtitleJob = scope.async {
      val subtitleExtensions = subtitleExtensionRepo.getPlugins()
        .map { list -> list.map { (metadata, client) -> SubtitleExtension(metadata, client) } }
        .first() // Ensure flow completes
      subtitleExtensionListFlow.value = subtitleExtensions
      subtitleExtensionFlow.value = subtitleExtensions.firstOrNull()
      subtitleExtensions.saveExtensions()
    }

    // Await all tasks to complete
    listOf(databaseJob, streamJob, subtitleJob).awaitAll()

    // Combine all extensions
    extensionsFlow.value =
      databaseExtensionListFlow.value + streamExtensionListFlow.value + subtitleExtensionListFlow.value

    refresher.emit(false)
  }


  private suspend fun List<Extension<*>>.saveExtensions() = coroutineScope {
    map {
      async {
        val extension = dataStore.getExtension(it.type, it.id);
        it.metadata.enabled = extension?.enabled ?: false
        dataStore.createOrUpdateExtension(it.type, it.metadata)
      }
    }
  }

  private suspend fun <T : BaseClient> ExtensionRepo<T>.getPlugins(): Flow<List<Pair<ExtensionMetadata, Lazy<Result<T>>>>> {
    val pluginFlow = getAllPlugins().catchWith(throwableFlow).map { list ->
      list.mapNotNull { result ->
        result.getOrElse {
          throwableFlow.emit(ExtensionLoadingException(type, it.cause ?: it))
          null
        }//?.takeIf { isExtensionEnabled(type, it.first).enabled }
      }
    }
    val priorityFlow = votingMap[type]!!
    return pluginFlow.combine(priorityFlow) { list, set ->
      list.sortedBy { set.indexOf(it.first.id) }
    }
  }


  private val votingMap = ExtensionType.entries.associateWith {
    val key = it.priorityKey()
    val list = sharedPreferences.getString(key, null).orEmpty().split(',')
    MutableStateFlow(list)
  }

  private suspend fun isExtensionEnabled(type: ExtensionType, metadata: ExtensionMetadata) =
    withContext(Dispatchers.IO) {
      dataStore.getExtension(type, metadata.id)?.enabled
        ?.let { metadata.copy(enabled = it) } ?: metadata
    }
}
