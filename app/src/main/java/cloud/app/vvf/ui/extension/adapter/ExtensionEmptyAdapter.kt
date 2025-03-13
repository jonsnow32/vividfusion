package cloud.app.vvf.ui.extension.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.databinding.ItemExtensionEmptyBinding
import cloud.app.vvf.ui.setting.ManageExtensionsFragment
import cloud.app.vvf.utils.navigate

class ExtensionEmptyAdapter(val fragment: Fragment?) :
  RecyclerView.Adapter<ExtensionEmptyAdapter.ViewHolder>() {

  override fun getItemCount() = 1

  class ViewHolder(val binding: ItemExtensionEmptyBinding) : RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
    ItemExtensionEmptyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
  )

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val binding = holder.binding;
    binding.addExtension.setOnClickListener {
      fragment?.navigate(ManageExtensionsFragment())
    }
  }
}
