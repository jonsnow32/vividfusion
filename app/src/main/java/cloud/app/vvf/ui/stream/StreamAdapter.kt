package cloud.app.vvf.ui.stream


import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.video.VVFVideo
import cloud.app.vvf.databinding.ItemStreamBinding
import cloud.app.vvf.ui.detail.loadWith
import cloud.app.vvf.utils.loadInto

class StreamAdapter(val listener: ItemClickListener) :
  ListAdapter<VVFVideo, StreamAdapter.ViewHolder>(DiffCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val view = StreamAdapter.ViewHolder(
      ItemStreamBinding.inflate(inflater, parent, false),
      parent.context
    )
    return view
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = getItem(position)
    holder.bind(item)
    holder.itemView.setOnClickListener {
      listener.onStreamItemClick(item)
    }
    holder.itemView.setOnLongClickListener {
      listener.onStreamItemLongClick(item)
      true
    }
    holder.itemView.setOnKeyListener { _, keyCode, event ->
      if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastClickTime

        if (elapsedTime <= DOUBLE_CLICK_TIME_DELTA) {
          listener.onDoubleDpadUpClicked()
        }
        lastClickTime = currentTime
        true
      }
      false
    }

  }

  private var lastClickTime = 0L
  private val DOUBLE_CLICK_TIME_DELTA = 100L // time in milliseconds


  class ViewHolder(val binding: ItemStreamBinding, val context: Context) :
    RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("ResourceType")
    fun bind(videoItem: VVFVideo) {
      binding.name.text = videoItem.title
      binding.url.text = videoItem.uri
      when(videoItem) {
        is VVFVideo.LocalVideo -> {
          videoItem.thumbnailUri.toImageHolder().loadInto(binding.logo)
        }
        is VVFVideo.RemoteVideo -> {
          videoItem.providerLogo?.toImageHolder().loadInto(binding.logo)
          videoItem.hostLogo?.toImageHolder().loadInto(binding.extLogo)
        }
      }

    }
  }

  interface ItemClickListener {
    fun onStreamItemClick(streamData: VVFVideo)
    fun onStreamItemLongClick(streamData: VVFVideo)
    fun onDoubleDpadUpClicked()
  }

  object DiffCallback : DiffUtil.ItemCallback<VVFVideo>() {
    override fun areItemsTheSame(oldItem: VVFVideo, newItem: VVFVideo): Boolean {
      return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: VVFVideo, newItem: VVFVideo): Boolean {
      return when(oldItem) {
        is VVFVideo.LocalVideo -> {
          newItem is VVFVideo.LocalVideo && oldItem.thumbnailUri == newItem.thumbnailUri
        }
        is VVFVideo.RemoteVideo -> {
          newItem is VVFVideo.RemoteVideo && oldItem.uri == newItem.uri && oldItem.fileSize == newItem.fileSize
        }
      }
    }
  }
}
