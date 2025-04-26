package cloud.app.vvf.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.databinding.DialogTrailersBinding
import cloud.app.vvf.ui.media.MediaItemAdapter
import cloud.app.vvf.ui.paging.toFlow
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.putSerialized
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrailerDialog : DockingDialog() {
  var binding by autoCleared<DialogTrailersBinding>()
  private val args by lazy { requireArguments() }
  private val items by lazy { args.getSerialized<List<Video>>(ARG_VIDEO_ITEMS) ?: emptyList() }
  private val extensionId by lazy {
    args.getString(ARG_CLIENT_ID)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogTrailersBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val videoAdapter = MediaItemAdapter(this, "", extensionId)
    binding.rvVideos.adapter = videoAdapter
    lifecycleScope.launch {
      val paged = items.map { AVPMediaItem.TrailerItem(it) }.toPaged()
      paged.toFlow().collectLatest(videoAdapter::submit)
    }
  }

  companion object {
    const val ARG_VIDEO_ITEMS = "ARG_VIDEO_ITEMS"
    const val ARG_CLIENT_ID = "ARG_CLIENT_ID"
    fun List<AVPMediaItem>.toPaged() = PagedData.Single { this }
    fun newInstance(videos: List<Video>?, extensionId: String) = TrailerDialog().apply {
      arguments = Bundle().apply {
        putSerialized(ARG_VIDEO_ITEMS, ArrayList(videos))
        putString(ARG_CLIENT_ID, extensionId)
      }
    }
  }
}
