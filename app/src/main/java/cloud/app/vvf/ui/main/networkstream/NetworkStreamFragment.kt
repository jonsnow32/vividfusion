package cloud.app.vvf.ui.main.networkstream

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.databinding.FragmentNetworkStreamBinding
import cloud.app.vvf.features.player.PlayerFragment
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class NetworkStreamFragment : Fragment() {
  private val parent by lazy {  parentFragment as Fragment }
  private var binding by autoCleared<FragmentNetworkStreamBinding>()
  private val viewModel: NetworkStreamViewModel by viewModels({ requireParentFragment() })
  private lateinit var uriHistoryAdapter: UriHistoryAdapter
  private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
    uri?.let {
      // Set the selected file's URI as the stream URL (or handle as needed)
      binding.etStreamUrl.setText(it.toString())

      // Check if it's a torrent file and launch in torrent player
      val mimeType = requireContext().contentResolver.getType(it)
      val fileName = it.lastPathSegment?.lowercase() ?: ""

      if (mimeType == "application/x-bittorrent" || fileName.endsWith(".torrent")) {
        // Launch torrent file directly in the player
        streamTorrentFile(it.toString())
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentNetworkStreamBinding.inflate(inflater, container, false)
    return binding.root
  }

  @SuppressLint("ClickableViewAccessibility")
  @OptIn(UnstableApi::class)
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.root, binding.rvUriHistory)


    uriHistoryAdapter = UriHistoryAdapter(
      emptyList(),
      onClick ={ selectedItem ->
        binding.etStreamUrl.setText(selectedItem.uri)
      },
      onLongClick = {
        context?.showToast(it.uri)
      },
      onDelete = {
        viewModel.deleteHistory(it)
        viewModel.refresh()
      }
    )
    binding.rvUriHistory.adapter = uriHistoryAdapter
    if (binding.rvUriHistory.layoutManager == null) {
      binding.rvUriHistory.layoutManager = LinearLayoutManager(requireContext())
    }
    observe(viewModel.streamUris) {
      if (it.isNullOrEmpty()) {
        binding.rvUriHistory.visibility = View.GONE
        binding.txtHistory.visibility = View.GONE
      } else {
        binding.rvUriHistory.visibility = View.VISIBLE
        binding.txtHistory.visibility = View.VISIBLE
        uriHistoryAdapter.updateItems(it)
      }
    }
    viewModel.refresh()
    binding.etStreamUrl.setText("magnet:?xt=urn:btih:53A4A411DECDAF7E1BE919607B7A4187987BF0BB")
//    binding.etStreamUrl.setText("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    binding.btnStreaming.setOnClickListener {
      stream(binding.etStreamUrl.text.toString().trim())
    }


    binding.btnDownload.setOnClickListener {
      //todo add to download queue
    }

    binding.etStreamUrl.setOnTouchListener { v, event ->
      var editText = binding.etStreamUrl
      if (event.action == MotionEvent.ACTION_UP) {
        val drawableEnd = editText.compoundDrawables[2]
        if (drawableEnd != null) {
          val openFile = editText.width - editText.paddingEnd - drawableEnd.intrinsicWidth
          if (event.x >= openFile) {
            // Open file browser to select a file (e.g., torrent)
            openFileLauncher.launch(arrayOf("application/x-bittorrent", "application/octet-stream", "*/*"))
            return@setOnTouchListener true
          }
        }
      }
      false
    }
  }
  @UnstableApi
  fun stream(uri: String){
    val streamUrl = uri
    if (streamUrl.isNotBlank()) {
      // Navigate to PlayerFragment to play the stream
      val video = Video.RemoteVideo(uri = streamUrl, title = streamUrl)
      val mediaItem = AVPMediaItem.VideoItem(video)
      viewModel.saveToUriHistory(streamUrl) // Save the stream URL to history
      viewModel.refresh()
      parent.navigate(
        PlayerFragment.newInstance(
          mediaItems = listOf(mediaItem),
          selectedMediaIdx = 0,
        )
      )
    }
  }

  @UnstableApi
  private fun streamTorrentFile(torrentUri: String) {
    // Create a video item for the torrent file
    val video = Video.RemoteVideo(uri = torrentUri, title = "Torrent File")
    val mediaItem = AVPMediaItem.VideoItem(video)

    // Save to history
    viewModel.saveToUriHistory(torrentUri)
    viewModel.refresh()

    // Navigate to PlayerFragment which will handle torrent processing
    parent.navigate(
      PlayerFragment.newInstance(
        mediaItems = listOf(mediaItem),
        selectedMediaIdx = 0,
      )
    )
  }

  fun addNetworkStream(stream: String) {
    val viewModel by parent.viewModels<NetworkStreamViewModel>()
  }

}
