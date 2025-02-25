package cloud.app.vvf.ui.extension

import android.content.Intent
import android.content.SharedPreferences
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.ExtensionOpenerActivity
import cloud.app.vvf.ExtensionOpenerActivity.Companion.installExtension
import cloud.app.vvf.R
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.getExtension
import cloud.app.vvf.datastore.helper.saveExtension
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
import cloud.app.vvf.viewmodels.SnackBarViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
  val settings: SharedPreferences,
  val extensionListFlow: MutableStateFlow<List<Extension<*>>>,
  val refresher: MutableSharedFlow<Boolean>,
  val okHttpClient: OkHttpClient,
  val messageFlow: MutableSharedFlow<SnackBarViewModel.Message>,
  val dataStore: DataStore,
  val votingService: VotingService
) : CatchingViewModel(throwableFlow) {

  fun refresh() {
    viewModelScope.launch {
      refresher.emit(true)
    }
  }

  fun getExtensionsByType(type: ExtensionType) = extensionListFlow.getExtensions(type)

  suspend fun install(context: FragmentActivity, file: File, installAsApk: Boolean): Boolean {
    val result = installExtension(context, file, installAsApk).getOrElse {
      throwableFlow.emit(it)
      false
    }
    if (result) messageFlow.emit(SnackBarViewModel.Message(context.getString(R.string.extension_installed_successfully)))
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
      if (result) messageFlow.emit(SnackBarViewModel.Message(context.getString(R.string.extension_uninstalled_successfully)))
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
        messageFlow.emit(SnackBarViewModel.Message(context.getString(R.string.list_is_empty)))
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
        SnackBarViewModel.Message(
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
          SnackBarViewModel.Message(
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
    val extension = dataStore.getExtension(id)
    extension?.enabled = checked
    extension?.let { dataStore.saveExtension(extension) }
    //viewModelScope.launch { refresher.emit(true) }
  }
  private val voteMutex = Mutex()

  fun vote(extensionMetadata: ExtensionMetadata, type: ExtensionType) {
    viewModelScope.launch(Dispatchers.IO) {
      val key = "${type.name}/${extensionMetadata.className}"
      voteMutex.withLock {
        val isVoted = dataStore.getKey<Boolean>(key) == true
        if (!isVoted) {
          val response = votingService.vote(type.name, extensionMetadata.className).execute()
          if (response.isSuccessful) {
            response.body()?.let {
              dataStore.setKey(key, true)
            }
          }
        } else {
          messageFlow.emit(SnackBarViewModel.Message("You have already voted for this extension"))
        }
      }
    }
  }
}
