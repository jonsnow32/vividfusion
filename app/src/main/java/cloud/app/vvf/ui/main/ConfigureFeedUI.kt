package cloud.app.vvf.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.Tab
import cloud.app.vvf.ui.extension.ExtensionViewModel
import cloud.app.vvf.ui.extension.adapter.ExtensionNotSupportedAdapter
import cloud.app.vvf.ui.extension.adapter.ExtensionLoadingAdapter
import cloud.app.vvf.ui.extension.adapter.ExtensionEmptyAdapter
import cloud.app.vvf.ui.extension.adapter.ExtensionUnselected
import cloud.app.vvf.ui.media.MediaContainerAdapter
import cloud.app.vvf.utils.FastScrollerHelper
import cloud.app.vvf.utils.configure
import cloud.app.vvf.utils.observe
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

suspend inline fun <reified T> Fragment.applyClient(
  extList: List<Extension<*>>?,
  selectedExt: Extension<*>?,
  recyclerView: RecyclerView,
  swipeRefresh: SwipeRefreshLayout,
  id: Int,
): MediaContainerAdapter? {
  swipeRefresh.isEnabled = selectedExt != null

  if (extList == null) { //loading
    recyclerView.adapter = ExtensionLoadingAdapter()
  } else { //loaded
    if (extList.isEmpty()) {
      recyclerView.adapter = ExtensionEmptyAdapter(parentFragment)
    } else {
      if (selectedExt == null) {
        recyclerView.adapter = ExtensionUnselected(parentFragment)
      } else if (selectedExt.instance.value.getOrNull() !is T) {
        recyclerView.adapter = ExtensionNotSupportedAdapter(selectedExt, T::class.java.toString())
      } else
        return MediaContainerAdapter(
          selectedExt,
          parentFragment as Fragment,
          recyclerView.context.getString(id),
          id.toString(),
        ).also { recyclerView.adapter = it.withLoaders() }

    }
  }
  return null
}


inline fun <reified T> Fragment.configureFeedUI(
  id: Int,
  viewModel: FeedViewModel,
  recyclerView: RecyclerView,
  swipeRefresh: SwipeRefreshLayout,
  tabLayout: TabLayout,
) {

  val parent = parentFragment as Fragment

  FastScrollerHelper.applyTo(recyclerView)
  swipeRefresh.configure {
    viewModel.refresh(viewModel.selectedExtension.value, true)
  }

  var mediaContainerAdapter: MediaContainerAdapter? = null
  lifecycleScope.launch {
    val extensionViewModel by activityViewModels<ExtensionViewModel>()
    combine(
      viewModel.selectedExtension,
      extensionViewModel.extListFlow
    ) { selectedExtension, list ->
      applyClient<T>(
        list, selectedExtension,
        recyclerView,
        swipeRefresh,
        id,
      )
    }.collectLatest { value ->
      mediaContainerAdapter = value
    }
  }

  val tabListener = object : TabLayout.OnTabSelectedListener {
    var enabled = true
    var tabs: List<Tab> = emptyList()
    override fun onTabSelected(tab: TabLayout.Tab) {
      if (!enabled) return
      val genre = tabs[tab.position]
      if (viewModel.tab == genre) return
      viewModel.tab = genre
      viewModel.refresh(viewModel.selectedExtension.value)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit
    override fun onTabReselected(tab: TabLayout.Tab) = Unit
  }
  tabLayout.addOnTabSelectedListener(tabListener)

  observe(viewModel.loading) {
    tabListener.enabled = !it
    swipeRefresh.isRefreshing = it
  }

  observe(viewModel.tabs) { genres ->
    tabLayout.removeAllTabs()
    tabListener.tabs = genres
    genres.forEach { genre ->
      val tab = tabLayout.newTab()
      tab.text = genre.name
      val selected = viewModel.tab?.id == genre.id
      tabLayout.addTab(tab, selected)
    }
  }

  observe(viewModel.feed) {
    mediaContainerAdapter?.submit(it)
  }
}
