package cloud.app.vvf.ui.download

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.databinding.FragmentDownloadsBinding
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class DownloadsFragment : Fragment() {

  private var binding by autoCleared<FragmentDownloadsBinding>()

  private val viewModel: DownloadsViewModel by viewModels()
  private lateinit var downloadsAdapter: DownloadsAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentDownloadsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    applyInsets {
      binding.appBarLayout.setPadding(0, it.top, 0, 0)
      binding.rvDownloads.setPadding(0, 0, 0, it.bottom)
    }
    setupTransition(view)
    setupRecyclerView()
    setupClickListeners()
    observeDownloads()
    observeStorageInfo()
  }

  private fun setupRecyclerView() {
    downloadsAdapter = DownloadsAdapter { action: DownloadAction, downloadItem ->
      when (action) {
        DownloadAction.PAUSE -> viewModel.pauseDownload(downloadItem.id)
        DownloadAction.RESUME -> viewModel.resumeDownload(downloadItem.id)
        DownloadAction.CANCEL -> viewModel.cancelDownload(downloadItem.id)
        DownloadAction.RETRY -> viewModel.retryDownload(downloadItem.id)
        DownloadAction.REMOVE -> viewModel.removeDownload(downloadItem.id)
        DownloadAction.PLAY -> viewModel.playDownloadedFile(requireContext(), downloadItem)
      }
    }

    downloadsAdapter = DownloadsAdapter() { action: DownloadAction, downloadItem ->
      when (action) {
        DownloadAction.PAUSE -> viewModel.pauseDownload(downloadItem.id)
        DownloadAction.RESUME -> viewModel.resumeDownload(downloadItem.id)
        DownloadAction.CANCEL -> viewModel.cancelDownload(downloadItem.id)
        DownloadAction.RETRY -> viewModel.retryDownload(downloadItem.id)
        DownloadAction.REMOVE -> viewModel.removeDownload(downloadItem.id)
        DownloadAction.PLAY -> viewModel.playDownloadedFile(requireContext(), downloadItem)
      }
    }
    binding.rvDownloads.apply {
      layoutManager = LinearLayoutManager(requireContext())
      adapter = downloadsAdapter
    }
  }

  private fun setupClickListeners() {
//        binding.btnOpenFolder.setOnClickListener {
//            viewModel.openDownloadsFolder(requireContext())
//        }
//
//        binding.btnClearCompleted.setOnClickListener {
//            viewModel.clearCompletedDownloads()
//        }
  }

  private fun observeDownloads() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.downloads.collect { downloads ->
        if (downloads.isEmpty()) {
          showEmptyState()
        } else {
          showDownloadsList()
          // Force new list creation to ensure DiffUtil detects changes
          val sortedList = downloads.sortedByDescending { it.createdAt }.toList()
          downloadsAdapter.submitList(sortedList)

          // Log for debugging
          sortedList.forEach { download ->
            Timber.d("DownloadsFragment: Submitting download ${download.id} with status ${download.status}")
          }
        }

        // Refresh storage info when downloads change
        viewModel.refreshStorageInfo()
      }
    }
  }

  private fun showEmptyState() {
    binding.rvDownloads.visibility = View.GONE
    binding.layoutEmptyState.visibility = View.VISIBLE
  }

  private fun showDownloadsList() {
    binding.rvDownloads.visibility = View.VISIBLE
    binding.layoutEmptyState.visibility = View.GONE
  }

  /**
   * Observe storage information changes and update UI
   */
  private fun observeStorageInfo() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.storageInfo.collect { storageInfo ->
        updateStorageDisplay(storageInfo)
      }
    }
  }

  /**
   * Update storage display in the UI
   */
  private fun updateStorageDisplay(storageInfo: StorageInfo?) {
    if (storageInfo == null || storageInfo.totalBytes == 0L) {
      // Hide storage display if no storage info available
      binding.downloadStorageAppbar.visibility = View.GONE
      return
    }

    // Show storage display
    binding.downloadStorageAppbar.visibility = View.VISIBLE

    // Calculate weights for the progress bars based on percentages
    val usedWeight = (storageInfo.usedPercentage / 100f).coerceIn(0f, 1f)
    val appWeight = (storageInfo.appUsedPercentage / 100f).coerceIn(0f, 1f)
    val freeWeight = (storageInfo.freePercentage / 100f).coerceIn(0f, 1f)

    // Update progress bar weights
    val usedLayoutParams = binding.downloadUsed.layoutParams as android.widget.LinearLayout.LayoutParams
    usedLayoutParams.weight = usedWeight
    binding.downloadUsed.layoutParams = usedLayoutParams

    val appLayoutParams = binding.downloadApp.layoutParams as android.widget.LinearLayout.LayoutParams
    appLayoutParams.weight = appWeight
    binding.downloadApp.layoutParams = appLayoutParams

    val freeLayoutParams = binding.downloadFree.layoutParams as android.widget.LinearLayout.LayoutParams
    freeLayoutParams.weight = freeWeight
    binding.downloadFree.layoutParams = freeLayoutParams

    // Update text labels
    binding.downloadUsedTxt.text = getString(
      cloud.app.vvf.R.string.storage_used_format,
      StorageInfo.formatBytes(storageInfo.usedBytes)
    )

    binding.downloadAppTxt.text = getString(
      cloud.app.vvf.R.string.storage_app_format,
      StorageInfo.formatBytes(storageInfo.appUsedBytes)
    )

    binding.downloadFreeTxt.text = getString(
      cloud.app.vvf.R.string.storage_free_format,
      StorageInfo.formatBytes(storageInfo.freeBytes)
    )
  }

  /**
   * Manually refresh storage information
   */
  private fun refreshStorage() {
    viewModel.refreshStorageInfo()
  }

}
