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
  private val fragment: Fragment,
  private val transition: String,
  private val extensionId: String?,
  private val itemWidth: Int? = null,
  private val itemHeight: Int? = null,
  private val listener: Listener = MediaClickListener(fragment.parentFragmentManager)
) : PagingDataAdapter<AVPMediaItem, MediaItemViewHolder>(DiffCallback) {

  interface Listener {
    fun onItemClick(extensionId: String?, item: AVPMediaItem, transitionView: View?)
    fun onItemLongClick(extensionId: String?, item: AVPMediaItem, transitionView: View?): Boolean
    fun onItemFocusChange( extensionId: String?, item: AVPMediaItem, hasFocus: Boolean);
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
    val holder = when (viewType) {
      0 -> MediaItemViewHolder.Media.create(parent)
      1 -> MediaItemViewHolder.Media.create(parent)
      2 -> MediaItemViewHolder.Media.create(parent)
      3 -> MediaItemViewHolder.Actor.create(parent)
      //4 -> MediaItemViewHolder.Stream.create(parent)
      5 -> MediaItemViewHolder.Season.create(parent)
      6 -> MediaItemViewHolder.Trailer.create(parent)
      7 -> MediaItemViewHolder.SeasonLarge.create(parent)
      8 -> MediaItemViewHolder.Media.create(parent)
      9 -> MediaItemViewHolder.Media.create(parent)
      10 -> MediaItemViewHolder.Media.create(parent)
      11 -> MediaItemViewHolder.Track.create(parent)
      12 -> MediaItemViewHolder.Media.create(parent) // Add DownloadItem viewholder
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
      //is AVPMediaItem.StreamItem -> 4
      is AVPMediaItem.SeasonItem -> if (item.season.generalInfo.poster.isNullOrEmpty()) 5 else 7
      is AVPMediaItem.TrailerItem -> 6
      is AVPMediaItem.PlaybackProgress -> 8
      is AVPMediaItem.VideoItem -> 9
      is AVPMediaItem.VideoCollectionItem -> 10
      is AVPMediaItem.TrackItem -> 11
      is AVPMediaItem.DownloadItem -> 12 // Add missing DownloadItem case
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
      listener.onItemClick(extensionId, item, holder.transitionView)
    }
    holder.itemView.setOnLongClickListener {
      listener.onItemLongClick(extensionId, item, holder.transitionView)
    }
    holder.itemView.setOnFocusChangeListener { v, hasFocus ->
      listener.onItemFocusChange(extensionId, item, hasFocus)
    }
  }

  fun withLoaders(): ConcatAdapter {
    val footer = MediaLoadStateAdapter(fragment, false) { retry() }
    val header = MediaLoadStateAdapter(fragment, false) { retry() }
    val empty = MediaEmptyAdapter()
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
      return oldItem.sameAs(newItem)
    }
  }
}
