package cloud.app.avp.ui.detail.show.season

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.app.avp.databinding.ItemSeasonBinding
import cloud.app.avp.databinding.ItemSeasonLargeBinding
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.setTextWithVisibility
import cloud.app.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.common.models.movie.Season

class SeasonAdapter(
  var seasonList: List<Season>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      1 -> SeasonLargeViewHolder(ItemSeasonLargeBinding.inflate(inflater, parent, false))
      else -> SeasonViewHolder(ItemSeasonBinding.inflate(inflater, parent, false))
    }
  }


  override fun getItemCount(): Int = seasonList.size


  override fun getItemViewType(position: Int): Int {
    val item = seasonList[position]
    return if (item.posterPath == null || item.overview == null) 0 else 1
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val item = seasonList[position]
    when (holder) {
      is SeasonLargeViewHolder -> holder.bind(item)
      is SeasonViewHolder -> holder.bind(item)
    }
  }

  @SuppressLint("NotifyDataSetChanged")
  fun submitList(seasons: List<Season>) {
    seasonList = seasons;
    notifyDataSetChanged()
  }

  inner class SeasonViewHolder(private val binding: ItemSeasonBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(season: Season) {
      binding.seasonTitle.text = season.title ?: "Season ${season.number}"
      binding.watchProgress.text = "${season.episodes?.size ?: 0}/${season.episodeCount} episodes"
      binding.seasonProgress.progress = ((season.episodes?.size?.toFloat() ?: 0f / season.episodeCount) * 100).toInt()
    }
  }

  inner class SeasonLargeViewHolder(private val binding: ItemSeasonLargeBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(season: Season) {
      binding.seasonTitle.text = season.title ?: "Season ${season.number}"
      binding.watchProgress.text = "${season.episodes?.size ?: 0}/${season.episodeCount} episodes"
      binding.seasonProgress.progress = ((season.episodes?.size?.toFloat() ?: 0f / season.episodeCount) * 100).toInt()
      binding.seasonOverview.setTextWithVisibility(season.overview)
      season.posterPath?.toImageHolder().loadInto(binding.seasonPoster)
//      binding.seasonPoster.load(season.posterPath) {
//        placeholder(R.drawable.ic_video)
//        error(R.drawable.ic_video)
//      }
    }
  }
}
