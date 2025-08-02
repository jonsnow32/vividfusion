package cloud.app.vvf.ui.main.networkstream

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.datastore.app.helper.UriHistoryItem

class UriHistoryAdapter(
  private var items: List<UriHistoryItem>,
  private val onClick: (UriHistoryItem) -> Unit,
  private val onLongClick: (UriHistoryItem) -> Unit,
  private val onDelete: (UriHistoryItem) -> Unit,
) : RecyclerView.Adapter<UriHistoryAdapter.UriHistoryViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UriHistoryViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_uri_history, parent, false)
    return UriHistoryViewHolder(view)
  }

  override fun onBindViewHolder(holder: UriHistoryViewHolder, position: Int) {
    val item = items[position]
    holder.textView.text = item.uri
    holder.itemView.setOnClickListener { onClick(item) }
    holder.itemView.setOnLongClickListener { onLongClick(item) ; true }
    holder.deleteButton.setOnClickListener { onDelete(item) }
  }

  override fun getItemCount(): Int = items.size

  fun updateItems(newItems: List<UriHistoryItem>) {
    items = newItems
    notifyDataSetChanged()
  }

  class UriHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textView: TextView = itemView.findViewById(R.id.tvUriHistory)
    val deleteButton: View = itemView.findViewById(R.id.btnDeleteHistory)
  }
}
