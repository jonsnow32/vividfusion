package cloud.app.vvf.ui.widget.dialog.account

import android.content.Context
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.datastore.account.AccountDataStore
import cloud.app.vvf.datastore.app.AppDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountDialogViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<List<Extension<*>>?>,
  val accountFlow: MutableStateFlow<AccountDataStore>,
  val dataFlow: MutableStateFlow<AppDataStore>
) : CatchingViewModel(throwableFlow) {
  val accounts = MutableStateFlow<List<Account>?>(null)

  fun loadAccounts() {
    viewModelScope.launch(Dispatchers.IO) {
      accountFlow.collectLatest { value ->
        val allAccounts = value.getAllAccounts()
        accounts.value = allAccounts
      }
    }
  }

  fun removeAccount(account: Account) {
    accountFlow.value.removeAccount(account.getSlug())
    loadAccounts()
  }

  fun setActiveAccount(context: Context?, account: Account, callback: (Boolean) -> Unit) {
    context ?: return

    val result = accountFlow.value.setActiveAccount(account)
    if(result)
      dataFlow.value = AppDataStore(context, account)
    callback(result)
  }
}
