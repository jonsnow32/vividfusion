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
import cloud.app.vvf.ExceptionActivity
import cloud.app.vvf.MainActivityViewModel.Companion.applyContentInsets
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentExceptionBinding
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerial
import cloud.app.vvf.utils.onAppBarChangeListener
import cloud.app.vvf.utils.setupTransition
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

class ExceptionFragment : Fragment() {
  private var binding by autoCleared<FragmentExceptionBinding>()
  private val throwable by lazy { requireArguments().getSerial<Throwable>("exception")!! }

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

    binding.exceptionMessage.title = requireContext().getTitle(throwable)
    binding.exceptionDetails.text = requireContext().getDetails(throwable)
    binding.exceptionMessage.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.exception_copy -> {
          with(requireContext()) {
            copyToClipboard(throwable.message, "```\n${getDetails(throwable)}\n```")
          }
          true
        }
        else -> false
      }
    }
  }

  companion object {
    fun Context.copyToClipboard(label: String?, string: String) {
      val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(label, string)
      clipboard.setPrimaryClip(clip)
    }

    fun Context.getTitle(throwable: Throwable): String = when (throwable) {
      is IncompatibleClassChangeError -> getString(R.string.incompatible_class)
      is ExceptionActivity.AppCrashException -> getString(R.string.app_crashed)
      is UnknownHostException, is UnresolvedAddressException -> getString(R.string.no_internet)
      else -> throwable.message ?: getString(R.string.error)
    }

    @Suppress("UnusedReceiverParameter")
    fun Context.getDetails(throwable: Throwable) = when (throwable) {
      is ExceptionActivity.AppCrashException -> throwable.causedBy
      else -> throwable.stackTraceToString()
    }

    fun newInstance(throwable: Throwable) = ExceptionFragment().apply {
      arguments = Bundle().apply {
        putSerializable("exception", throwable)
      }
    }
  }
}
