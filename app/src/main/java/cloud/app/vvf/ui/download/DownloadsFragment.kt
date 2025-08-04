package cloud.app.vvf.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cloud.app.vvf.databinding.FragmentDownloadsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DownloadsViewModel by viewModels()
    private lateinit var downloadsAdapter: DownloadsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeDownloads()
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

        binding.rvDownloads.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnOpenFolder.setOnClickListener {
            viewModel.openDownloadsFolder(requireContext())
        }

        binding.btnClearCompleted.setOnClickListener {
            viewModel.clearCompletedDownloads()
        }
    }

    private fun observeDownloads() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloads.collect { downloads ->
                if (downloads.isEmpty()) {
                    showEmptyState()
                } else {
                    showDownloadsList()
                    downloadsAdapter.submitList(downloads)
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
