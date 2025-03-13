package cloud.app.vvf.ui.main.search

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.MainActivityViewModel.Companion.applyContentInsets
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentSearchBinding
import cloud.app.vvf.ui.media.MediaItemAdapter
import cloud.app.vvf.utils.*
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.ui.detail.loadWith
import cloud.app.vvf.ui.extension.ExtensionViewModel
import cloud.app.vvf.ui.main.configureFeedUI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SearchFragment : Fragment(){

  private val parent get() = parentFragment as Fragment
  private var binding by autoCleared<FragmentSearchBinding>()
  private val viewModel: SearchViewModel by lazy {
    parent.viewModels<SearchViewModel>().value
  }
  private val extensionViewModel by activityViewModels<ExtensionViewModel>()

  private var searchJob: Job? = null
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentSearchBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupUI()
    setupObservers()
    configureSearchView()
  }

  private fun setupUI() {
    setupTransition(binding.root)
    applyInsetsMain(binding.searchBar, binding.recyclerView)
    applyInsets { binding.quickSearchViewHolder.applyContentInsets(it) }

    configureFeedUI<DatabaseClient>(
      R.string.home, viewModel, binding.recyclerView,
      binding.swipeRefresh, binding.tabLayout
    )

    val quickSearchAdapter = QuickSearchAdapter(object : QuickSearchAdapter.Listener {
      override fun onClick(item: SearchItem, transitionView: View) {
        binding.mainSearch.setQuery(item.query, true)
      }

      override fun onLongClick(item: SearchItem, transitionView: View): Boolean {
        onClick(item, transitionView)
        return true
      }

      override fun onDelete(item: SearchItem) {
        viewModel.deleteHistory(item)
        viewModel.getHistory()
      }
    })

    binding.quickSearchRecyclerView.adapter = quickSearchAdapter
  }

  private fun setupObservers() {
    observe(viewModel.loading) {
      binding.searchLoading.isGone = !it
      binding.mainSearch.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        .isGone = it
    }

    observe(viewModel.feed) {
      binding.searchLoading.isGone = true
    }

    observe(viewModel.historyQuery) { history ->
      (binding.quickSearchRecyclerView.adapter as? QuickSearchAdapter)?.submitList(history)
    }

    observe(extensionViewModel.selectedExtension) { extension ->
      binding.searchExtension.loadWith(extension?.metadata?.iconUrl?.toImageHolder())
    }

  }

  private fun configureSearchView() {
    binding.mainSearch.apply {
      val searchManager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
      setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))

      setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean = true

        override fun onQueryTextChange(newText: String): Boolean {
          handleQueryTextChange(newText)
          return true
        }
      })

      setOnSuggestionListener(object : SearchView.OnSuggestionListener {
        override fun onSuggestionSelect(position: Int): Boolean = false

        @SuppressLint("Range")
        override fun onSuggestionClick(position: Int): Boolean {
          val cursor = suggestionsAdapter.getItem(position) as Cursor
          val selection =
            cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))
          setQuery(selection, false)
          return false
        }
      })

      setQuery(viewModel.query, false)
    }
  }

  private fun handleQueryTextChange(newText: String) {
    searchJob?.cancel()
    if (newText.isNotBlank()) {
      viewModel.query = newText.trim()
      searchJob = lifecycleScope.launch {
        delay(300) // Debounce delay
        viewModel.refresh(extensionViewModel.selectedExtension.value)
        toggleQuickSearchVisibility(false)
      }
    } else {
      toggleQuickSearchVisibility(true)
      viewModel.getHistory()
    }
  }

  private fun toggleQuickSearchVisibility(show: Boolean) {
    binding.quickSearchRecyclerView.isGone = !show
    binding.swipeRefresh.isGone = show
    binding.tabLayout.isGone = show
  }

  override fun onPause() {
    viewModel.saveHistory()
    super.onPause()
  }

  override fun onStop() {
    viewModel.recyclerPosition = binding.recyclerView.firstVisible()
    super.onStop()
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (hidden) {
      // Fragment is hidden
      viewModel.saveHistory()
      Timber.d("Fragment ${this::class.java.simpleName} is hidden")
    } else {
      // Fragment is shown again
      Timber.d("Fragment ${this::class.java.simpleName} is visible")
    }
  }
}
