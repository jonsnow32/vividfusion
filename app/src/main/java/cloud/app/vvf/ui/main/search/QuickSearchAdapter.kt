package cloud.app.vvf.ui.main.search

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import cloud.app.vvf.common.models.SearchItem

class QuickSearchAdapter(val listener: Listener) :
    ListAdapter<SearchItem, QuickSearchViewHolder>(diff) {

    interface Listener {
        fun onClick(item: SearchItem, transitionView: View)
        fun onLongClick(item: SearchItem, transitionView: View): Boolean
        fun onDelete(item: SearchItem)
    }

    companion object {
        val diff = object : DiffUtil.ItemCallback<SearchItem>() {
            override fun areItemsTheSame(oldItem: SearchItem, newItem: SearchItem) =
                oldItem.sameAs(newItem)

            override fun areContentsTheSame(oldItem: SearchItem, newItem: SearchItem) =
                oldItem == newItem

        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QuickSearchViewHolder.Query.create(parent)

    override fun onBindViewHolder(holder: QuickSearchViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
        holder.itemView.setOnClickListener {
            listener.onClick(item, holder.transitionView)
        }
        holder.itemView.setOnLongClickListener {
            listener.onLongClick(item, holder.transitionView)
        }
        holder.deleteView.setOnClickListener {
            listener.onDelete(item)
        }
    }
}
