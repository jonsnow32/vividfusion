package cloud.app.vvf.ui.media

import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem

class MediaItemAdapter(
  private val listener: Listener,
  private val transition: String,
  private val extensionId: String?,
  private val itemWidth: Int? = null,
  private val itemHeight: Int? = null
) : PagingDataAdapter<AVPMediaItem, MediaItemViewHolder>(DiffCallback) {

  interface Listener {
    fun onClick(extensionId: String?, item: AVPMediaItem, transitionView: View?)
    fun onLongClick(extensionId: String?, item: AVPMediaItem, transitionView: View?): Boolean
    fun onFocusChange(extensionId: String?, item: AVPMediaItem, hasFocus: Boolean);
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
    val holder = when (viewType) {
      0 -> MediaItemViewHolder.Movie.create(parent)
      1 -> MediaItemViewHolder.Show.create(parent)
      2 -> MediaItemViewHolder.Episode.create(parent)
      3 -> MediaItemViewHolder.Actor.create(parent)
      4 -> MediaItemViewHolder.Stream.create(parent)
      5 -> MediaItemViewHolder.Season.create(parent)
      6 -> MediaItemViewHolder.Trailer.create(parent)
      7 -> MediaItemViewHolder.SeasonLarge.create(parent)
      else -> throw IllegalArgumentException("Invalid view type")
    }
    return holder
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position) ?: return 0
    return when (item) {
      is AVPMediaItem.MovieItem -> 0
      is AVPMediaItem.ShowItem -> 1
      is AVPMediaItem.EpisodeItem -> 2
      is AVPMediaItem.ActorItem -> 3
      is AVPMediaItem.StreamItem -> 4
      is AVPMediaItem.SeasonItem -> if (item.season.generalInfo.poster.isNullOrEmpty()) 5 else 7
      is AVPMediaItem.TrailerItem -> 6
    }
  }


  override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
    val item = getItem(position) ?: return

    if (itemWidth != null && itemHeight != null) {
      val layoutParams = holder.itemView.layoutParams
      // Change width and height dynamically, for example:
      layoutParams.width = itemWidth
      layoutParams.height = itemHeight
      // Apply new layout parameters
      holder.itemView.layoutParams = layoutParams

      var cover = holder.itemView.findViewById<CardView?>(R.id.cover)
      cover?.layoutParams?.width = itemWidth
      cover?.layoutParams?.height = itemWidth * 3 / 2

    }

    holder.transitionView.transitionName = (transition + item.id).hashCode().toString()
    holder.bind(item)
    holder.itemView.setOnClickListener {
      listener.onClick(extensionId, item, holder.transitionView)
    }
    holder.itemView.setOnLongClickListener {
      listener.onLongClick(extensionId, item, holder.transitionView)
    }
    holder.itemView.setOnFocusChangeListener { v, hasFocus ->
      listener.onFocusChange(extensionId, item, hasFocus)
    }
  }

  fun withLoaders(): ConcatAdapter {
    val footer = MediaContainerLoadingAdapter(listener as Fragment) { retry() }
    val header = MediaContainerLoadingAdapter(listener) { retry() }
    val empty = MediaContainerEmptyAdapter()
    addLoadStateListener { loadStates ->
      empty.loadState = if (loadStates.refresh is LoadState.NotLoading && itemCount == 0)
        LoadState.Loading
      else LoadState.NotLoading(false)
      header.loadState = loadStates.refresh
      footer.loadState = loadStates.append
    }
    return ConcatAdapter(empty, header, this, footer)
  }

  suspend fun submit(pagingData: PagingData<AVPMediaItem>?) {
    submitData(pagingData ?: PagingData.empty())
  }

  object DiffCallback : DiffUtil.ItemCallback<AVPMediaItem>() {
    override fun areItemsTheSame(
      oldItem: AVPMediaItem,
      newItem: AVPMediaItem
    ): Boolean {
      return oldItem.sameAs(newItem)
    }

    override fun areContentsTheSame(
      oldItem: AVPMediaItem,
      newItem: AVPMediaItem
    ): Boolean {
      return oldItem == newItem
    }
  }
}
