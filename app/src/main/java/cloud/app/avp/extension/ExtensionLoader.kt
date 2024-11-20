package cloud.app.avp.extension

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.FOLDER_PLUGIN_SETTINGS
import cloud.app.avp.datastore.helper.getExtension
import cloud.app.avp.extension.plugger.FileChangeListener
import cloud.app.avp.extension.plugger.PackageChangeListener
import cloud.app.avp.plugin.BuiltInExtensionRepo
import cloud.app.avp.utils.catchWith
import cloud.app.common.clients.BaseClient
import cloud.app.common.clients.DatabaseExtension
import cloud.app.common.clients.StreamExtension
import cloud.app.common.clients.SubtitleExtension
import cloud.app.common.clients.mvdatabase.DatabaseClient
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.ExtensionType
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import cloud.app.common.models.ExtensionMetadata
import cloud.app.common.models.priorityKey
import cloud.app.common.settings.PrefSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class ExtensionLoader(
  private val context: Context,
  private val dataStore: DataStore,
  private val httpHelper: HttpHelper,
  private val throwableFlow: MutableSharedFlow<Throwable>,
  private val sharedPreferences: SharedPreferences,
  private val databaseExtensionListFlow: MutableStateFlow<List<DatabaseExtension>>,
  private val databaseExtensionFlow: MutableStateFlow<DatabaseExtension?>,
  private val streamExtensionFlow: MutableStateFlow<List<StreamExtension>>,
  private val subtitleExtensionFlow: MutableStateFlow<List<SubtitleExtension>>,
) {
  private val scope = MainScope() + CoroutineName("ExtensionLoader")
  private val listener = PackageChangeListener(context)
  private val fileListener = FileChangeListener(scope)
  private val builtIn = BuiltInExtensionRepo<DatabaseClient>()

  private val databaseExtensionRepo = DatabaseExtensionRepo(
    context,
    httpHelper,
    toSettings(ExtensionType.DATABASE),
    listener,
    fileListener,
    builtIn
  )
  private val streamExtensionRepo =
    StreamExtensionRepo(
      context,
      httpHelper,
      toSettings(ExtensionType.STREAM),
      listener,
      fileListener
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
    val databasePlugins = MutableStateFlow<Unit?>(null)
    val streamPlugins = MutableStateFlow<Unit?>(null)
    val subtitlePlugins = MutableStateFlow<Unit?>(null)

    scope.launch {
      databaseExtensionRepo.getPlugins { list ->
        val databaseExtensions = list.map { (metadata, client) ->
          DatabaseExtension(metadata, client)
        }
        databaseExtensionListFlow.value = databaseExtensions
        databaseExtensionFlow.value = databaseExtensions.get(0)
        databasePlugins.emit(Unit)
      }
    }
  }

  private suspend fun <T : BaseClient> ExtensionRepo<T>.getPlugins(
    collector: FlowCollector<List<Pair<ExtensionMetadata, Lazy<Result<T>>>>>
  ) {
    val pluginFlow = getAllPlugins().catchWith(throwableFlow).map { list ->
      list.mapNotNull { result ->
        val (metadata, client) = result.getOrElse {
          val error = it.cause ?: it
          throwableFlow.emit(ExtensionLoadingException(type, error))
          null
        } ?: return@mapNotNull null
        val metadataEnabled = isExtensionEnabled(type, metadata)
        Pair(metadataEnabled, client)
      }
    }
    val priorityFlow = votingMap[type]!!
    pluginFlow.combine(priorityFlow) { list, set ->
      list.sortedBy { set.indexOf(it.first.id) }
    }.collect(collector)
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
