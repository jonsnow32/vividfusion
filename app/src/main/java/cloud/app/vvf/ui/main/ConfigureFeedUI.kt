package cloud.app.vvf.ui.main

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.ui.media.MediaContainerAdapter
import cloud.app.vvf.utils.FastScrollerHelper
import cloud.app.vvf.utils.applyExtAdapter
import cloud.app.vvf.utils.collect
import cloud.app.vvf.utils.configure
import cloud.app.vvf.utils.observe
import cloud.app.vvf.common.models.Tab
import cloud.app.vvf.ui.main.home.ClientNotFoundAdapter
import com.google.android.material.tabs.TabLayout

suspend inline fun <reified T> Fragment.applyClient(
  recyclerView: RecyclerView,
  swipeRefresh: SwipeRefreshLayout,
  id: Int,
  extension: Extension<*>?
): MediaContainerAdapter? {
  swipeRefresh.isEnabled = extension != null

  if(extension != null) {
    val parent = parentFragment as Fragment
    val adapter = MediaContainerAdapter(
      extension,
      parent,
      recyclerView.context.getString(id),
      id.toString(),
    )
    val concatAdapter = adapter.withLoaders()
    recyclerView.applyExtAdapter<T>(extension, id, concatAdapter, parent)
    return adapter
  } else {
    recyclerView.applyExtAdapter<T>(null, id, ClientNotFoundAdapter(parentFragment), parentFragment)
    return null
  }
}

inline fun <reified T> Fragment.configureFeedUI(
  id: Int,
  viewModel: FeedViewModel,
  recyclerView: RecyclerView,
  swipeRefresh: SwipeRefreshLayout,
  tabLayout: TabLayout,
  clientId: String? = null,
)  {

  val parent = parentFragment as Fragment

  FastScrollerHelper.applyTo(recyclerView)

  swipeRefresh.configure {
    viewModel.refresh(true)
  }

  var mediaContainerAdapter: MediaContainerAdapter? = null

  if (clientId == null) collect(viewModel.dbExtFlow) {
    val adapter = applyClient<T>(recyclerView, swipeRefresh, id, it)
    mediaContainerAdapter = adapter
  } else collect(viewModel.extListFlow){
   val extension = viewModel.extListFlow.value?.find { it.id == clientId && it.types.contains(ExtensionType.DATABASE)}
    val adapter = applyClient<T>(recyclerView, swipeRefresh, id, extension)
    mediaContainerAdapter = adapter
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
