package cloud.app.vvf.extension

import android.content.Context
import android.content.SharedPreferences
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.provider.HttpHelperProvider
import cloud.app.vvf.common.clients.provider.MessageFlowProvider
import cloud.app.vvf.common.clients.provider.SettingProvider
import cloud.app.vvf.common.helpers.Injectable
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.common.models.extension.Message
import cloud.app.vvf.extension.builtIn.local.BuiltInClient
import cloud.app.vvf.extension.repo.CombinedRepository
import cloud.app.vvf.extension.repo.ExtensionParser
import cloud.app.vvf.extension.tmdb.TmdbTvdbClient
import cloud.app.vvf.utils.toSettings
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.io.File

class ExtensionLoader(
  private val context: Context,
  private val httpHelper: HttpHelper,
  private val throwableFlow: MutableSharedFlow<Throwable>,
  private val extensionsFlow: MutableStateFlow<List<Extension<*>>>,
  private val messageFlow: MutableSharedFlow<Message>,
  private val refresher: MutableSharedFlow<Boolean>,
  private val sharedPreferences: SharedPreferences,
) {

  val parser = ExtensionParser(context)
  private val scope = MainScope() + CoroutineName("ExtensionLoader")


  val fileIgnoreFlow = MutableSharedFlow<File?>()

  private val extensionRepo = CombinedRepository(
    scope, context, fileIgnoreFlow, parser,
    BuiltInClient.metadata to lazy { BuiltInClient(context) },
    TmdbTvdbClient.metadata to lazy { TmdbTvdbClient() }
  )

  fun initialize() {
    scope.launch {
      all.collectLatest { list ->
        extensionsFlow.value = list
      }
    }
  }

  val priorityMap = ExtensionType.entries.associateWith {
    val key = it.priorityKey()
    val list = sharedPreferences.getString(key, null).orEmpty().split(',')
    MutableStateFlow(list)
  }


  private fun <T : Extension<*>> mapped(
    transform: (ExtensionMetadata, Injectable<BaseClient>) -> T
  ) = injectedList.map { list ->
    list.mapNotNull {
      val (meta, injectable) = it.getOrNull() ?: return@mapNotNull null
      transform(meta, injectable)
    }
  }.stateIn(scope, SharingStarted.Lazily, emptyList())


  private fun <T : Extension<*>> mapped(type : ExtensionType,
    transform: (ExtensionMetadata, Injectable<BaseClient>) -> T
  ) = injectedList.map { list ->
    list.mapNotNull {
      val (meta, injectable) = it.getOrNull() ?: return@mapNotNull null
      if (!meta.types.contains(type)) return@mapNotNull null
      transform(meta, injectable)
    }
  }.stateIn(scope, SharingStarted.Lazily, emptyList())


  private val injectedList = extensionRepo.flow.map { list ->
    list?.groupBy { it.getOrNull()?.first?.run { types to className } }?.map { entry ->
      entry.value.minBy { it.getOrNull()?.first?.importType?.ordinal ?: Int.MAX_VALUE }
    }.orEmpty()
  }.map { list ->
    list.map { result ->
      result.map {
        it.first to it.second.injected(it.first)
      }
    }
  }

  val database = mapped(ExtensionType.DATABASE) { meta, injectable ->
    Extension(meta, injectable.casted())
  }

  val subtitle = mapped(ExtensionType.SUBTITLE) { meta, injectable ->
    Extension(meta, injectable.casted())
  }
  val stream = mapped(ExtensionType.STREAM) { meta, injectable ->
    Extension(meta, injectable.casted())
  }

  val all = combine(database, subtitle, stream) { db, sub, str ->
    (db + sub + str).distinctBy { it.id }
  }.stateIn(scope, SharingStarted.Lazily, emptyList())


  private fun Lazy<BaseClient>.injected(
    metadata: ExtensionMetadata
  ) = Injectable(::value, mutableListOf({
    setSetting((toSettings(sharedPreferences)))

    if (this is HttpHelperProvider) setHttpHelper(httpHelper)
    //if(this is ExtractorFlowProvider) setExtractorsFlow(extensionsFlow, messageFlow)
    if (this is MessageFlowProvider) setMessageFlow(messageFlow)

    onInitialize()
  }))


  private fun <T> List<T>.sorted(type: ExtensionType, id: (T) -> String): List<T> {
    val priority = priorityMap[type]!!.value
    return sortedBy { priority.indexOf(id(it)) }
  }

  companion object {
    fun ExtensionType.priorityKey() = "priority_${this.feature}"
  }
}
