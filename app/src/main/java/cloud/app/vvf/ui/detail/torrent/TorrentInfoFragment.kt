package cloud.app.vvf.ui.detail.torrent

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.MainActivityViewModel.Companion.applySystemInsets
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.databinding.FragmentTorrentInfoBinding
import cloud.app.vvf.features.player.PlayerFragment
import cloud.app.vvf.utils.KUniFile
import cloud.app.vvf.utils.TimeUtils.toLocalDayMonthYear
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getFreeSpace
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.toHumanReadableSize
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class TorrentInfoFragment : Fragment() {
  private var binding by autoCleared<FragmentTorrentInfoBinding>()
  val viewModel by viewModels<TorrentInfoViewModel>()

  private val args by lazy { requireArguments() }
  private val uri by lazy { args.getParcelable<Uri>("uri")!! }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentTorrentInfoBinding.inflate(inflater, container, false)
    return binding.root
  }

  @OptIn(UnstableApi::class)
  @SuppressLint("SetTextI18n")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)


    applySystemInsets(binding.appBarLayout, binding.contentPanel) {
      binding.layoutButtons.setPadding(it.start, 0, it.end, it.bottom)
      binding.contentPanel.setPadding(it.start, 8, it.end, it.bottom + 60)
    }

    observe(viewModel.torrentInfo) { it ->
      val unknownString = context?.getString(R.string.unknown)
      binding.toolbar.apply {
        title = it?.name ?: "Torrent Info"
        setNavigationIcon(R.drawable.ic_back)
        setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
      }
      binding.tvUploadPath.text = uri.path
      binding.tvTorrentSize.text = it?.totalSize?.toHumanReadableSize() ?: unknownString
      binding.tvFreeSpace.text = requireContext().filesDir?.let { dir ->
        getFreeSpace(dir.absolutePath).toHumanReadableSize() + " " + context?.getString(R.string.free)
      } ?: unknownString
      binding.tvFileCount.text = it?.fileCount?.toString() ?: unknownString
      binding.tvHashSum.text = it?.infoHash ?: unknownString
      binding.tvCreated.text = it?.creationDate?.toLocalDayMonthYear() ?: unknownString
      binding.tvComment.text = it?.comment ?: unknownString
      binding.tvCreatedInProgram.text = it?.createdBy ?: unknownString

      // New fields
      binding.tvAnnounce.text = it?.announce ?: unknownString
      binding.tvAnnounceList.text =
        it?.announceList?.joinToString("\n") { tier -> tier.joinToString(", ") } ?: unknownString
      binding.tvPieceLength.text = it?.pieceLength?.toString() ?: unknownString
      binding.tvPieces.text = it?.pieces?.let { p -> p.size.toLong().toHumanReadableSize() } ?: unknownString
      binding.tvPrivate.text = if (it?.isPrivate == true) "Yes" else "No"
      binding.tvEncoding.text = it?.encoding ?: unknownString

      // Show file list as simple text like announceList
      val fileList = it?.files ?: emptyList()
      binding.tvTorrentFilesText?.text = fileList.joinToString("\n") { file ->
        "${file.path} (${file.size.toHumanReadableSize()})"
      }
    }

    binding.btnStream.setOnClickListener {
      val actualUri = if (uri.scheme == "content") {
        val file = getFileFromContentUri(requireContext(), uri)
        file?.absolutePath ?: uri.toString()
      } else {
        uri.toString()
      }
      val video = Video.RemoteVideo(uri = actualUri, title = viewModel.torrentInfo.value?.name)
      val mediaItem = AVPMediaItem.VideoItem(video)
      navigate(
        PlayerFragment.newInstance(
          mediaItems = listOf(mediaItem),
          selectedMediaIdx = 0,
        )
      )
    }
    binding.btnDownload.setOnClickListener {

    }

    viewModel.loadTorrentInfo(uri)
  }

  fun getFileFromContentUri(context: Context, uri: Uri): File? {
    val kuniFile = KUniFile.fromUri(context, uri) ?: return null
    return try {
      val inputStream = kuniFile.openInputStream()
      val file = File.createTempFile("torrent_temp", ".torrent", context.cacheDir)
      file.outputStream().use { output ->
        inputStream.copyTo(output)
      }
      file
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
