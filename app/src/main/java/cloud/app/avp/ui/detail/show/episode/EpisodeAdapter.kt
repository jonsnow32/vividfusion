package cloud.app.avp.ui.detail.show.episode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.avp.databinding.EpisodeItemLargeBinding
import cloud.app.avp.databinding.EpisodeItemSmallBinding
import cloud.app.common.models.AVPMediaItem

class EpisodeAdapter : ListAdapter<AVPMediaItem.EpisodeItem, RecyclerView.ViewHolder>(EpisodeDiffCallback()) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when(viewType) {
      1 -> {
        val binding = EpisodeItemLargeBinding.inflate(inflater, parent, false) // Assuming you have EpisodeItemBinding
        ViewHolderLarge(binding)
      }
      else -> {
        val binding = EpisodeItemSmallBinding.inflate(inflater, parent, false) // Assuming you have EpisodeItemBinding
        ViewHolderSmall(binding)
      }
    }

  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val episodeItem = getItem(position)
  }

  class EpisodeDiffCallback : DiffUtil.ItemCallback<AVPMediaItem.EpisodeItem>() {
    override fun areItemsTheSame(oldItem: AVPMediaItem.EpisodeItem, newItem: AVPMediaItem.EpisodeItem): Boolean {
      return oldItem.sameAs(newItem) // Assuming 'id' is a unique identifier
    }

    override fun areContentsTheSame(oldItem: AVPMediaItem.EpisodeItem, newItem: AVPMediaItem.EpisodeItem): Boolean {
      return oldItem == newItem // Compare all relevant fields for content changes
    }
  }


  class ViewHolderLarge(val binding: EpisodeItemLargeBinding) : RecyclerView.ViewHolder(binding.root)
  class ViewHolderSmall(val binding: EpisodeItemSmallBinding) : RecyclerView.ViewHolder(binding.root)
}
