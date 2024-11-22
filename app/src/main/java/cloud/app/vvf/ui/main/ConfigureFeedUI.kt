package cloud.app.vvf.ui.main

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cloud.app.vvf.ui.media.MediaContainerAdapter
import cloud.app.vvf.utils.FastScrollerHelper
import cloud.app.vvf.utils.applyAdapter
import cloud.app.vvf.utils.collect
import cloud.app.vvf.utils.configure
import cloud.app.vvf.utils.observe
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.Tab
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

  FastScrollerHelper.applyTo(recyclerView)
  swipeRefresh.configure {
    viewModel.refresh(true)
  }

  val mediaContainerAdapter = if (listener == null)
    MediaContainerAdapter(parent, id,  id.toString())
  else MediaContainerAdapter(parent, id,  id.toString(), listener)

  if (listener == null) {
    val concatAdapter = mediaContainerAdapter.withLoaders()
    recyclerView.adapter = concatAdapter
    collect(viewModel.databaseExtensionFlow) {
      recyclerView.itemAnimator = null
      swipeRefresh.isEnabled = it != null
      mediaContainerAdapter.clientId = it?.hashCode().toString();// it?.metadata?.id
      recyclerView.applyAdapter<DatabaseClient>(it, id, concatAdapter)
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
