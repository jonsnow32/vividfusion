package cloud.app.vvf.ui.main.files

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.databinding.FragmentBrowseBinding
import cloud.app.vvf.features.player.PlayerFragment
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilesFragment : Fragment() {

  private var binding by autoCleared<FragmentBrowseBinding>()
  private val viewModel by viewModels<FilesViewModel>()
  private val parent by lazy { parentFragment as Fragment }

  private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    if (grants.values.any { it }) loadFiles()
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
    binding.filter.isVisible = false

    val adapter = FilesAdapter(emptyList()) { item -> playFile(item) }
    binding.recyclerView.adapter = adapter
    binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

    binding.swipeRefresh.setOnRefreshListener { checkPermissionAndLoad() }

    observe(viewModel.files) { files ->
      adapter.submitList(files)
      binding.swipeRefresh.isRefreshing = false
    }

    observe(viewModel.isLoading) { loading ->
      if (loading) binding.swipeRefresh.isRefreshing = true
    }

    checkPermissionAndLoad()
  }

  private fun checkPermissionAndLoad() {
    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
      arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val allGranted = perms.all {
      ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }
    if (allGranted) loadFiles() else permissionLauncher.launch(perms)
  }

  private fun loadFiles() {
    viewModel.loadFiles(requireContext())
  }

  @OptIn(UnstableApi::class)
  private fun playFile(item: AVPMediaItem.VideoItem) {
    parent.navigate(
      PlayerFragment.newInstance(mediaItems = listOf(item), selectedMediaIdx = 0)
    )
  }
}
