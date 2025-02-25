package cloud.app.vvf.common.clients.user

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.models.Request
import cloud.app.vvf.common.models.User

sealed interface LoginClient : BaseClient{

    interface UsernamePassword : LoginClient {
        suspend fun onLogin(username: String, password: String): List<User>
    }

    sealed interface WebView : LoginClient {
        val loginWebViewInitialUrl: Request
        val loginWebViewStopUrlRegex: Regex
        suspend fun onLoginWebviewStop(url: String, data: String): List<User>
        interface Cookie : WebView
        interface Evaluate : WebView {
            val javascriptToEvaluate: String
        }
    }

    interface CustomTextInput : LoginClient {
        val loginInputFields: List<InputField>
        suspend fun onLogin(data: Map<String, String?>): List<User>?
    }

    data class InputField(
        val key: String,
        val label: String,
        val isRequired: Boolean,
        val isPassword: Boolean = false
    )

    suspend fun onSetLoginUser(user: User?)
}
