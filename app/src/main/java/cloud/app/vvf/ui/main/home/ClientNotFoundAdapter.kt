package cloud.app.vvf.ui.main.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.databinding.ItemClientEmptyBinding
import cloud.app.vvf.databinding.ItemClientLoadingBinding
import cloud.app.vvf.ui.setting.ManageExtensionsFragment
import cloud.app.vvf.utils.navigate

class ClientNotFoundAdapter(val fragment: Fragment?) :
  RecyclerView.Adapter<ClientNotFoundAdapter.ViewHolder>() {

  override fun getItemCount() = 1

  class ViewHolder(val binding: ItemClientEmptyBinding) : RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
    ItemClientEmptyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
  )

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val binding = holder.binding;
    binding.addExtension.setOnClickListener {
      fragment?.navigate(ManageExtensionsFragment())
    }
  }
}
