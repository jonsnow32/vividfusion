package cloud.app.vvf.ui.main.files

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.video.Video
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit

class FilesAdapter(
  private var items: List<AVPMediaItem.VideoItem>,
  private val onClick: (AVPMediaItem.VideoItem) -> Unit,
) : RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = items[position]
    val video = item.video
    holder.title.text = video.title
    holder.subtitle.text = formatDuration(video.duration)
    val thumbUri = video.thumbnailUri?.let { Uri.parse(it) }
    Glide.with(holder.thumbnail)
      .load(thumbUri)
      .placeholder(R.drawable.ic_video)
      .error(R.drawable.ic_video)
      .centerCrop()
      .into(holder.thumbnail)
    holder.itemView.setOnClickListener { onClick(item) }
  }

  override fun getItemCount() = items.size

  fun submitList(newItems: List<AVPMediaItem.VideoItem>) {
    items = newItems
    notifyDataSetChanged()
  }

  private fun formatDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0) return ""
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val thumbnail: ImageView = itemView.findViewById(R.id.imageView)
    val title: TextView = itemView.findViewById(R.id.title)
    val subtitle: TextView = itemView.findViewById(R.id.subtitle)
  }
}
