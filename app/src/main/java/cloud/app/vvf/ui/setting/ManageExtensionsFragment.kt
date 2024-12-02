package cloud.app.vvf.ui.setting

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.vvf.MainActivityViewModel.Companion.applyFabInsets
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentManageExtensionsBinding
import cloud.app.vvf.ui.extension.adapter.ExtensionAdapter
import cloud.app.vvf.ui.extension.ExtensionViewModel
import cloud.app.vvf.utils.EMULATOR
import cloud.app.vvf.utils.FastScrollerHelper
import cloud.app.vvf.utils.TV
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.configure
import cloud.app.vvf.utils.isLayout
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.utils.showNginxTextInputDialog
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Job

class ManageExtensionsFragment : Fragment() {
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
      binding.appBarLayout.setPadding(0, it.top, 0, 0)
      binding.recyclerView.setPadding(0, 0, 0, it.bottom)
      binding.fabContainer.applyFabInsets(it, systemInsets.value)
    }

    FastScrollerHelper.applyTo(binding.recyclerView)
    binding.swipeRefresh.configure { viewModel.refresh() }


    if (context?.isLayout(TV or EMULATOR) == true) {
      binding.toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
      }
    }

    binding.toolbar.apply {
      setNavigationIcon(R.drawable.ic_back)
      setNavigationOnClickListener {
        activity?.onBackPressedDispatcher?.onBackPressed()
      }

      setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menu_refresh -> {
            viewModel.refresh()
            true
          }

          else -> false
        }
      }
    }


    val extensionAdapter = ExtensionAdapter(object : ExtensionAdapter.Listener {
      override fun onClick(extension: Extension<*>, view: View) {
        navigate(ExtensionSettingFragment.newInstance(extension), view)
      }

      override fun onDragHandleTouched(viewHolder: ExtensionAdapter.ViewHolder) {
        // touchHelper.startDrag(viewHolder)
      }

      override fun onDelete(extension: Extension<*>) {
        TODO("Not yet implemented")
      }
    })

    binding.recyclerView.adapter = extensionAdapter.withEmptyAdapter()


    var type = ExtensionType.entries[binding.extTabLayout.selectedTabPosition]
    fun change(pos: Int): Job {
      type = ExtensionType.entries[pos]
      val flow = viewModel.getExtensionListFlow(type)
      return observe(flow) { list ->
        extensionAdapter.submit(list ?: emptyList())
      }
    }

    binding.extTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      var job: Job? = null
      override fun onTabSelected(tab: TabLayout.Tab) {
        job?.cancel()
        job = change(tab.position)
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {}
      override fun onTabReselected(tab: TabLayout.Tab) {}
    })

    observe(viewModel.refresher) {
      change(binding.extTabLayout.selectedTabPosition)
      binding.swipeRefresh.isRefreshing = it
    }

    binding.fabAddExtensions.setOnClickListener {
      //ExtensionsAddListBottomSheet.LinkFile().show(parentFragmentManager, null)
      activity?.showNginxTextInputDialog(
        getString(R.string.add_extensions), "https://gist.github.com/jonsnow32/6decd455453956cdcacfd74c7b2cbb13/raw/d80659811eeab174b6364b4b32f403872a7dd999/sample_extensions_repo.json",
        InputType.TYPE_TEXT_VARIATION_URI, {}) { url ->
        activity?.let {
          viewModel.addFromLinkOrCode(it, url);
        }
      }
    }
  }
}
