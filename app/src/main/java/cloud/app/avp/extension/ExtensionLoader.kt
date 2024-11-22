package cloud.app.avp.extension

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.FOLDER_PLUGIN_SETTINGS
import cloud.app.avp.datastore.helper.getExtension
import cloud.app.avp.extension.plugger.FileChangeListener
import cloud.app.avp.extension.plugger.PackageChangeListener
import cloud.app.avp.plugin.DatabaseBuiltInExtensionRepo
import cloud.app.avp.plugin.StreamBuiltInExtensionRepo
import cloud.app.avp.utils.catchWith
import cloud.app.common.clients.BaseClient
import cloud.app.common.clients.DatabaseExtension
import cloud.app.common.clients.Extension
import cloud.app.common.clients.StreamExtension
import cloud.app.common.clients.SubtitleExtension
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.ExtensionMetadata
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.priorityKey
import cloud.app.common.settings.PrefSettings
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
  private val databaseExtensionListFlow: MutableStateFlow<List<DatabaseExtension>>,
  private val databaseExtensionFlow: MutableStateFlow<DatabaseExtension?>,
  private val streamExtensionListFlow: MutableStateFlow<List<StreamExtension>>,
  private val streamExtensionFlow: MutableStateFlow<StreamExtension?>,
  private val subtitleExtensionListFlow: MutableStateFlow<List<SubtitleExtension>>,
  private val subtitleExtensionFlow: MutableStateFlow<SubtitleExtension?>,
  private val extensionsFlow: MutableStateFlow<List<Extension<*>>>

) {
  private val scope = MainScope() + CoroutineName("ExtensionLoader")
  private val listener = PackageChangeListener(context)
  private val fileListener = FileChangeListener(scope)
  private val databaseEtxBuiltin = DatabaseBuiltInExtensionRepo()
  private val streamEtxBuiltin = StreamBuiltInExtensionRepo()

  private val databaseExtensionRepo = DatabaseExtensionRepo(
    context,
    httpHelper,
    toSettings(ExtensionType.DATABASE),
    listener,
    fileListener,
    databaseEtxBuiltin
  )
  private val streamExtensionRepo =
    StreamExtensionRepo(
      context,
      httpHelper,
      toSettings(ExtensionType.STREAM),
      listener,
      fileListener,
      streamEtxBuiltin
    )
  private val subtitleExtensionRepo =
    SubtitleExtensionRepo(
      context,
      httpHelper,
      toSettings(ExtensionType.SUBTITLE),
      listener,
      fileListener
    )

  fun initialize() {
    scope.launch {
      getAllPlugins(scope)
    }
  }

  private fun toSettings(type: ExtensionType) = object : PrefSettings {
    override fun getString(key: String) =
      sharedPreferences.getString("$FOLDER_PLUGIN_SETTINGS/${type.feature}/$key", null)

    override fun putString(key: String, value: String?) {
      sharedPreferences.edit { putString("$FOLDER_PLUGIN_SETTINGS/${type.feature}/$key", value) }
    }

    override fun getInt(key: String) =
      if (sharedPreferences.contains(key)) sharedPreferences.getInt(
        "$FOLDER_PLUGIN_SETTINGS/${type.feature}/$key",
        0
      )
      else null

    override fun putInt(key: String, value: Int?) {
      sharedPreferences.edit { putInt(key, value) }
    }

    override fun getBoolean(key: String) =
      if (sharedPreferences.contains(key)) sharedPreferences.getBoolean(
        "$FOLDER_PLUGIN_SETTINGS/${type.feature}/$key",
        false
      )
      else null

    override fun putBoolean(key: String, value: Boolean?) {
      sharedPreferences.edit { putBoolean("$FOLDER_PLUGIN_SETTINGS/${type.feature}/$key", value) }
    }
  }

  private suspend fun getAllPlugins(scope: CoroutineScope) {
    val databaseJob = scope.async {
      databaseExtensionRepo.getPlugins().map { list ->
        list.map { (metadata, client) -> DatabaseExtension(metadata, client) }
      }.collect { databaseExtensions ->
        databaseExtensionListFlow.value = databaseExtensions
        databaseExtensionFlow.value = databaseExtensions.firstOrNull()
      }
    }

    val streamJob = scope.async {
      streamExtensionRepo.getPlugins().map { list ->
        list.map { (metadata, client) -> StreamExtension(metadata, client) }
      }.collect { streamExtensions ->
        streamExtensionListFlow.value = streamExtensions
        streamExtensionFlow.value = streamExtensions.firstOrNull()
      }
    }

     listOf(databaseJob, streamJob).awaitAll()
    // Wait for both tasks to complete
    extensionsFlow.value = databaseExtensionListFlow.value + streamExtensionListFlow.value
  }
  private suspend fun <T : BaseClient> ExtensionRepo<T>.getPlugins(): Flow<List<Pair<ExtensionMetadata, Lazy<Result<T>>>>> {
    val pluginFlow = getAllPlugins().catchWith(throwableFlow).map { list ->
      list.mapNotNull { result ->
        result.getOrElse {
          throwableFlow.emit(ExtensionLoadingException(type, it.cause ?: it))
          null
        }?.takeIf { isExtensionEnabled(type, it.first).enabled }
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
