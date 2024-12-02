package cloud.app.vvf.ui.extension

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentExtensionRepoBinding
import cloud.app.vvf.extension.ExtensionAssetResponse
import cloud.app.vvf.ui.extension.adapter.ExtensionsRepoAdapter
import cloud.app.vvf.ui.extension.widget.InstallStatus
import cloud.app.vvf.utils.EMULATOR
import cloud.app.vvf.utils.FastScrollerHelper
import cloud.app.vvf.utils.TV
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.configure
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.isLayout
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class ExtensionRepoFragment : Fragment(), ExtensionsRepoAdapter.Listener {
  companion object {
    fun newInstance(name: String, list: List<ExtensionAssetResponse>) =
      ExtensionRepoFragment().apply {
        arguments = Bundle().apply {
          putSerialized("list", list)
          putString("repoName", name)
        }
      }
  }

  private var binding by autoCleared<FragmentExtensionRepoBinding>()
  private val args by lazy { requireArguments() }
  val list by lazy { args.getSerialized<List<ExtensionAssetResponse>>("list")!! }
  val repoName by lazy { args.getString("repoName") }

  override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
    binding = FragmentExtensionRepoBinding.inflate(inflater, parent, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupTransition(view)
    applyInsets {
      binding.appBarLayout.setPadding(0, it.top, 0, 0)
      binding.recyclerView.setPadding(0, 0, 0, it.bottom)
    }

    if (context?.isLayout(TV or EMULATOR) == true) {
      binding.toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
      }
    }

    binding.toolbar.apply {
      title = repoName
      setNavigationIcon(R.drawable.ic_back)
      setNavigationOnClickListener {
        activity?.onBackPressedDispatcher?.onBackPressed()
      }
    }

    fun load() {
      val extensionViewModel by activityViewModels<ExtensionViewModel>()
      lifecycleScope.launch {
        val installed = extensionViewModel.allExtensions().map { it.id }
        val extensionListAdapter = ExtensionsRepoAdapter(list.map {
          val isInstalled = it.id in installed
          ExtensionsRepoAdapter.Item(
            it,
            if (isInstalled) InstallStatus.INSTALLED else InstallStatus.NOT_INSTALL
          )
        }, this@ExtensionRepoFragment)
        binding.recyclerView.adapter = extensionListAdapter
        binding.swipeRefresh.isRefreshing = false
      }
    }
    FastScrollerHelper.applyTo(binding.recyclerView)
    binding.swipeRefresh.configure { load() }
    load()

  }

  suspend fun downloadExtension(item: ExtensionAssetResponse) = suspendCoroutine { cont ->
    cont.resume(Unit)
  }

  override fun onItemClicked(item: ExtensionsRepoAdapter.Item) {
    when (item.status) {
      InstallStatus.CANCELED,
      InstallStatus.NOT_INSTALL -> {
        val extensionViewModel by activityViewModels<ExtensionViewModel>()
        extensionViewModel.viewModelScope.launch {
          extensionViewModel.addExtension(this@ExtensionRepoFragment.requireActivity(), item.data)
            .collect {
              item.status = it
              (binding.recyclerView.adapter as ExtensionsRepoAdapter).updateItem(item)
            }
        }
      }

      InstallStatus.INSTALLING,
      InstallStatus.DOWNLOADING -> {
        createSnack(getString(R.string.nothing_to_show))
      }

      InstallStatus.FAILED -> TODO()
      InstallStatus.PAUSED -> TODO()
      InstallStatus.INSTALLED -> TODO()
    }
  }

}
