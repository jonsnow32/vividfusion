package cloud.app.vvf.ui.main.files

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.databinding.FragmentBrowseBinding
import cloud.app.vvf.features.player.PlayerFragment
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FilesFragment : Fragment() {

  private var binding by autoCleared<FragmentBrowseBinding>()
  private val viewModel by viewModels<FilesViewModel>()
  private val parent by lazy { parentFragment as Fragment }
  private lateinit var filesAdapter: FilesAdapter

  private var permissionDeniedPermanently = false

  private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    if (grants.values.any { it }) {
      startCollecting()
    } else {
      permissionDeniedPermanently = !shouldShowRationale()
      showPermissionDenied()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentBrowseBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.appBarLayoutCustom, binding.recyclerView)

    binding.backBtn.isVisible = false
    binding.title.text = getString(R.string.files)

    setupAdapter()
    setupFilterButton()
    setupSearchBar()

    binding.swipeRefresh.setOnRefreshListener { filesAdapter.refresh() }

    checkPermissionAndLoad()
  }

  private fun setupAdapter() {
    filesAdapter = FilesAdapter(
      onVideoClick = { playVideo(it) },
      onAudioClick = { playAudio(it) },
    )
    val spanCount = 3
    val layoutManager = GridLayoutManager(requireContext(), spanCount)
    layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
      override fun getSpanSize(position: Int) =
        if (filesAdapter.getItemViewType(position) == FilesAdapter.VIEW_TYPE_AUDIO) spanCount else 1
    }
    binding.recyclerView.layoutManager = layoutManager
    binding.recyclerView.adapter = filesAdapter
    filesAdapter.addLoadStateListener { handleLoadStates(it) }
  }

  private fun setupFilterButton() {
    binding.filter.isVisible = true
    binding.searchBtn.isVisible = true

    observe(viewModel.filter) { active ->
      val colorRes = if (active != MediaFilter.ALL)
        R.color.md_theme_primary
      else
        android.R.color.darker_gray
      binding.filter.setColorFilter(ContextCompat.getColor(requireContext(), colorRes))
    }

    binding.filter.setOnClickListener {
      val popup = PopupMenu(requireContext(), binding.filter)
      popup.menuInflater.inflate(R.menu.menu_files_filter, popup.menu)

      val active = viewModel.filter.value
      popup.menu.findItem(R.id.filter_all).isChecked = active == MediaFilter.ALL
      popup.menu.findItem(R.id.filter_video).isChecked = active == MediaFilter.VIDEO
      popup.menu.findItem(R.id.filter_audio).isChecked = active == MediaFilter.AUDIO

      popup.setOnMenuItemClickListener { item ->
        val newFilter = when (item.itemId) {
          R.id.filter_video -> MediaFilter.VIDEO
          R.id.filter_audio -> MediaFilter.AUDIO
          else -> MediaFilter.ALL
        }
        viewModel.setFilter(newFilter)
        true
      }
      popup.show()
    }
  }

  private fun setupSearchBar() {
    binding.searchBtn.setOnClickListener { toggleSearchBar(open = true) }

    binding.searchClear.setOnClickListener {
      if (binding.searchEdit.text.isNotEmpty()) {
        binding.searchEdit.text.clear()
      } else {
        toggleSearchBar(open = false)
      }
    }

    binding.searchEdit.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
      override fun afterTextChanged(s: Editable?) {
        val q = s?.toString() ?: ""
        binding.searchClear.isVisible = q.isNotEmpty()
        viewModel.setQuery(q)
      }
    })

    binding.searchEdit.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        hideKeyboard()
        true
      } else false
    }
  }

  private fun toggleSearchBar(open: Boolean) {
    binding.searchBar.isVisible = open
    if (open) {
      binding.searchEdit.requestFocus()
      showKeyboard(binding.searchEdit)
    } else {
      binding.searchEdit.text.clear()
      viewModel.setQuery("")
      hideKeyboard()
    }
  }

  private fun showKeyboard(view: View) {
    val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
    imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
  }

  private fun hideKeyboard() {
    val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
    imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
  }

  private fun checkPermissionAndLoad() {
    val perms = requiredPermissions()
    val allGranted = perms.all {
      ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }
    if (allGranted) startCollecting() else permissionLauncher.launch(perms)
  }

  private fun startCollecting() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.files.collectLatest { filesAdapter.submitData(it) }
      }
    }
  }

  private fun handleLoadStates(states: CombinedLoadStates) {
    val refresh = states.refresh
    binding.swipeRefresh.isRefreshing = refresh is LoadState.Loading
    when {
      refresh is LoadState.Error -> showEmptyState(
        refresh.error.localizedMessage ?: getString(R.string.error_loading_files)
      )
      refresh is LoadState.NotLoading && filesAdapter.itemCount == 0 ->
        showEmptyState(getString(R.string.no_files_found))
      refresh is LoadState.NotLoading -> {
        binding.emptyState.isVisible = false
        binding.recyclerView.isVisible = true
      }
    }
  }

  private fun showEmptyState(message: String) {
    binding.swipeRefresh.isRefreshing = false
    binding.emptyState.isVisible = true
    binding.recyclerView.isVisible = false
    binding.emptyStateText.text = message
    binding.emptyStateAction.isVisible = false
  }

  private fun showPermissionDenied() {
    binding.swipeRefresh.isRefreshing = false
    binding.emptyState.isVisible = true
    binding.recyclerView.isVisible = false
    binding.emptyStateText.text = getString(R.string.storage_permission_required)
    binding.emptyStateAction.isVisible = true
    if (permissionDeniedPermanently) {
      binding.emptyStateAction.text = getString(R.string.open_settings)
      binding.emptyStateAction.setOnClickListener { openAppSettings() }
    } else {
      binding.emptyStateAction.text = getString(R.string.grant_permission)
      binding.emptyStateAction.setOnClickListener { checkPermissionAndLoad() }
    }
  }

  private fun shouldShowRationale(): Boolean {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
      Manifest.permission.READ_MEDIA_VIDEO
    else
      Manifest.permission.READ_EXTERNAL_STORAGE
    return shouldShowRequestPermissionRationale(perm)
  }

  private fun openAppSettings() {
    startActivity(
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", requireContext().packageName, null)
      }
    )
  }

  @OptIn(UnstableApi::class)
  private fun playVideo(item: AVPMediaItem.VideoItem) {
    parent.navigate(PlayerFragment.newInstance(mediaItems = listOf(item), selectedMediaIdx = 0))
  }

  @OptIn(UnstableApi::class)
  private fun playAudio(item: AVPMediaItem.TrackItem) {
    parent.navigate(PlayerFragment.newInstance(mediaItems = listOf(item), selectedMediaIdx = 0))
  }

  private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
      arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
  }
}
