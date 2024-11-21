package cloud.app.avp.ui.stream

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.databinding.FragmentStreamBinding
import cloud.app.avp.features.player.PlayerManager
import cloud.app.avp.features.player.data.PlayData
import cloud.app.avp.ui.setting.ExtensionSettingFragment
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.getSerialized
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.putSerialized
import cloud.app.avp.utils.setupTransition
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.stream.StreamData
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
      if (stream == null) return@observe
      val current = adapter.currentList.toMutableList()
      if (!current.contains(stream))
        current.add(stream)
      adapter.submitList(current)
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
