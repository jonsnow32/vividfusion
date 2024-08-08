package cloud.app.avp.ui.main.media

import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow
import java.lang.ref.WeakReference

class MediaContainerAdapter(
  val fragment: Fragment,
  val transition: String,
  val listener: Listener = getListener(fragment)
) : PagingDataAdapter<MediaItemsContainer, MediaContainerViewHolder>(DiffCallback) {

  interface Listener : MediaItemAdapter.Listener {
    fun onClick(clientId: String?, container: MediaItemsContainer, transitionView: View)
    fun onLongClick(
      clientId: String?, container: MediaItemsContainer, transitionView: View
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

  var clientId: String? = null
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
      if (it.refresh == LoadState.Loading) clearState()
    }
  }

  private val stateViewModel: StateViewModel by fragment.viewModels()

  class StateViewModel : ViewModel() {
    val layoutManagerStates = hashMapOf<Int, Parcelable?>()
    val visibleScrollableViews = hashMapOf<Int, WeakReference<MediaContainerViewHolder.Category>>()
    val pagers = hashMapOf<Int, WeakReference<Flow<PagingData<AVPMediaItem>>>?>()
  }

  private fun clearState() {
    stateViewModel.layoutManagerStates.clear()
    stateViewModel.visibleScrollableViews.clear()
  }

  private fun saveState() {
    stateViewModel.visibleScrollableViews.values.forEach { item ->
      item.get()?.let { saveScrollState(it) }
    }
    stateViewModel.visibleScrollableViews.clear()
  }

  override fun onViewRecycled(holder: MediaContainerViewHolder) {
    super.onViewRecycled(holder)
    if (holder is MediaContainerViewHolder.Category) saveScrollState(holder) {
      stateViewModel.visibleScrollableViews.remove(holder.bindingAdapterPosition)
    }
  }

  private fun saveScrollState(
    holder: MediaContainerViewHolder.Category,
    block: ((MediaContainerViewHolder.Category) -> Unit)? = null
  ) {
    val layoutManagerStates = stateViewModel.layoutManagerStates
    layoutManagerStates[holder.bindingAdapterPosition] =
      holder.layoutManager?.onSaveInstanceState()
    block?.invoke(holder)
  }

  suspend fun submit(pagingData: PagingData<MediaItemsContainer>?) {
    saveState()
    submitData(pagingData ?: PagingData.empty())
  }

  // Binding
  override fun onBindViewHolder(holder: MediaContainerViewHolder, position: Int) {
    val items = getItem(position) ?: return
    holder.transitionView.transitionName = (transition + items.id).hashCode().toString()
    holder.bind(items)
    val clickView = holder.clickView
    clickView.setOnClickListener {
      listener.onClick(clientId, items, holder.transitionView)
    }
    clickView.setOnLongClickListener {
      listener.onLongClick(clientId, items, holder.transitionView)
      true
    }
  }

  override fun getItemViewType(position: Int): Int {
    val item = getItem(position) ?: return 0
    return when (item) {
      is MediaItemsContainer.Category -> 0
      is MediaItemsContainer.Item -> 1
    }
  }

  private val sharedPool = RecycledViewPool()
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
    0 -> MediaContainerViewHolder.Category.create(
      parent,
      stateViewModel,
      sharedPool,
      clientId,
      listener
    )

    else -> MediaContainerViewHolder.Media.create(parent, clientId, listener)
  }
}

