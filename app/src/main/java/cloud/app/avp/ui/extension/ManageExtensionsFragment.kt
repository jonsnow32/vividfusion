package cloud.app.avp.ui.extension

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.avp.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.avp.databinding.FragmentManageExtensionsBinding
import cloud.app.avp.utils.autoCleared
import com.google.android.material.tabs.TabLayout
import cloud.app.avp.R
import cloud.app.avp.utils.FastScrollerHelper
import cloud.app.avp.utils.configure
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.onAppBarChangeListener
import cloud.app.avp.utils.setupTransition
import cloud.app.common.clients.infos.FeedClient
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.clients.subtitles.SubtitleClient
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

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
    applyInsetsMain(binding.appBarLayout, binding.recyclerView)

    binding.appBarLayout.onAppBarChangeListener { offset ->
      binding.appBarOutline.alpha = offset
      binding.appBarOutline.isVisible = offset > 0
      binding.toolBar.alpha = 1 - offset
    }
    binding.toolBar.setNavigationOnClickListener {
      parentFragmentManager.popBackStack()
    }
    FastScrollerHelper.applyTo(binding.recyclerView)
    val refresh = binding.toolBar.findViewById<View>(R.id.menu_refresh)
    refresh.setOnClickListener { viewModel.refresh() }
    binding.swipeRefresh.configure { viewModel.refresh() }


    val flow = MutableStateFlow(
      viewModel.extensionFlowList.value
    )

    fun change(pos: Int) {
      when (pos) {
        0 -> flow.value = viewModel.extensionFlowList.value
        1 -> flow.value = viewModel.extensionFlowList.value.filter { it is FeedClient }
        2 -> flow.value = viewModel.extensionFlowList.value.filter { it is StreamClient }
        3 -> flow.value = viewModel.extensionFlowList.value.filter { it is SubtitleClient }
      }
    }

    val extensionAdapter = ExtensionAdapter { extension, view ->
      navigate(R.id.extensionInfoFragment, view, bundleOf("extensionMetadata" to extension.metadata, "extensionClassName" to extension.javaClass))
    }
    binding.recyclerView.adapter = extensionAdapter.withEmptyAdapter()

    observe(flow) { extensionAdapter.submit(it ?: emptyList()) }

    binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        change(tab.position)
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {}
      override fun onTabReselected(tab: TabLayout.Tab) {}
    })

  }
}
