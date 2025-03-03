package cloud.app.vvf.ui.media

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import cloud.app.vvf.base.BasePagingAdapter
import cloud.app.vvf.base.ViewHolderState
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.utils.observe
import kotlinx.coroutines.Job

class MediaContainerAdapter(
  val extension: Extension<*>,
  val fragment: Fragment,
  id: String,
  val transition: String,
  val listener: Listener = getListener(fragment)
) : BasePagingAdapter<MediaItemsContainer, MediaContainerViewHolder>(fragment, id, DiffCallback) {

  interface Listener : MediaItemAdapter.Listener {
    fun onClick(extensionId: String?, container: MediaItemsContainer, transitionView: View)
    fun onLongClick(
      extensionId: String?,
      container: MediaItemsContainer,
      transitionView: View
    ): Boolean
  }

  companion object {
    fun getListener(fragment: Fragment): Listener {
      val type = fragment.arguments?.getString("itemListener")
      return when (type) {
        "search" -> TODO("not implemented")
        else -> MediaClickListener(fragment.parentFragmentManager)
      }
    }
  }

  fun withLoaders(): ConcatAdapter {
    val footer = MediaContainerLoadingAdapter(fragment) { retry() }
    val header = MediaContainerLoadingAdapter(fragment) { retry() }
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
      listener.onClick(extension.id, items, holder.transitionView)
    }
    clickView.setOnLongClickListener {
      listener.onLongClick(extension.id, items, holder.transitionView)
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
      parent,
      stateViewModel,
      sharedPool,
      extension.id,
      listener
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

