package cloud.app.avp.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentManageExtensionsBinding
import cloud.app.avp.ui.extension.ExtensionAdapter
import cloud.app.avp.ui.extension.ExtensionViewModel
import cloud.app.avp.utils.EMULATOR
import cloud.app.avp.utils.FastScrollerHelper
import cloud.app.avp.utils.TV
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.configure
import cloud.app.avp.utils.isLayout
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.setupTransition
import cloud.app.common.clients.Extension
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.MutableStateFlow

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
    })

    binding.recyclerView.adapter = extensionAdapter.withEmptyAdapter()

    val flow = MutableStateFlow<List<Extension<*>>>(emptyList())
    fun change(pos: Int) {
      when (pos) {
        0 -> flow.value = viewModel.databaseExtensionListFlow.value
        1 -> flow.value = viewModel.streamExtensionListFlow.value
        2 -> flow.value = viewModel.streamExtensionListFlow.value
      }
    }

    observe(flow) { extensionAdapter.submit(it) }
    change(binding.extTabLayout.selectedTabPosition)

    binding.extTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        change(tab.position)
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {}
      override fun onTabReselected(tab: TabLayout.Tab) {}
    })

//    observe(viewModel.extensionFlowList) { extensions ->
//      change(binding.extTabLayout.selectedTabPosition)
//    }
  }

}
