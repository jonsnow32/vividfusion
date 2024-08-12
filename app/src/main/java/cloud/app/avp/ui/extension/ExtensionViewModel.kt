package cloud.app.avp.ui.extension

import android.content.Context
import android.content.SharedPreferences
import androidx.recyclerview.widget.RecyclerView
import cloud.app.avp.R
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.ui.main.ClientNotSupportedAdapter
import cloud.app.avp.ui.main.home.ClientLoadingAdapter
import cloud.app.avp.utils.mapState
import cloud.app.avp.viewmodels.SnackBarViewModel
import cloud.app.common.clients.BaseExtension
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@HiltViewModel
class ExtensionViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<BaseExtension>,
  val extensionFlowList: MutableStateFlow<List<BaseExtension>>,
  val settings: SharedPreferences,
//  val refresher: MutableSharedFlow<Boolean>
) : CatchingViewModel(throwableFlow) {

  fun refresh() {
    TODO("Not yet implemented")
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
      extension: BaseExtension?,
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
