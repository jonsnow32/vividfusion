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
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.setTextWithVisibility
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.common.models.movie.Episode

class EpisodeAdapter :
  ListAdapter<Episode, RecyclerView.ViewHolder>(EpisodeDiffCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      1 -> ViewHolderLarge(EpisodeItemLargeBinding.inflate(inflater, parent, false))
      else -> ViewHolderSmall(EpisodeItemSmallBinding.inflate(inflater, parent, false))
    }
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    return if (item?.generalInfo?.backdrop == null) 0 else 1
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val item = getItem(position) ?: return
    when (holder) {
      is ViewHolderLarge -> holder.bind(item)
      is ViewHolderSmall -> holder.bind(item)
    }
  }

  companion object EpisodeDiffCallback : DiffUtil.ItemCallback<Episode>() {
    override fun areItemsTheSame(
      oldItem: Episode,
      newItem: Episode
    ): Boolean = oldItem.ids.equals(newItem)

    override fun areContentsTheSame(
      oldItem: Episode,
      newItem: Episode
    ): Boolean = oldItem == newItem
  }

  class ViewHolderLarge(private val binding: EpisodeItemLargeBinding) :
    RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(item: Episode) {
      val unixTimeMS = System.currentTimeMillis()
      val isUpcoming = unixTimeMS < (item.generalInfo.releaseDateMsUTC ?: 0L)

      binding.episodeText.text = "${item.episodeNumber}. ${item.generalInfo.title}"
      item.generalInfo.backdrop?.let { it.toImageHolder().loadInto(binding.episodePoster) }

      binding.episodeDescript.setTextWithVisibility(item.generalInfo.overview)
      binding.episodeRating.setTextWithVisibility(
        if (isUpcoming) itemView.context.getString(R.string.up_coming)
        else item.generalInfo.rating?.toString()
      )
      binding.episodeRuntime.setTextWithVisibility(item.generalInfo.runtime?.toString())
      binding.episodeDate.setTextWithVisibility(item.generalInfo.releaseDateMsUTC?.toLocalDayMonthYear())
      binding.episodeProgress.isGone = true
    }
  }

  class ViewHolderSmall(private val binding: EpisodeItemSmallBinding) :
    RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(item: Episode) {
      val unixTimeMS = System.currentTimeMillis()
      val isUpcoming = unixTimeMS < (item.generalInfo.releaseDateMsUTC ?: 0L)

      binding.episodeText.text = "${item.episodeNumber}. ${item.generalInfo.title}"
      binding.episodeRating.setTextWithVisibility(
        if (isUpcoming) itemView.context.getString(R.string.up_coming)
        else item.generalInfo.rating?.toString()
      )
      binding.episodeRuntime.setTextWithVisibility(item.generalInfo.runtime?.toString())
      binding.episodeDate.setTextWithVisibility(item.generalInfo.releaseDateMsUTC?.toLocalDayMonthYear())
      binding.episodeProgress.isGone = true
    }
  }
}
