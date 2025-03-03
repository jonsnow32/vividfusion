package cloud.app.vvf.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.databinding.FragmentManageExtensionsBinding
import cloud.app.vvf.ui.extension.ExtensionViewModel
import cloud.app.vvf.ui.extension.adapter.ExtensionAdapter
import cloud.app.vvf.ui.widget.dialog.InputDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import kotlinx.coroutines.launch

class ManageExtensionsFragment : BaseSettingsFragment() {
  override val title: String?
    get() = getString(R.string.manage_extensions)
  override val transitionName: String?
    get() = "manage_extensions"
  override val container = { ExtensionListFragment() }


  class ExtensionListFragment : Fragment() {
    var binding by autoCleared<FragmentManageExtensionsBinding>()
    val viewModel by activityViewModels<ExtensionViewModel>()

    override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
      binding = FragmentManageExtensionsBinding.inflate(inflater, container, false)
      return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      setupTransition(view)
      applyInsets {
        //binding.fabContainer.applyFabInsets(it, systemInsets.value)
      }


      val extensionAdapter = ExtensionAdapter(object : ExtensionAdapter.Listener {
        override fun onClick(extension: Extension<*>, view: View) {
          parentFragment?.navigate(ExtensionSettingFragment.newInstance(extension.id, extension.name), view)
        }

        override fun onDragHandleTouched(viewHolder: ExtensionAdapter.ViewHolder) {
          // touchHelper.startDrag(viewHolder)
        }

        override fun onDelete(extension: Extension<*>) {
          viewModel.uninstall(requireActivity(), extension) {
//          if (it) createSnack(getString(R.string.extension_uninstalled_successfully))
//          else createSnack(getString(R.string.extension_uninstalled_fail))
            if (it) viewModel.refresh()
          }
        }
      })

      binding.recyclerView.adapter = extensionAdapter.withEmptyAdapter()


      observe(viewModel.extensionListFlow) { list ->
          lifecycleScope.launch {
            extensionAdapter.submit(list)
          }

      }

      observe(viewModel.refresher) {
        binding.swipeRefresh.isRefreshing = it
      }

      binding.fabAddExtensions.setOnClickListener {
        //ExtensionsAddListBottomSheet.LinkFile().show(parentFragmentManager, null)

        val inputDialog = InputDialog.newInstance(
          getString(R.string.add_extensions),
          "https://github.com/jonsnow32/vivid-sample-extension/releases/download/1ba398f/plugins.json"
        ) { url ->
          activity?.let {
            viewModel.addFromLinkOrCode(it, url);
          }
        }

        inputDialog.show(parentFragmentManager, "add extension")
//      activity?.showNginxTextInputDialog(
//        getString(R.string.add_extensions),
//        "https://github.com/jonsnow32/vivid-sample-extension/releases/download/1ba398f/plugins.json",
//        InputType.TYPE_TEXT_VARIATION_URI,
//        {}) { url ->
//        activity?.let {
//          viewModel.addFromLinkOrCode(it, url);
//        }
//      }
      }
    }
  }

}
