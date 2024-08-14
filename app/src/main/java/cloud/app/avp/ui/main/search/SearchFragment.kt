package cloud.app.avp.ui.main.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cloud.app.avp.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.avp.databinding.FragmentSearchBinding
import cloud.app.avp.ui.media.MediaContainerAdapter
import cloud.app.avp.ui.media.MediaItemAdapter
import cloud.app.avp.utils.applyAdapter
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.collect
import cloud.app.avp.utils.first
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.setupTransition
import cloud.app.common.clients.mvdatabase.SearchClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.Tab
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment(), MediaItemAdapter.Listener {
  private val parent get() = parentFragment as Fragment
  private var binding by autoCleared<FragmentSearchBinding>()
  val viewModel by viewModels<SearchViewModel>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentSearchBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.searchBar, binding.recyclerView)

    val tabListener = object : TabLayout.OnTabSelectedListener {
      var enabled = true
      var tabs: List<Tab> = emptyList()
      override fun onTabSelected(tab: TabLayout.Tab) {
        if (!enabled) return
        val genre = tabs[tab.position]
        if (viewModel.tab == genre) return
        viewModel.tab = genre
        viewModel.refresh()
      }

      override fun onTabUnselected(tab: TabLayout.Tab) = Unit
      override fun onTabReselected(tab: TabLayout.Tab) = Unit
    }
    binding.tabLayout.addOnTabSelectedListener(tabListener)


    viewModel.initialize()

    observe(viewModel.loading) {
      tabListener.enabled = !it
    }

    observe(viewModel.tabs) { genres ->
      binding.tabLayout.removeAllTabs()
      tabListener.tabs = genres
      binding.tabLayout.isVisible = genres.isNotEmpty()
      genres.forEach { genre ->
        val tab = binding.tabLayout.newTab()
        tab.text = genre.name
        val selected = viewModel.tab?.id == genre.id
        binding.tabLayout.addTab(tab, selected)
      }
    }

    val mediaContainerAdapter = MediaContainerAdapter(parentFragment as Fragment, "search")
    val concatAdapter = mediaContainerAdapter.withLoaders()
    binding.recyclerView.adapter = concatAdapter
    collect(viewModel.extensionFlow) {
      binding.recyclerView.itemAnimator = null
      mediaContainerAdapter.clientId = it?.hashCode().toString()
      binding.recyclerView.applyAdapter<SearchClient>(it, id, concatAdapter)
    }

    observe(viewModel.feed) {
      binding.recyclerView.visibility = View.VISIBLE
      mediaContainerAdapter.submit(it)
    }
  }

  override fun onStop() {
    val viewModel by parent.viewModels<SearchViewModel>()
    viewModel.recyclerPosition = binding.recyclerView.first()
    super.onStop()
  }

  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {
    TODO("Not yet implemented")
  }

  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }
}

