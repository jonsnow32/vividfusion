package cloud.app.vvf.ui.media

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import cloud.app.vvf.base.BasePagingAdapter
import cloud.app.vvf.base.ViewHolderState
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.MediaItemsContainer

class MediaContainerAdapter(
  val extension: Extension<*>,
  val fragment: Fragment,
  id: String,
  val transition: String,
  val listener: Listener = MediaClickListener(fragment.parentFragmentManager)
) : BasePagingAdapter<MediaItemsContainer, MediaContainerViewHolder>(fragment, id, DiffCallback) {

  interface Listener : MediaItemAdapter.Listener {
    fun onContainerClick(extensionId: String?, container: MediaItemsContainer, holder: MediaContainerViewHolder)
    fun onContainerLongClick(
      extensionId: String?,
      container: MediaItemsContainer,
      holder: MediaContainerViewHolder
    ): Boolean
  }

  fun withLoaders(): ConcatAdapter {
    val footer = MediaLoadStateAdapter(fragment) { retry() }
    val header = MediaLoadStateAdapter(fragment) { retry() }
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

  object DiffCallback : DiffUtil.ItemCallback<MediaItemsContainer>() {
    override fun areItemsTheSame(
      oldItem: MediaItemsContainer,
      newItem: MediaItemsContainer
    ): Boolean {
      return oldItem.sameAs(newItem)
    }

    override fun areContentsTheSame(
      oldItem: MediaItemsContainer,
      newItem: MediaItemsContainer
    ): Boolean {
      return oldItem == newItem
    }
  }


  //Nested RecyclerView State Management

  init {
    addLoadStateListener {
      if (it.refresh == LoadState.Loading) clear()
    }
  }


  override fun bindContent(h: ViewHolderState<MediaContainerViewHolder>, position: Int) {
    val items = getItem(position) ?: return
    val holder = h as MediaContainerViewHolder

    holder.transitionView.transitionName = (transition + items.id).hashCode().toString()
    holder.bind(items)
    val clickView = holder.clickView
    clickView.setOnClickListener {
      listener.onContainerClick(extension.id, items, holder, )
    }
    clickView.setOnLongClickListener {
      listener.onContainerLongClick(extension.id, items, holder)
      true
    }
  }


  suspend fun submit(pagingData: PagingData<MediaItemsContainer>?) {
    submitData(pagingData ?: PagingData.empty())
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position) ?: return 0
    return when (item) {
      is MediaItemsContainer.Category -> 0
      is MediaItemsContainer.Item -> 1
      is MediaItemsContainer.PageView -> 2
    }
  }

  private val sharedPool = RecycledViewPool()
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
    0 -> MediaContainerViewHolder.Category.create(
      fragment,
      parent,
      stateViewModel,
      sharedPool,
      extension.id,
    ) as ViewHolderState<MediaContainerViewHolder>

    2 -> MediaContainerViewHolder.PageView.create(
      extension.id,
      parent,
      stateViewModel,
      fragment,
      listener
    ) as ViewHolderState<MediaContainerViewHolder>

    else -> MediaContainerViewHolder.Media.create(
      parent,
      extension.id,
      listener
    ) as ViewHolderState<MediaContainerViewHolder>
  }
}

