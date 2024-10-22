package cloud.app.avp.ui.detail.show.episode

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.avp.R
import cloud.app.avp.databinding.EpisodeItemLargeBinding
import cloud.app.avp.databinding.EpisodeItemSmallBinding
import cloud.app.avp.utils.TimeUtils.toLocalDayMonthYear
import cloud.app.avp.utils.TimeUtils.toLocalMonthYear
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.setTextWithVisibility
import cloud.app.common.models.AVPMediaItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

class EpisodeAdapter :
  ListAdapter<AVPMediaItem.EpisodeItem, RecyclerView.ViewHolder>(EpisodeDiffCallback()) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      1 -> {
        val binding = EpisodeItemLargeBinding.inflate(
          inflater,
          parent,
          false
        ) // Assuming you have EpisodeItemBinding
        ViewHolderLarge(binding)
      }

      else -> {
        val binding = EpisodeItemSmallBinding.inflate(
          inflater,
          parent,
          false
        ) // Assuming you have EpisodeItemBinding
        ViewHolderSmall(binding)
      }
    }

  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    return if (item.backdrop == null) 0 else 1
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

    when (holder) {
      is ViewHolderLarge -> {
        holder.bind(getItem(position))
      }

      is ViewHolderSmall -> {
        holder.bind(getItem(position))
      }
    }


  }

  class EpisodeDiffCallback : DiffUtil.ItemCallback<AVPMediaItem.EpisodeItem>() {
    override fun areItemsTheSame(
      oldItem: AVPMediaItem.EpisodeItem,
      newItem: AVPMediaItem.EpisodeItem
    ): Boolean {
      return oldItem.sameAs(newItem) // Assuming 'id' is a unique identifier
    }

    override fun areContentsTheSame(
      oldItem: AVPMediaItem.EpisodeItem,
      newItem: AVPMediaItem.EpisodeItem
    ): Boolean {
      return oldItem == newItem // Compare all relevant fields for content changes
    }
  }


  class ViewHolderLarge(val binding: EpisodeItemLargeBinding) :
    RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(item: AVPMediaItem.EpisodeItem) {
      val unixTimeMS = System.currentTimeMillis()

      val isUpcoming = unixTimeMS < (item.episode.generalInfo.releaseDateMsUTC ?: 0L)

      binding.episodeText.text = "${item.episode.episodeNumber}. ${item.title}"
      item.backdrop.loadInto(binding.episodePoster)
      binding.episodeDescript.setTextWithVisibility(item.episode.generalInfo.overview)
      if(isUpcoming)
        binding.episodeRating.text = this.itemView.context.getString(R.string.up_coming)
      else
        binding.episodeRating.setTextWithVisibility(item.episode.generalInfo.rating?.toString())

      binding.episodeRuntime.setTextWithVisibility(item.episode.generalInfo.runtime?.toString())
      binding.episodeDate.setTextWithVisibility(item.episode.generalInfo.releaseDateMsUTC?.toLocalDayMonthYear())
    }
  }

  class ViewHolderSmall(val binding: EpisodeItemSmallBinding) :
    RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(item: AVPMediaItem.EpisodeItem) {
      val unixTimeMS = System.currentTimeMillis()
      val isUpcoming = unixTimeMS < (item.episode.generalInfo.releaseDateMsUTC ?: 0L)
      val upComingText = this.itemView.context.getString(R.string.up_coming);
      binding.episodeProgress.isGone = true
      binding.episodeText.text = "${item.episode.episodeNumber}. ${item.title} ${if(isUpcoming) upComingText else "" }"
    }
  }
}
