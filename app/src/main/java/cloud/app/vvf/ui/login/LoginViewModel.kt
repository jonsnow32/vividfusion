package cloud.app.vvf.ui.login

import android.app.Application
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.R
import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.user.LoginClient
import cloud.app.vvf.common.models.User
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.extension.get
import cloud.app.vvf.viewmodels.SnackBarViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val extensionList: MutableStateFlow<List<Extension<*>>?>,
    private val context: Application,
    val messageFlow: MutableSharedFlow<SnackBarViewModel.Message>,
    database: DataStore,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    val loginClient: MutableStateFlow<Int?> = MutableStateFlow(null)
    val loadingOver = MutableSharedFlow<Unit>()

    private suspend fun afterLogin(
        extension: Extension<*>,
        users: List<User>
    ) {
        if (users.isEmpty()) {
            messageFlow.emit(SnackBarViewModel.Message(context.getString(R.string.no_user_found)))
            return
        }
//        val entities = users.map { it.toEntity(extension.id) }
//        userDao.setUsers(entities)
//        val user = entities.first()
//        userDao.setCurrentUser(user.toCurrentUser())
        loadingOver.emit(Unit)
    }

    fun onWebViewStop(
        extension: Extension<*>,
        url: String,
        cookie: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.WebView, List<User>>(throwableFlow) {
                onLoginWebviewStop(url, cookie)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }

    fun onUsernamePasswordSubmit(
        extension: Extension<*>,
        username: String,
        password: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.UsernamePassword, List<User>>(throwableFlow) {
                onLogin(username, password)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }

    val inputs = mutableMapOf<String, String?>()
    fun onCustomTextInputSubmit(
        extension: Extension<*>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.CustomTextInput, List<User>?>(throwableFlow) {
                onLogin(inputs)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }
}
