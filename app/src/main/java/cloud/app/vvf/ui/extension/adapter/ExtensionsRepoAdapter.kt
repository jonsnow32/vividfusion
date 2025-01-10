package cloud.app.vvf.ui.extension.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.databinding.ItemExtensionBinding
import cloud.app.vvf.extension.ExtensionAssetResponse
import cloud.app.vvf.ui.extension.widget.InstallStatus
import cloud.app.vvf.utils.loadWith
import cloud.app.vvf.utils.setTextWithVisibility

class ExtensionsRepoAdapter(
  var list: List<Item>,
  val listener: Listener
) : RecyclerView.Adapter<ExtensionsRepoAdapter.ViewHolder>() {

  data class Item(
    val data: ExtensionAssetResponse,
    var status: InstallStatus = InstallStatus.INSTALLED
  )

  fun updateItem(item: Item) {
    val index = list.indexOfFirst { it.data.className == item.data.className }
    if (index != -1) {
      val updatedList = list.toMutableList()
      updatedList[index] = item
      // Notify the adapter about the change
      (list as MutableList<Item>).apply {
        clear()
        addAll(updatedList)
      }
      notifyItemChanged(index)
    }
  }

  fun interface Listener {
    fun onItemClicked(item: Item)
  }

  inner class ViewHolder(val binding: ItemExtensionBinding) :
    RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemExtensionBinding.inflate(inflater, parent, false)
    return ViewHolder(binding)
  }

  override fun getItemCount() = list.size

  @SuppressLint("SetTextI18n")
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = list[position]
    val binding = holder.binding
    binding.extensionName.text =  "${item.data.name} [v${item.data.version}]"
    binding.extensionVersion.setTextWithVisibility(item.data.description)
    binding.itemExtension.apply {
      item.data.iconUrl?.toImageHolder().loadWith(this, R.drawable.ic_extension_24dp) {
        setImageDrawable(it)
      }
    }
    binding.extensionAddOrRemove.setDownloadStatus(item.status)

    binding.extensionAddOrRemove.setOnClickListener {
      listener.onItemClicked(item)
    }
  }

}
