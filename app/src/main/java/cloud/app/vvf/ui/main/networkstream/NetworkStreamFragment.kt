package cloud.app.vvf.ui.main.networkstream

import android.annotation.SuppressLint
import android.content.Intent
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
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.databinding.FragmentNetworkStreamBinding
import cloud.app.vvf.features.player.PlayerFragment
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkStreamFragment : Fragment() {
  private val parent get() = parentFragment as Fragment
  private var binding by autoCleared<FragmentNetworkStreamBinding>()
  private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
    uri?.let {
      // Set the selected file's URI as the stream URL (or handle as needed)
      binding.etStreamUrl.setText(it.toString())
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
    val viewModel by parent.viewModels<NetworkStreamViewModel>()
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.root, binding.rvStreams)
    binding.etStreamUrl.setText("magnet:?xt=urn:btih:53A4A411DECDAF7E1BE919607B7A4187987BF0BB&dn=Ballerina%20From%20the%20World%20of%20John%20Wick%202025%20576p%20WEBRip%20x265-SSN&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce&tr=udp%3A%2F%2Ftracker.bittor.pw%3A1337%2Fannounce&tr=udp%3A%2F%2Fpublic.popcorn-tracker.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.dler.org%3A6969%2Fannounce&tr=udp%3A%2F%2Fexodus.desync.com%3A6969&tr=udp%3A%2F%2Fopen.demonii.com%3A1337%2Fannounce")
    binding.btnStreaming.setOnClickListener {
      val streamUrl = binding.etStreamUrl.text.toString()
      if (streamUrl.isNotBlank()) {
        // Navigate to PlayerFragment to play the stream
        val video = Video.RemoteVideo(uri = streamUrl, title = streamUrl)
        val mediaItem = AVPMediaItem.VideoItem(video)
        parent.navigate(
          PlayerFragment.newInstance(
            mediaItems = listOf(mediaItem),
            selectedMediaIdx = 0,
            //this is testing
//            subtitles = listOf(
//              SubtitleData(
//                name = "WebVTT positioning",
//                mimeType = "text/vtt",
//                languageCode = "en",
//                origin = SubtitleOrigin.URL,
//                url = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/numeric-lines.vtt",
//                headers = mapOf()
//              ),
//            ),
          )
        )
      }
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

  fun addNetworkStream(stream: String) {
    val viewModel by parent.viewModels<NetworkStreamViewModel>()
  }

}
