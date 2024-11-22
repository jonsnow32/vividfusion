package cloud.app.vvf.ui.extension

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.ui.main.ClientNotSupportedAdapter
import cloud.app.vvf.ui.main.home.ClientLoadingAdapter
import cloud.app.vvf.viewmodels.SnackBarViewModel
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.DatabaseExtension
import cloud.app.vvf.common.clients.StreamExtension
import cloud.app.vvf.common.clients.SubtitleExtension
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ExtensionViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val settings: SharedPreferences,
  val databaseExtensionListFlow: MutableStateFlow<List<DatabaseExtension>>,
  val streamExtensionListFlow: MutableStateFlow<List<StreamExtension>>,
  val subtitleExtensionListFlow: MutableStateFlow<List<SubtitleExtension>>
) : CatchingViewModel(throwableFlow) {

  fun refresh() {
    viewModelScope.launch {
      tryWith{
        TODO("Not yet implemented")
      }
    }
  }


  companion object {
    fun Context.noClient() = SnackBarViewModel.Message(
      getString(R.string.error_no_client)
    )

    fun Context.searchNotSupported(client: String) = SnackBarViewModel.Message(
      getString(R.string.not_supported, getString(R.string.search), client)
    )

    fun Context.loginNotSupported(client: String) = SnackBarViewModel.Message(
      getString(R.string.not_supported, getString(R.string.login), client)
    )

    inline fun <reified T> RecyclerView.applyAdapter(
        extension: BaseClient?,
        name: Int,
        adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
        block: ((T?) -> Unit) = {}
    ) {
      block(extension as? T)
      setAdapter(
        if (extension == null)
          ClientLoadingAdapter()
        else if (extension !is T)
          ClientNotSupportedAdapter(name, extension.toString())
        else adapter
      )
    }
  }
}
