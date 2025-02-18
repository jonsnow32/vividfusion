package cloud.app.vvf.ui.exception

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.ExceptionActivity
import cloud.app.vvf.MainActivityViewModel.Companion.applyContentInsets
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.VVFApplication.Companion.appVersion
import cloud.app.vvf.databinding.FragmentExceptionBinding
import cloud.app.vvf.extension.ExtensionLoadingException
import cloud.app.vvf.extension.RequiredExtensionsException
import cloud.app.vvf.utils.ContinuationCallback.Companion.await
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerial
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.onAppBarChangeListener
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.setupTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

class ExceptionFragment : Fragment() {
  private var binding by autoCleared<FragmentExceptionBinding>()
  private val throwable by lazy {
    requireArguments().getSerialized<ExceptionActivity.ExceptionDetails>("exception")!!
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentExceptionBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupTransition(view)
    applyInsets {
      binding.exceptionIconContainer.updatePadding(top = it.top)
      binding.nestedScrollView.applyContentInsets(it)
    }

    binding.appBarLayout.onAppBarChangeListener { offset ->
      binding.toolbarOutline.alpha = offset
    }

    binding.exceptionMessage.apply {
      val icon = navigationIcon
      navigationIcon = icon.takeIf { parentFragmentManager.fragments.size > 1 }
      setNavigationOnClickListener {
        parentFragmentManager.popBackStack()
      }
    }

    binding.exceptionMessage.title = throwable.title
    binding.exceptionDetails.text = throwable.causedBy
    binding.exceptionMessage.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.exception_copy -> {
          lifecycleScope.launch {
            val pasteLink = getPasteLink(throwable).getOrElse {
              throwable.causedBy
            }
            requireContext().copyToClipboard("Error", pasteLink)
          }
          true
        }
        else -> false
      }
    }
  }

  companion object {
    private val client = OkHttpClient()

    suspend fun getPasteLink(throwable: ExceptionActivity.ExceptionDetails) =
      withContext(Dispatchers.IO) {
        val details = throwable.causedBy
        val request = Request.Builder()
          .url("https://paste.rs")
          .post(details.toRequestBody())
          .build()
        runCatching { client.newCall(request).await().body.string() }
      }

    fun Context.copyToClipboard(label: String?, string: String) {
      val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(label, string)
      clipboard.setPrimaryClip(clip)
    }

    fun Context.getTitle(throwable: Throwable): String = when (throwable) {
      is IncompatibleClassChangeError -> getString(R.string.incompatible_class)
      is UnknownHostException, is UnresolvedAddressException -> getString(R.string.no_internet)
      is ExtensionLoadingException -> "${getString(R.string.invalid_extension)} : ${throwable.metadata?.className}"
      is RequiredExtensionsException -> getString(
        R.string.extension_requires,
        throwable.name,
        throwable.requiredExtensions.joinToString(", ")
      )

      is AppException -> throwable.run {
        when (this) {
          is AppException.Unauthorized ->
            getString(R.string.unauthorized, extension.name)

          is AppException.LoginRequired ->
            getString(R.string.login_required, extension.name)

          is AppException.NotSupported ->
            getString(R.string.is_not_supported, operation, extension.name)

          is AppException.Other -> "${extension.name} : ${getTitle(cause)}"
        }
      }

      else -> throwable.message ?: getString(R.string.error)
    }

    fun Context.getDetails(throwable: Throwable): String = when (throwable) {
      is RequiredExtensionsException -> """
          Extension : ${throwable.name}
          Required Extensions : ${throwable.requiredExtensions.joinToString(", ")}
          """.trimIndent()

      is AppException -> """
          Extension : ${throwable.extension.name}
          Id : ${throwable.extension.name}
          Type : ${throwable.extension.metadata.types?.joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \"") { it.feature } ?: "[]"}
          Version : ${throwable.extension.metadata.version}
          App Version : ${appVersion()}
          ${getDetails(throwable.cause)}
          """.trimIndent()

      else -> throwable.stackTraceToString()
    }

    fun newInstance(context: Context, throwable: Throwable) = ExceptionFragment().apply {
      arguments = Bundle().apply {
        putSerialized(
          "exception",
          ExceptionActivity.ExceptionDetails(
            context.getTitle(throwable),
            context.getDetails(throwable)
          )
        )
      }
    }

    fun newInstance(details: ExceptionActivity.ExceptionDetails) = ExceptionFragment().apply {
      arguments = Bundle().apply {
        putSerialized("exception", details)
      }
    }
  }
}
