package cloud.app.vvf.ui.login

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.MainActivityViewModel.Companion.applyContentInsets
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.VVFApplication.Companion.loginNotSupported
import cloud.app.vvf.VVFApplication.Companion.noClient
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.user.LoginClient
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.databinding.FragmentLoginBinding
import cloud.app.vvf.databinding.ItemInputBinding
import cloud.app.vvf.extension.getExtension
import cloud.app.vvf.extension.isClient
import cloud.app.vvf.ui.exception.AppException
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.loadWith
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.onAppBarChangeListener
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.set
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class LoginFragment : Fragment() {
    companion object {
        fun newInstance(extensionId: String, clientName: String, extensionType: String) =
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putString("extensionId", extensionId)
                    putString("clientName", clientName)
                    putString("extensionType", extensionType)
                }
            }

        fun newInstance(error: AppException.LoginRequired) =
            newInstance(error.extensionId.id, error.extensionId.name, error.extensionId.metadata.types[0].name)

        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 2; Jeff Bezos) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36"
    }

    private var binding by autoCleared<FragmentLoginBinding>()
    private val clientType by lazy {
        val type = requireArguments().getString("extensionType")!!
        ExtensionType.valueOf(type)
    }
    private val extensionId by lazy { requireArguments().getString("extensionId")!! }
    private val clientName by lazy { requireArguments().getString("clientName")!! }
    private val loginViewModel by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    private suspend inline fun <reified T : LoginClient> Extension<*>.getClient(
        button: MaterialButton, noinline configure: FragmentLoginBinding.(T) -> Unit
    ) = run {
        val client = instance.value.getOrNull()
        if (client !is T) null
        else Pair(button) { configure(binding, client) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.loginContainer.applyContentInsets(it)
            binding.loadingContainer.root.applyContentInsets(it)
        }
        //applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.title = getString(R.string.extension_login, clientName)

        val extension = loginViewModel.extensionList.getExtension(extensionId)

        if (extension == null) {
            createSnack(requireContext().noClient())
            parentFragmentManager.popBackStack()
            return
        }

        val metadata = extension.metadata

        lifecycleScope.launch {
            if (!extension.isClient<LoginClient>()) {
                createSnack(requireContext().loginNotSupported(clientName))
                parentFragmentManager.popBackStack()
                return@launch
            }

            metadata.iconUrl?.toImageHolder()
                .loadWith(binding.extensionIcon, R.drawable.ic_extension_24dp) {
                    binding.extensionIcon.setImageDrawable(it)
                }

            binding.loginContainer.isVisible = true

            val clients = listOfNotNull(
                extension.getClient<LoginClient.UsernamePassword>(binding.loginUserPass) {
                    configureUsernamePassword(extension)
                },
                extension.getClient<LoginClient.WebView>(binding.loginWebview) {
                    configureWebView(extension, it)
                },
                extension.getClient<LoginClient.CustomTextInput>(binding.loginCustomInput) {
                    configureCustomTextInput(extension, it)
                },
            )

            if (clients.isEmpty()) {
                createSnack(requireContext().loginNotSupported(clientName))
                parentFragmentManager.popBackStack()
                return@launch
            } else if (clients.size == 1) loginViewModel.loginClient.value = 0
            else {
                binding.loginToggleGroup.isVisible = true
                clients.forEachIndexed { index, pair ->
                    val (button, _) = pair
                    button.isVisible = true
                    button.setOnClickListener {
                        loginViewModel.loginClient.value = index
                        binding.loginToggleGroup.isVisible = false
                    }
                }
            }
            observe(loginViewModel.loginClient) {
                it ?: return@observe
                binding.loginToggleGroup.isVisible = false
                clients[it].second()
            }
        }


        observe(loginViewModel.loadingOver) {
            parentFragmentManager.popBackStack()
        }
    }

    private fun FragmentLoginBinding.configureWebView(
        extension: Extension<*>,
        client: LoginClient.WebView
    ) = with(client) {
        webView.isVisible = true
        webView.applyDarkMode()
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                webView.goBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        webView.webViewClient = object : WebViewClient() {

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                callback.isEnabled = webView.canGoBack()
                url ?: return
                if (loginWebViewStopUrlRegex.find(url) != null) {
                    webView.stopLoading()
                    lifecycleScope.launch {
                        val data = webView.loadData(url, client)
                        webView.isVisible = false
                        loadingContainer.root.isVisible = true
                        callback.isEnabled = false
                        WebStorage.getInstance().deleteAllData()
                        CookieManager.getInstance().run {
                            removeAllCookies(null)
                            flush()
                        }
                        loginViewModel.onWebViewStop(extension, url, data)
                    }
                }
            }
        }
        webView.settings.apply {
            domStorageEnabled = true
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            userAgentString = loginWebViewInitialUrl.headers["User-Agent"]
                ?: USER_AGENT
        }
        webView.loadUrl(loginWebViewInitialUrl.url, loginWebViewInitialUrl.headers)

        lifecycleScope.launch {
            delay(1000)
            appBarLayout.setExpanded(false, true)
        }
    }

    private suspend fun WebView.loadData(url: String, client: LoginClient.WebView) = when (client) {
        is LoginClient.WebView.Cookie ->
            CookieManager.getInstance().getCookie(url) ?: ""

        is LoginClient.WebView.Evaluate -> suspendCoroutine {
            evaluateJavascript(client.javascriptToEvaluate) { result -> it.resume(result) }
        }
    }

    private fun WebView.applyDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.isAlgorithmicDarkeningAllowed = true
        }
    }

    private fun FragmentLoginBinding.configureCustomTextInput(
        extension: Extension<*>,
        client: LoginClient.CustomTextInput
    ) {
        customInputContainer.isVisible = true
        client.loginInputFields.forEachIndexed { index, field ->
            val input = ItemInputBinding.inflate(
                layoutInflater,
                customInput,
                false
            )
            input.root.hint = field.label
            input.editText.inputType = if (!field.isPassword) TYPE_CLASS_TEXT
            else TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
            input.editText.setText(loginViewModel.inputs[field.key])
            input.editText.doAfterTextChanged { editable ->
                loginViewModel.inputs[field.key] = editable.toString().takeIf { it.isNotBlank() }
            }
            input.editText.setOnEditorActionListener { _, _, _ ->
                if (index < client.loginInputFields.size - 1) {
                    customInput.getChildAt(index + 1).requestFocus()
                } else loginCustomSubmit.performClick()
                true
            }
            customInput.addView(input.root)
        }
        loginCustomSubmit.setOnClickListener {
            client.loginInputFields.forEach {
                if (it.isRequired && loginViewModel.inputs[it.key].isNullOrEmpty()) {
                    lifecycleScope.launch {
                        loginViewModel.messageFlow.emit(
                            SnackBarViewModel.Message(
                                getString(
                                    R.string.required_field,
                                    it.label
                                )
                            )
                        )
                    }
                    return@setOnClickListener
                }
            }
            loginViewModel.onCustomTextInputSubmit(extension)
            customInputContainer.isVisible = false
            loadingContainer.root.isVisible = true
        }
    }

    private fun FragmentLoginBinding.configureUsernamePassword(
        extension: Extension<*>
    ) {
        usernamePasswordContainer.isVisible = true
        loginUsername.requestFocus()
        loginUsername.setOnEditorActionListener { _, _, _ ->
            loginPassword.requestFocus()
            true
        }
        loginPassword.setOnEditorActionListener { _, _, _ ->
            loginUserPassSubmit.performClick()
            true
        }
        loginUserPassSubmit.setOnClickListener {
            val username = loginUsername.text.toString()
            val password = loginPassword.text.toString()
            if (username.isEmpty()) {
                lifecycleScope.launch {
                    loginViewModel.messageFlow.emit(
                        SnackBarViewModel.Message(
                            getString(R.string.required_field, getString(R.string.username))
                        )
                    )
                }
                return@setOnClickListener
            }
            loginViewModel.onUsernamePasswordSubmit(extension, username, password)
            usernamePasswordContainer.isVisible = false
            loadingContainer.root.isVisible = true
        }
    }
}



