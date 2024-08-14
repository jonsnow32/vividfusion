package cloud.app.avp.ui.stream

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cloud.app.avp.MainActivityViewModel.Companion.applyContentInsets
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.databinding.FragmentMovieBinding
import cloud.app.avp.databinding.FragmentStreamBinding
import cloud.app.avp.ui.media.MediaItemAdapter
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.setupTransition
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.stream.StreamData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StreamFragment : Fragment(), StreamAdapter.ItemClickListener {
  private var binding by autoCleared<FragmentStreamBinding>()
  private val viewModel by viewModels<StreamViewModel>()
  private val adapter = StreamAdapter(this)

  private val args by lazy { requireArguments() }
  private val mediaItem by lazy { args.getParcel<AVPMediaItem>("mediaItem")!! }

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
      binding.root.setPadding(0,it.top,0,it.bottom)
    }
    viewModel.mediaItem = mediaItem
    binding.recyclerView.adapter = adapter
    observe(viewModel.streams) {
      val current = adapter.currentList.toMutableList()
      current.addAll(it)
      adapter.submitList(current)
    }
    viewModel.initialize()
  }

  override fun onStreamItemClick(streamData: StreamData) {
    TODO("Not yet implemented")
  }

  override fun onStreamItemLongClick(streamData: StreamData) {
    TODO("Not yet implemented")
  }

  override fun onDoubleDpadUpClicked() {
    TODO("Not yet implemented")
  }

}
