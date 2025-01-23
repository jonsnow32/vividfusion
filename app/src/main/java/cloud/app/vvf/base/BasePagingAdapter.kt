package cloud.app.vvf.base

import android.view.View
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding


class StateViewModel : ViewModel() {
  val layoutManagerStates = hashMapOf<String, HashMap<Int, Any?>>()
}


/**
 * A base class for ViewHolder that supports saving and restoring state.
 * This class can be extended to create ViewHolders that can save their state when they are recycled and restore it when they are re-bound.
 *
 * @param T The type of the state object.
 * @param viewBinding The ViewBinding associated with the ViewHolder.
 *
 * @property viewBinding The ViewBinding associated with the ViewHolder.
 */
open class ViewHolderState<T>(val viewBinding: ViewBinding) :
  RecyclerView.ViewHolder(viewBinding.root) {
  open fun save(): T? = null
  open fun restore(state: T) = Unit
  open fun onViewAttachedToWindow() = Unit
  open fun onViewDetachedFromWindow() = Unit
  open fun onViewRecycled() = Unit
}


/**
 * Base class for PagingDataAdapter that provides state management and other functionalities.
 *
 * @param T The type of the items in the adapter.
 * @param S The type of the state to be saved and restored for each item.
 * @param rootFragment The fragment that owns this adapter. Used to access the StateViewModel.
 * @param id An optional unique ID for this adapter. Used to differentiate state management
 *           between multiple adapters in the same fragment. Defaults to 0.
 * @param diffCallback The DiffUtil.ItemCallback to be used for calculating differences between items.
 *                     Defaults to a basic BaseDiffCallback.
 */
abstract class BasePagingAdapter<T : Any, S : Any>(
  rootFragment: Fragment,
  val id: String? = null,
  diffCallback: DiffUtil.ItemCallback<T> = BaseDiffCallback()
) : PagingDataAdapter<T, ViewHolderState<S>>(
  diffCallback
) {
  protected val stateViewModel: StateViewModel by rootFragment.viewModels()

  abstract fun bindContent(holder: ViewHolderState<S>, position: Int)

  override fun onBindViewHolder(
    holder: ViewHolderState<S>,
    position: Int,
    payloads: MutableList<Any>
  ) {
    if (payloads.isEmpty()) {
      super.onBindViewHolder(holder, position, payloads)
      return
    }
    bindContent(holder, position)
  }

  final override fun onBindViewHolder(holder: ViewHolderState<S>, position: Int) {
    bindContent(holder, position)
    getState(holder)?.let { state ->
      holder.restore(state)
    }
  }

  override fun onViewAttachedToWindow(holder: ViewHolderState<S>) {
    holder.onViewAttachedToWindow()
  }

  override fun onViewDetachedFromWindow(holder: ViewHolderState<S>) {
    holder.onViewDetachedFromWindow()
  }

  final override fun onViewRecycled(holder: ViewHolderState<S>) {
    setState(holder)
    holder.onViewRecycled()
    super.onViewRecycled(holder)
  }

  @Suppress("UNCHECKED_CAST")
  fun save(recyclerView: RecyclerView) {
    for (child in recyclerView.children) {
      val holder =
        recyclerView.findContainingViewHolder(child) as? ViewHolderState<S> ?: continue
      setState(holder)
    }
  }

  fun clear() {
    stateViewModel.layoutManagerStates[id]?.clear()
  }

  @Suppress("UNCHECKED_CAST")
  private fun getState(holder: ViewHolderState<S>): S? =
    stateViewModel.layoutManagerStates[id]?.get(holder.absoluteAdapterPosition) as? S

  private fun setState(holder: ViewHolderState<S>) {
    if (id == null) return

    if (!stateViewModel.layoutManagerStates.contains(id)) {
      stateViewModel.layoutManagerStates[id] = HashMap()
    }

    stateViewModel.layoutManagerStates[id]?.let { map ->
      map[holder.absoluteAdapterPosition] = holder.save()
    }
  }

  private val attachListener = object : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) = Unit
    override fun onViewDetachedFromWindow(v: View) {
      if (v !is RecyclerView) return
      save(v)
    }
  }

  final override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    recyclerView.addOnAttachStateChangeListener(attachListener)
    super.onAttachedToRecyclerView(recyclerView)
  }

  final override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    recyclerView.removeOnAttachStateChangeListener(attachListener)
    super.onDetachedFromRecyclerView(recyclerView)
  }
}

abstract class NoStateAdapter<T : Any>(fragment: Fragment) :
  BasePagingAdapter<T, Any>(fragment, null)

class BaseDiffCallback<T : Any>(
  val itemSame: (T, T) -> Boolean = { a, b -> a.hashCode() == b.hashCode() },
  val contentSame: (T, T) -> Boolean = { a, b -> a.hashCode() == b.hashCode() }
) : DiffUtil.ItemCallback<T>() {
  override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = itemSame(oldItem, newItem)
  override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = contentSame(oldItem, newItem)
  override fun getChangePayload(oldItem: T, newItem: T): Any = Any()
}
