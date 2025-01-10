package cloud.app.vvf.ui.stream


import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.databinding.ItemStreamBinding
import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.utils.loadInto

class StreamAdapter(val listener: ItemClickListener) :
  ListAdapter<StreamData, StreamAdapter.ViewHolder>(DiffCallback) {

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
    fun bind(streamData: StreamData) {
      binding.name.text = streamData.providerName
      binding.url.text = streamData.originalUrl
      streamData.providerLogo?.toImageHolder().loadInto(binding.logo)
      streamData.hostLogo?.toImageHolder().loadInto(binding.extLogo)
    }
  }

  interface ItemClickListener {
    fun onStreamItemClick(streamData: StreamData)
    fun onStreamItemLongClick(streamData: StreamData)
    fun onDoubleDpadUpClicked()
  }

  object DiffCallback : DiffUtil.ItemCallback<StreamData>() {
    override fun areItemsTheSame(oldItem: StreamData, newItem: StreamData): Boolean {
      return oldItem.originalUrl == newItem.originalUrl
    }

    override fun areContentsTheSame(oldItem: StreamData, newItem: StreamData): Boolean {
      return oldItem.originalUrl == newItem.originalUrl && oldItem.fileSize == newItem.fileSize
    }
  }
}
