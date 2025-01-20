package cloud.app.vvf.ui.login

import androidx.lifecycle.viewModelScope
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.User
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.CurrentUser
import cloud.app.vvf.datastore.helper.getAllUsers
import cloud.app.vvf.datastore.helper.getCurrentUser
import cloud.app.vvf.extension.ExtensionLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginUserViewModel @Inject constructor(
  userDao: DataStore,
  throwableFlow: MutableSharedFlow<Throwable>,
  val extensionFlow: MutableStateFlow<Extension<DatabaseClient>?>,
  val extensionLoader: ExtensionLoader,
) : CatchingViewModel(throwableFlow) {

    val currentExtension = MutableStateFlow<Extension<*>?>(null)
    val currentUser = MutableStateFlow<Pair<Extension<*>?, User?>>(null to null)

    init {
        suspend fun update() {
            val currentExt = currentExtension.value
            val user = userDao.getCurrentUser(currentExt?.id)
            currentUser.value = currentExt to user
        }
        viewModelScope.launch {
            launch { currentExtension.collect { update() } }
            //userDao.observeCurrentUser().collect { update() }
        }
    }

    val allUsers = currentExtension.map { extensionData ->
        val metadata = extensionData?.metadata
        withContext(Dispatchers.IO) {
            metadata to metadata?.className?.let { id ->
                userDao.getAllUsers(id)?.map { it }
            }
        }

    }

    fun logout(client: String?, user: String?) {
        if (client == null || user == null) return
        viewModelScope.launch(Dispatchers.IO) {
            //userDao.deleteUser(user, client)
        }
    }

    fun setLoginUser(user: User?) {
//        val currentUser = user?.toCurrentUser()
//            ?: CurrentUser(extensionFlow.value?.metadata?.id ?: return, null)
//        setLoginUser(currentUser)
    }

    fun setLoginUser(currentUser: CurrentUser) {
        viewModelScope.launch(Dispatchers.IO) {
            //userDao.setCurrentUser(currentUser)
        }
    }

}
