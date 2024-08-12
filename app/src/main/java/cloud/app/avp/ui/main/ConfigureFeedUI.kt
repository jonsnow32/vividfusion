package cloud.app.avp.ui.main

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cloud.app.avp.ui.media.MediaContainerAdapter
import cloud.app.avp.utils.applyAdapter
import cloud.app.avp.utils.collect
import cloud.app.avp.utils.configure
import cloud.app.avp.utils.observe
import cloud.app.common.clients.infos.FeedClient
import cloud.app.common.models.Tab
import com.google.android.material.tabs.TabLayout

fun Fragment.configureFeedUI(
  id: Int,
  viewModel: FeedViewModel,
  recyclerView: RecyclerView,
  swipeRefresh: SwipeRefreshLayout,
  tabLayout: TabLayout,
  listener: MediaContainerAdapter.Listener? = null
): MediaContainerAdapter {

  val parent = parentFragment as Fragment

  swipeRefresh.configure {
    viewModel.refresh(true)
  }

  val mediaContainerAdapter = if (listener == null)
    MediaContainerAdapter(parent, id.toString())
  else MediaContainerAdapter(parent, id.toString(), listener)

  if (listener == null) {
    val concatAdapter = mediaContainerAdapter.withLoaders()
    recyclerView.adapter = concatAdapter
    collect(viewModel.extensionFlow) {
      recyclerView.itemAnimator = null
      swipeRefresh.isEnabled = it != null
      mediaContainerAdapter.clientId = it?.hashCode().toString();// it?.metadata?.id
      recyclerView.applyAdapter<FeedClient>(it, id, concatAdapter)
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
      viewModel.refresh()
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
    tabLayout.isVisible = genres.isNotEmpty()
    genres.forEach { genre ->
      val tab = tabLayout.newTab()
      tab.text = genre.name
      val selected = viewModel.tab?.id == genre.id
      tabLayout.addTab(tab, selected)
    }
  }


  observe(viewModel.feed) {
    mediaContainerAdapter.submit(it)
  }

  return mediaContainerAdapter
}
