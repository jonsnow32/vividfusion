package cloud.app.vvf.ui.widget.dialog.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.databinding.ItemAccountAvatarBinding

class AvatarAdapter(val items: List<AvatarItem>) :
  RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AvatarViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemAccountAvatarBinding.inflate(inflater, parent, false)
    return AvatarViewHolder(binding)
  }

  override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
    val item = items[position]
    holder.bind(item)
    holder.itemView.setOnClickListener { v ->
      val old = items.find { it.selected }
      old?.selected = false
      notifyItemChanged(items.indexOf(old))

      item.selected = true
      notifyItemChanged(position)
    }
  }

  override fun getItemCount() = items.size

  class AvatarViewHolder(val binding: ItemAccountAvatarBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(avatarItem: AvatarItem) {
      binding.avatarImage.setImageResource(avatarItem.resId)
      binding.isSelected.visibility = if (avatarItem.selected) View.VISIBLE else View.GONE
      binding.avatarImage.alpha = if (avatarItem.selected) 1f else 0.6f
    }
  }
}
