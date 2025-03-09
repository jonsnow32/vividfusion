package cloud.app.vvf.ui.detail.show.episode

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.databinding.EpisodeItemLargeBinding
import cloud.app.vvf.databinding.EpisodeItemSmallBinding
import cloud.app.vvf.utils.TimeUtils.toLocalDayMonthYear
import cloud.app.vvf.utils.loadInto
import cloud.app.vvf.utils.setTextWithVisibility
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.movie.Episode

class EpisodeAdapter(private val listener: Listener) :
  ListAdapter<Episode, RecyclerView.ViewHolder>(EpisodeDiffCallback) {


  interface Listener {
    fun onClick(episode: Episode)
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
    return if (item?.generalInfo?.backdrop == null) 0 else 1
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
  }

  companion object EpisodeDiffCallback : DiffUtil.ItemCallback<Episode>() {
    override fun areItemsTheSame(
      oldItem: Episode,
      newItem: Episode
    ): Boolean = oldItem.ids.equals(newItem)

    override fun areContentsTheSame(
      oldItem: Episode,
      newItem: Episode
    ): Boolean = oldItem.ids.equals(newItem)
  }

  class ViewHolderLarge(private val binding: EpisodeItemLargeBinding) :
    RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: Episode) {
      val unixTimeMS = System.currentTimeMillis()
      val isUpcoming = unixTimeMS < (item.generalInfo.releaseDateMsUTC ?: 0L)

      binding.episodeText.text = "${item.episodeNumber}. ${item.generalInfo.title}"
      item.generalInfo.backdrop?.toImageHolder()?.loadInto(binding.episodePoster)

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
