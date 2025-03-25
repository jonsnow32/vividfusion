package cloud.app.vvf.ui.extension

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.ExtensionOpenerActivity
import cloud.app.vvf.ExtensionOpenerActivity.Companion.installExtension
import cloud.app.vvf.R
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.Message
import cloud.app.vvf.datastore.account.AccountDataStore
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.extension.ExtensionAssetResponse
import cloud.app.vvf.extension.ExtensionLoader
import cloud.app.vvf.extension.downloadUpdate
import cloud.app.vvf.extension.getExtensionList
import cloud.app.vvf.extension.getExtensions
import cloud.app.vvf.extension.getUpdateFileUrl
import cloud.app.vvf.extension.installExtension
import cloud.app.vvf.extension.uninstallExtension
import cloud.app.vvf.extension.waitForResult
import cloud.app.vvf.network.api.voting.VotingService
import cloud.app.vvf.ui.extension.widget.InstallStatus
import cloud.app.vvf.utils.navigate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionLoader: ExtensionLoader,
  val accountDataStoreFlow: MutableStateFlow<AccountDataStore>,
  val extListFlow: MutableStateFlow<List<Extension<*>>?>,
  val refresher: MutableSharedFlow<Boolean>,
  val okHttpClient: OkHttpClient,
  val messageFlow: MutableSharedFlow<Message>,
  val dataFlow: MutableStateFlow<AppDataStore>,
  val votingService: VotingService,
  val selectedExtension: MutableStateFlow<Extension<DatabaseClient>?>
) : CatchingViewModel(throwableFlow) {


  override fun onInitialize() {
    viewModelScope.launch {
      combine(extListFlow, dataFlow) { extensions, value ->
        val metadata = value.getCurrentDBExtension()
        metadata?.let { extensions?.find { it.id == metadata.className } }
          ?: extensions?.firstOrNull()
      }.collectLatest {
        selectedExtension.value = it as Extension<DatabaseClient>?
      }
    }

  }
  fun refresh() {
    viewModelScope.launch {
      refresher.emit(true)
    }

    viewModelScope.launch {
      extListFlow.collectLatest { extensions ->
        extensions ?: return@collectLatest
        val votingMap = mapOf<String, Int>(); //sort by voting api

        dataFlow.value.saveExtensions(extensions.map {
          it.metadata.rating = votingMap[it.id] ?: 0
          it.metadata
        })
      }
    }
  }

  fun getExtensionsByType(type: ExtensionType) = extListFlow.getExtensions(type)

  suspend fun install(context: FragmentActivity, file: File, installAsApk: Boolean): Boolean {
    val result = installExtension(context, file, installAsApk).getOrElse {
      throwableFlow.emit(it)
      false
    }
    if (result) messageFlow.emit(Message(context.getString(R.string.extension_installed_successfully)))
    return result
  }

  fun uninstall(
    context: FragmentActivity, extension: Extension<*>, function: (Boolean) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val result = uninstallExtension(context, extension).getOrElse {
        throwableFlow.emit(it)
        false
      }
      if (result) messageFlow.emit(Message(context.getString(R.string.extension_uninstalled_successfully)))
      function(result)
    }

  }

  fun addFromLinkOrCode(context: FragmentActivity, link: String) {
    viewModelScope.launch {
      val actualLink = when {
        link.startsWith("http://") or link.startsWith("https://") -> link
        else -> "https://v.gd/$link"
      }

      val list = runCatching { getExtensionList(actualLink, okHttpClient) }.getOrElse {
        throwableFlow.emit(it)
        return@launch
      }

      if (list.isEmpty()) {
        messageFlow.emit(Message(context.getString(R.string.list_is_empty)))
        return@launch
      }

      context.navigate(ExtensionRepoFragment.newInstance("name of repo", list))
    }
  }

  fun addFromFile(context: FragmentActivity) {
    viewModelScope.launch {
      val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "application/octet-stream"
        addCategory(Intent.CATEGORY_OPENABLE)
      }
      val result = context.waitForResult(intent)
      val file = result.data?.data ?: return@launch
      val newIntent = Intent(context, ExtensionOpenerActivity::class.java).apply {
        setData(file)
      }
      context.startActivity(newIntent)
    }
  }

  suspend fun addExtension(context: FragmentActivity, extension: ExtensionAssetResponse) =
    flow<InstallStatus> {
      val url = getUpdateFileUrl("", extension.url, okHttpClient).getOrElse {
        throwableFlow.emit(it)
        emit(InstallStatus.FAILED)
        null
      } ?: return@flow
      messageFlow.emit(
        Message(
          context.getString(R.string.downloading_update_for_extension, extension.name)
        )
      )
      emit(InstallStatus.DOWNLOADING)
      val file = downloadUpdate(context, url, okHttpClient).getOrElse {
        emit(InstallStatus.FAILED)
        throwableFlow.emit(it)
        return@flow
      }
      val result = context.installExtension(file.toUri().toString())
      emit(result)
    }

  fun addExtensions(context: FragmentActivity, extensions: List<ExtensionAssetResponse>) {
    viewModelScope.launch {
      extensions.forEach { extension ->
        val url = getUpdateFileUrl("", extension.url, okHttpClient).getOrElse {
          throwableFlow.emit(it)
          null
        } ?: return@forEach
        messageFlow.emit(
          Message(
            context.getString(R.string.downloading_update_for_extension, extension.name)
          )
        )
        val file = downloadUpdate(context, url, okHttpClient).getOrElse {
          throwableFlow.emit(it)
          return@forEach
        }
        context.installExtension(file.toUri().toString())
      }
    }
  }

  fun setExtensionEnabled(id: String, checked: Boolean) {
    val extension = dataFlow.value.getExtension(id)
    extension?.enabled = checked
    extension?.let { dataFlow.value.saveExtension(extension) }
    //viewModelScope.launch { refresher.emit(true) }
  }

  fun selectDbExtension(extension: Extension<*>) {
    selectedExtension.value = extension as Extension<DatabaseClient>?
    dataFlow.value.setCurrentDBExtension(extension.metadata)
  }

  private val voteMutex = Mutex()

  fun vote(extensionMetadata: ExtensionMetadata, type: ExtensionType) {
    viewModelScope.launch(Dispatchers.IO) {
//      val key = "${type.name}/${extensionMetadata.className}"
      voteMutex.withLock {
        val isVoted = accountDataStoreFlow.value.checkVoted(extensionMetadata, type)
        if (!isVoted) {
          val response = votingService.vote(type.name, extensionMetadata.className).execute()
          if (response.isSuccessful) {
            response.body()?.let {
              accountDataStoreFlow.value.setVotedExtension(extensionMetadata, type)
            }
          }
        } else {
          messageFlow.emit(Message("You have already voted for this extension"))
        }
      }
    }
  }
}
