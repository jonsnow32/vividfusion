package cloud.app.avp.ui.main.media

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cloud.app.common.models.AVPMediaItem
import kotlinx.coroutines.flow.Flow

class MediaItemAdapter(
  private val listener: Listener,
  private val transition: String,
  private val clientId: String?,
) : PagingDataAdapter<AVPMediaItem, MediaItemViewHolder>(DiffCallback) {

  interface Listener {
    fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?)
    fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean
  }


  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
    return when (viewType) {
      0 -> MediaItemViewHolder.Movie.create(parent)
      1 -> MediaItemViewHolder.Show.create(parent)
      2 -> MediaItemViewHolder.Episode.create(parent)
      3 -> MediaItemViewHolder.Actor.create(parent)
      else -> throw IllegalArgumentException("Invalid view type")
    }
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position) ?: return 0
    return when (item) {
      is AVPMediaItem.MovieItem -> 0
      is AVPMediaItem.ShowItem -> 1
      is AVPMediaItem.EpisodeItem -> 2
      is AVPMediaItem.ActorItem -> 3
    }
  }


  override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
    val item = getItem(position) ?: return
    holder.transitionView.transitionName = (transition + item.id).hashCode().toString()
    holder.bind(item)
    holder.itemView.setOnClickListener {
      listener.onClick(clientId, item, holder.transitionView)
    }
    holder.itemView.setOnLongClickListener {
      listener.onLongClick(clientId, item, holder.transitionView)
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
