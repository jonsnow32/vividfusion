package cloud.app.avp.ui.detail.show.episode

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cloud.app.avp.R
import cloud.app.avp.databinding.EpisodeItemLargeBinding
import cloud.app.avp.databinding.EpisodeItemSmallBinding
import cloud.app.avp.utils.TimeUtils.toLocalDayMonthYear
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.setTextWithVisibility
import cloud.app.common.models.AVPMediaItem

class EpisodeAdapter :
  PagingDataAdapter<AVPMediaItem.EpisodeItem, RecyclerView.ViewHolder>(EpisodeDiffCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      1 -> ViewHolderLarge(EpisodeItemLargeBinding.inflate(inflater, parent, false))
      else -> ViewHolderSmall(EpisodeItemSmallBinding.inflate(inflater, parent, false))
    }
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    return if (item?.backdrop == null) 0 else 1
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val item = getItem(position) ?: return
    when (holder) {
      is ViewHolderLarge -> holder.bind(item)
      is ViewHolderSmall -> holder.bind(item)
    }
  }

  suspend fun submit(pagingData: PagingData<AVPMediaItem.EpisodeItem>?) {
    submitData(pagingData ?: PagingData.empty())
  }


  companion object EpisodeDiffCallback : DiffUtil.ItemCallback<AVPMediaItem.EpisodeItem>() {
    override fun areItemsTheSame(
      oldItem: AVPMediaItem.EpisodeItem,
      newItem: AVPMediaItem.EpisodeItem
    ): Boolean = oldItem.sameAs(newItem)

    override fun areContentsTheSame(
      oldItem: AVPMediaItem.EpisodeItem,
      newItem: AVPMediaItem.EpisodeItem
    ): Boolean = oldItem == newItem
  }

  class ViewHolderLarge(private val binding: EpisodeItemLargeBinding) :
    RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(item: AVPMediaItem.EpisodeItem) {
      val unixTimeMS = System.currentTimeMillis()
      val isUpcoming = unixTimeMS < (item.episode.generalInfo.releaseDateMsUTC ?: 0L)

      binding.episodeText.text = "${item.episode.episodeNumber}. ${item.title}"
      item.backdrop?.let { it.loadInto(binding.episodePoster) }

      binding.episodeDescript.setTextWithVisibility(item.episode.generalInfo.overview)
      binding.episodeRating.setTextWithVisibility(
        if (isUpcoming) itemView.context.getString(R.string.up_coming)
        else item.episode.generalInfo.rating?.toString()
      )
      binding.episodeRuntime.setTextWithVisibility(item.episode.generalInfo.runtime?.toString())
      binding.episodeDate.setTextWithVisibility(item.episode.generalInfo.releaseDateMsUTC?.toLocalDayMonthYear())
      binding.episodeProgress.isGone = true
    }
  }

  class ViewHolderSmall(private val binding: EpisodeItemSmallBinding) :
    RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(item: AVPMediaItem.EpisodeItem) {
      val unixTimeMS = System.currentTimeMillis()
      val isUpcoming = unixTimeMS < (item.episode.generalInfo.releaseDateMsUTC ?: 0L)

      binding.episodeText.text = "${item.episode.episodeNumber}. ${item.title}"
      binding.episodeRating.setTextWithVisibility(
        if (isUpcoming) itemView.context.getString(R.string.up_coming)
        else item.episode.generalInfo.rating?.toString()
      )
      binding.episodeRuntime.setTextWithVisibility(item.episode.generalInfo.runtime?.toString())
      binding.episodeDate.setTextWithVisibility(item.episode.generalInfo.releaseDateMsUTC?.toLocalDayMonthYear())
      binding.episodeProgress.isGone = true
    }
  }
}
