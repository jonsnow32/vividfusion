package cloud.app.vvf.ui.main.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.databinding.ItemQuickSearchQueryBinding
import cloud.app.vvf.common.models.SearchItem

sealed class QuickSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: SearchItem)
    abstract val deleteView: View
    open val transitionView: View
        get() = this.deleteView

    class Query(val binding: ItemQuickSearchQueryBinding) : QuickSearchViewHolder(binding.root) {
        override val deleteView: View
            get() = binding.delete

        override fun bind(item: SearchItem) {
            binding.history.visibility = if (item.searched) View.VISIBLE else View.INVISIBLE
            binding.query.text = "${item.query} ${item.searchedAt}"
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): QuickSearchViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Query(
                    ItemQuickSearchQueryBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }


}
