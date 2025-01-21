package cloud.app.vvf.ui.main.search

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.MainActivityViewModel.Companion.applyContentInsets
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.databinding.FragmentSearchBinding
import cloud.app.vvf.ui.media.MediaContainerAdapter
import cloud.app.vvf.ui.media.MediaItemAdapter
import cloud.app.vvf.utils.Utils
import cloud.app.vvf.utils.applyAdapter
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.collect
import cloud.app.vvf.utils.firstVisible
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.Tab
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment(), MediaItemAdapter.Listener {
  private val parent get() = parentFragment as Fragment
  private var binding by autoCleared<FragmentSearchBinding>()
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
    applyInsets {
      binding.quickSearchViewHolder.applyContentInsets(it)
    }
    val viewModel by parent.viewModels<SearchViewModel>()


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

    val mediaContainerAdapter = MediaContainerAdapter(parentFragment as Fragment, 1, "search")
    val concatAdapter = mediaContainerAdapter.withLoaders()
    binding.recyclerView.adapter = concatAdapter
    collect(viewModel.databaseExtensionFlow) {
      binding.recyclerView.itemAnimator = null
      mediaContainerAdapter.clientId = it?.javaClass.toString()
      binding.recyclerView.applyAdapter<DatabaseClient>(it, id, concatAdapter)
    }

    observe(viewModel.feed) {
      binding.recyclerView.visibility = View.VISIBLE
      binding.tabLayout.visibility = View.VISIBLE
      binding.searchLoading.visibility = View.GONE
      mediaContainerAdapter.submit(it)
    }



    val quickSearchAdapter = QuickSearchAdapter(object : QuickSearchAdapter.Listener {
      override fun onClick(item: SearchItem, transitionView: View) = binding.mainSearch.setQuery(item.query, true)

      override fun onLongClick(item: SearchItem, transitionView: View) = run {
        onClick(item, transitionView)
        true
      }

      override fun onDelete(item: SearchItem) {
        viewModel.deleteHistory(item)
        viewModel.getHistory()
      }
    })

    binding.quickSearchRecyclerView.adapter = quickSearchAdapter

    observe(viewModel.historyQuery) {
      binding.apply {
        quickSearchViewHolder.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tabLayout.visibility = View.GONE
      }
      quickSearchAdapter.submitList(it)
    }

    binding.mainSearch.apply {
      val searchManager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
      setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))
      setOnSuggestionListener(object : SearchView.OnSuggestionListener {
        override fun onSuggestionSelect(position: Int): Boolean {
          return false
        }

        @SuppressLint("Range")
        override fun onSuggestionClick(position: Int): Boolean {
          val cursor = suggestionsAdapter.getItem(position) as Cursor
          val selection =
            cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))
          setQuery(selection, false)
          return false
        }
      })

      setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean {

          if (query.isNotEmpty()) {
            viewModel.query = query
            viewModel.refresh()
            viewModel.saveHistory()
            Utils.hideKeyboard(binding.mainSearch)
          }


          return true
        }

        private var searchJob: Job? = null
        override fun onQueryTextChange(newText: String): Boolean {
          if (newText.isNotEmpty()) {
            viewModel.query = newText

            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
              delay(300) // Adjust the delay as needed
              viewModel.refresh()
            }
          } else {
            viewModel.getHistory()
          }
          return true
        }
      })
    }


    viewModel.query?.isNotBlank().let {
      binding.mainSearch.setQuery(viewModel.query, true)
    }

  }

  override fun onStop() {
    val viewModel by parent.viewModels<SearchViewModel>()
    viewModel.recyclerPosition = binding.recyclerView.firstVisible()
    super.onStop()
  }

  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {

    TODO("Not yet implemented")
  }

  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }

  override fun onFocusChange(clientId: String?, item: AVPMediaItem, hasFocus: Boolean) {

  }

}

