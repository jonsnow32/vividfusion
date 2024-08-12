package cloud.app.avp.ui.extension

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
import cloud.app.avp.R
import cloud.app.avp.databinding.ItemExtensionBinding
import cloud.app.avp.ui.media.MediaContainerEmptyAdapter
import cloud.app.avp.utils.loadWith
import cloud.app.common.clients.BaseExtension
import cloud.app.common.models.ImageHolder.Companion.toImageHolder

class ExtensionAdapter(
    val listener: Listener
) : PagingDataAdapter<BaseExtension, ExtensionAdapter.ViewHolder>(DiffCallback) {

    fun interface Listener {
        fun onClick(metadata: BaseExtension, view: View)
    }

    object DiffCallback : DiffUtil.ItemCallback<BaseExtension>() {
        override fun areItemsTheSame(
            oldItem: BaseExtension, newItem: BaseExtension
        ) = oldItem.javaClass == newItem.javaClass

        override fun areContentsTheSame(
            oldItem: BaseExtension, newItem: BaseExtension
        ) = oldItem.metadata == newItem.metadata
    }

    private val empty = MediaContainerEmptyAdapter()
    fun withEmptyAdapter() = ConcatAdapter(empty, this)

    class ViewHolder(val binding: ItemExtensionBinding, val listener: Listener) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(extension: BaseExtension) {
            binding.root.transitionName = extension.metadata.name
            binding.root.setOnClickListener { listener.onClick(extension, binding.root) }
            binding.extensionName.text = extension.metadata.name
            binding.extensionVersion.text = extension.metadata.version
            binding.itemExtension.apply {
              extension.metadata.icon.toImageHolder().loadWith(this, R.drawable.ic_extension_24dp) {
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

    suspend fun submit(list: List<BaseExtension>) {
        empty.loadState = if (list.isEmpty()) LoadState.Loading else LoadState.NotLoading(true)
        submitData(PagingData.from(list))
    }
}
