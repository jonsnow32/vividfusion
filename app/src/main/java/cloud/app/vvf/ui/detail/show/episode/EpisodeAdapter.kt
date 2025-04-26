package cloud.app.vvf.ui.detail.show.episode

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.databinding.EpisodeItemLargeBinding
import cloud.app.vvf.databinding.EpisodeItemSmallBinding
import cloud.app.vvf.utils.TimeUtils.toLocalDayMonthYear
import cloud.app.vvf.utils.loadInto
import cloud.app.vvf.utils.setTextWithVisibility
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.movie.Episode
import cloud.app.vvf.ui.detail.show.episode.EpisodeAdapter.EpisodeData

class EpisodeAdapter(private val listener: Listener) :
  ListAdapter<EpisodeData, RecyclerView.ViewHolder>(EpisodeDiffCallback) {

  data class EpisodeData(val episode: Episode, var position: Long, var duration: Long? = null)

  interface Listener {
    fun onClick(episode: EpisodeData)
    fun onLongClick(episode: EpisodeData) : Boolean
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      1 -> ViewHolderLarge(EpisodeItemLargeBinding.inflate(inflater, parent, false))
      else -> ViewHolderSmall(EpisodeItemSmallBinding.inflate(inflater, parent, false))
    }
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    return if (item?.episode?.generalInfo?.backdrop == null) 0 else 1
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val item = getItem(position) ?: return
    when (holder) {
      is ViewHolderLarge -> holder.bind(item)
      is ViewHolderSmall -> holder.bind(item)
    }
    holder.itemView.setOnClickListener {
      listener.onClick(item)
    }

    holder.itemView.setOnLongClickListener {
      listener.onLongClick(item)
    }
  }

  companion object EpisodeDiffCallback : DiffUtil.ItemCallback<EpisodeData>() {
    override fun areItemsTheSame(
      oldItem: EpisodeData,
      newItem: EpisodeData
    ): Boolean = oldItem.episode.seasonNumber == newItem.episode.seasonNumber && oldItem.episode.episodeNumber == newItem.episode.episodeNumber

    override fun areContentsTheSame(
      oldItem: EpisodeData,
      newItem: EpisodeData
    ): Boolean = oldItem.position == newItem.position && oldItem.duration == newItem.duration
  }

  class ViewHolderLarge(private val binding: EpisodeItemLargeBinding) :
    RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: EpisodeData) {
      val unixTimeMS = System.currentTimeMillis()
      val isUpcoming = unixTimeMS < (item.episode.generalInfo.releaseDateMsUTC ?: 0L)

      binding.episodeText.text = "${item.episode.episodeNumber}. ${item.episode.generalInfo.title}"
      item.episode.generalInfo.backdrop?.toImageHolder()?.loadInto(binding.episodePoster)

      binding.episodeDescript.setTextWithVisibility(item.episode.generalInfo.overview)
      binding.episodeRating.setTextWithVisibility(
        if (isUpcoming) itemView.context.getString(R.string.upcoming)
        else item.episode.generalInfo.rating?.toString()
      )
      binding.episodeRuntime.setTextWithVisibility(item.episode.generalInfo.runtime?.toString())
      binding.episodeDate.setTextWithVisibility(item.episode.generalInfo.releaseDateMsUTC?.toLocalDayMonthYear())
      binding.episodeProgress.isGone = false
      val duration = item.duration ?: 0
      val progress = if (duration > 0) (item.position * 100 / duration).toInt() else 0
      binding.episodeProgress.progress = progress
    }
  }

  class ViewHolderSmall(private val binding: EpisodeItemSmallBinding) :
    RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(item: EpisodeData) {
      val unixTimeMS = System.currentTimeMillis()
      val isUpcoming = unixTimeMS < (item.episode.generalInfo.releaseDateMsUTC ?: 0L)

      binding.episodeText.text = "${item.episode.episodeNumber}. ${item.episode.generalInfo.title}"
      binding.episodeRating.setTextWithVisibility(
        if (isUpcoming) itemView.context.getString(R.string.upcoming)
        else item.episode.generalInfo.rating?.toString()
      )
      binding.episodeRuntime.setTextWithVisibility(item.episode.generalInfo.runtime?.toString())
      binding.episodeDate.setTextWithVisibility(item.episode.generalInfo.releaseDateMsUTC?.toLocalDayMonthYear())
      binding.episodeProgress.isGone = false
      val duration = item.duration ?: 0
      binding.episodeProgress.progress = if (duration > 0) (item.position * 100 / duration).toInt() else 0
    }
  }
}
