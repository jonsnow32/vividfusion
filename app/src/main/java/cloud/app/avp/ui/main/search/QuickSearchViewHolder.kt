package cloud.app.avp.ui.main.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.app.avp.databinding.ItemQuickSearchMediaBinding
import cloud.app.avp.databinding.ItemQuickSearchQueryBinding
import cloud.app.avp.ui.main.media.MediaItemViewHolder.Companion.placeHolder
import cloud.app.avp.utils.loadInto
import cloud.app.common.models.QuickSearchItem

sealed class QuickSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: QuickSearchItem)
    abstract val insertView: View
    open val transitionView: View
        get() = this.insertView

    class Query(val binding: ItemQuickSearchQueryBinding) : QuickSearchViewHolder(binding.root) {
        override val insertView: View
            get() = binding.insert

        override fun bind(item: QuickSearchItem) {
            item as QuickSearchItem.SearchQueryItem
            binding.history.visibility = if (item.searched) View.VISIBLE else View.INVISIBLE
            binding.query.text = item.query
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

    class Media(val binding: ItemQuickSearchMediaBinding) : QuickSearchViewHolder(binding.root) {

        override val insertView: View
            get() = binding.insert

        override val transitionView: View
            get() = binding.coverContainer

        override fun bind(item: QuickSearchItem) {
            item as QuickSearchItem.SearchMediaItem
            binding.query.text = item.mediaItem.title
            item.mediaItem.poster.loadInto(binding.cover, item.mediaItem.placeHolder())
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): QuickSearchViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Media(
                    ItemQuickSearchMediaBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }
}
