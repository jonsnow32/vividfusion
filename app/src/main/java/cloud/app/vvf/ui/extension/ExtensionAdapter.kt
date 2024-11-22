package cloud.app.vvf.ui.extension

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.databinding.ItemExtensionBinding
import cloud.app.vvf.ui.media.MediaContainerEmptyAdapter
import cloud.app.vvf.utils.loadWith
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder

class ExtensionAdapter(
    val listener: Listener
) : PagingDataAdapter<Extension<*>, ExtensionAdapter.ViewHolder>(DiffCallback) {

  interface Listener {
    fun onClick(extension: Extension<*>, view: View)
    fun onDragHandleTouched(viewHolder: ViewHolder)
  }

  object DiffCallback : DiffUtil.ItemCallback<Extension<*>>() {
    override fun areItemsTheSame(oldItem: Extension<*>, newItem: Extension<*>) =
      oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Extension<*>, newItem: Extension<*>) =
      oldItem == newItem
  }

    private val empty = MediaContainerEmptyAdapter()
    fun withEmptyAdapter() = ConcatAdapter(empty, this)

    class ViewHolder(val binding: ItemExtensionBinding, val listener: Listener) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(extension: Extension<*>) {
            binding.root.transitionName = extension.name
            binding.root.setOnClickListener { listener.onClick(extension, binding.root) }
            binding.extensionName.text = extension.metadata.name
            binding.extensionVersion.text = extension.metadata.version
            binding.itemExtension.apply {
              extension.metadata.iconUrl?.toImageHolder().loadWith(this, R.drawable.ic_extension_24dp) {
                    setImageDrawable(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemExtensionBinding.inflate(inflater, parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val download = getItem(position) ?: return
        holder.bind(download)
    }

    suspend fun submit(list: List<Extension<*>>) {
        empty.loadState = if (list.isEmpty()) LoadState.Loading else LoadState.NotLoading(true)
        submitData(PagingData.from(list))
    }
}
