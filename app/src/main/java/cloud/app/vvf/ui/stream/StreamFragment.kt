package cloud.app.vvf.ui.stream

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.databinding.FragmentStreamBinding
import cloud.app.vvf.features.player.PlayerManager
import cloud.app.vvf.features.player.data.PlayData
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.stream.StreamData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StreamFragment : Fragment(), StreamAdapter.ItemClickListener {
  private var binding by autoCleared<FragmentStreamBinding>()
  private val viewModel by viewModels<StreamViewModel>()
  private val adapter = StreamAdapter(this)

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
    setupTransition(view)
    applyInsets {
      binding.root.setPadding(0, it.top, 0, it.bottom)
    }
    viewModel.mediaItem = mediaItem
    binding.recyclerView.adapter = adapter
    observe(viewModel.streams) { stream ->
      adapter.submitList(stream)
    }

    viewModel.initialize()
  }

  override fun onStreamItemClick(streamData: StreamData) {
    val playData = PlayData(
      listOf(streamData),
      selectedId = 0,
      title = streamData.fileName
    )
    PlayerManager.getInstance().play(playData, parentFragmentManager)
  }

  override fun onStreamItemLongClick(streamData: StreamData) {
    TODO("Not yet implemented")
  }

  override fun onDoubleDpadUpClicked() {
    TODO("Not yet implemented")
  }

}
