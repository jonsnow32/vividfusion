package cloud.app.vvf.ui.extension.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.databinding.ItemExtensionUnselectedBinding

class ExtensionUnselected(val fragment: Fragment?) :
  RecyclerView.Adapter<ExtensionUnselected.ViewHolder>() {

  override fun getItemCount() = 1

  class ViewHolder(val binding: ItemExtensionUnselectedBinding) : RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
    ItemExtensionUnselectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
  )

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val binding = holder.binding;
  }
}
