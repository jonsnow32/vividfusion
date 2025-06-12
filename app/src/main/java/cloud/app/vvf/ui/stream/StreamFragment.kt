package cloud.app.vvf.ui.stream

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.stream.PremiumType
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.databinding.FragmentStreamBinding
import cloud.app.vvf.features.playerManager.PlayerManager
import cloud.app.vvf.features.playerManager.data.PlayData
import cloud.app.vvf.ui.setting.appLanguages
import cloud.app.vvf.ui.setting.getCurrentLocale
import cloud.app.vvf.ui.setting.getCurrentRegion
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.SubtitleHelper
import cloud.app.vvf.utils.Utils
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.setTextWithVisibility
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max

@AndroidEntryPoint
class StreamFragment : DockingDialog(), StreamAdapter.ItemClickListener {
  private var binding by autoCleared<FragmentStreamBinding>()
  private val viewModel by viewModels<StreamViewModel>()

  private lateinit var streamAdapter: StreamAdapter
  private lateinit var loadStateAdapter: StreamLoadStateAdapter

  private val args by lazy { requireArguments() }
  private val mediaItem by lazy { args.getSerialized<AVPMediaItem>("mediaItem")!! }

  companion object {
    fun newInstance(
      avpMediaItem: AVPMediaItem
    ) = StreamFragment().apply {
      arguments = Bundle().apply {
        putSerialized("mediaItem", avpMediaItem)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentStreamBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.mediaItem = mediaItem
    streamAdapter = StreamAdapter(this)
    loadStateAdapter = StreamLoadStateAdapter { viewModel.loadStream(mediaItem) }

    binding.apply {
      text1.text = getString(R.string.streams)
      recyclerView.adapter = ConcatAdapter(loadStateAdapter, streamAdapter)

    }

    observe(viewModel.region) {
      if (it != null) {
        binding.filterLanguageText.setTextWithVisibility(it)
        viewModel.loadStream(mediaItem)}

    }

    observe(viewModel.isLoading) { isLoading ->
      loadStateAdapter.isLoading = isLoading
    }

    observe(viewModel.streams) { stream ->
      if(stream != null) {
        streamAdapter.submitList(stream)
        binding.emptyView.root.isGone = stream.isNotEmpty()
      }
    }

    binding.filterLanguageRoot.setOnClickListener {
      val tempLang = viewModel.getSupportRegion()
      val current = getCurrentRegion(requireContext())
      val languageCodes = tempLang.keys.toList()
      val languageNames = tempLang.values.toList()

      val index = max(languageCodes.indexOf(current), 0)
      SelectionDialog.single(languageNames, index, getString(R.string.select_region), false)
        .show(parentFragmentManager) { result ->
          result?.let {
            it.getIntegerArrayList("selected_items")?.let { indexs ->
              viewModel.region.value = languageNames[indexs[0]]
            }
          }
        }
    }
  }

  override fun onStreamItemClick(streamData: Video) {
    when (streamData) {
      is Video.LocalVideo -> {
        val playData = PlayData(
          listOf(streamData),
          selectedId = 0,
          title = streamData.title
        )
        PlayerManager.getInstance().play(playData, parentFragmentManager)
      }

      is Video.RemoteVideo -> {
        when (streamData.premiumType) {
          PremiumType.JustWatch.ordinal -> {
            Utils.launchBrowser(requireContext(), streamData.originalUrl)
          }

          else -> {
            val playData = PlayData(
              listOf(streamData),
              selectedId = 0,
              title = streamData.fileName
            )
            PlayerManager.getInstance().play(playData, parentFragmentManager)
          }
        }
      }
    }
  }

  override fun onStreamItemLongClick(streamData: Video) {
    TODO("Not yet implemented")
  }

  override fun onDoubleDpadUpClicked() {
    TODO("Not yet implemented")
  }
}
