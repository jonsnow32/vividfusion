package cloud.app.avp.ui.media

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import cloud.app.avp.R
import cloud.app.avp.base.StateViewModel
import cloud.app.avp.base.ViewHolderState
import cloud.app.avp.databinding.ContainerCategoryBinding
import cloud.app.avp.databinding.ContainerItemBinding
import cloud.app.avp.ui.media.MediaItemViewHolder.Companion.placeHolder
import cloud.app.avp.ui.paging.toFlow
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.tv.FOCUS_SELF
import cloud.app.avp.utils.tv.setLinearListLayout
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer
import kotlinx.coroutines.launch

sealed class MediaContainerViewHolder(
  binding: ViewBinding,
) : ViewHolderState<MediaContainerViewHolder.SaveStateData>(binding) {

  data class SaveStateData(
    var layoutManagerState: Parcelable? = null,
  )

  abstract fun bind(container: MediaItemsContainer)
  open val clickView = binding.root
  abstract val transitionView: View

  class Category(
    val binding: ContainerCategoryBinding,
    val viewModel: StateViewModel,
    private val sharedPool: RecyclerView.RecycledViewPool,
    private val clientId: String?,
    val listener: MediaItemAdapter.Listener,
  ) : MediaContainerViewHolder(binding) {

    override fun save(): SaveStateData = SaveStateData(
      layoutManagerState = binding.recyclerView.layoutManager?.onSaveInstanceState(),
    )

    override fun restore(state: SaveStateData) {
      binding.recyclerView.layoutManager?.onRestoreInstanceState(state.layoutManagerState)
    }

    override fun bind(container: MediaItemsContainer) {
      val category = container as MediaItemsContainer.Category
      binding.title.text = category.title
      binding.subtitle.text = category.subtitle
      val adapter = MediaItemAdapter(
        listener,
        transitionView.transitionName + category.id,
        clientId,
      )
      binding.recyclerView.adapter = adapter
      binding.recyclerView.setLinearListLayout(
        isHorizontal = true,
        nextLeft = R.id.navView,
        nextRight = FOCUS_SELF,
      )
      binding.recyclerView.setRecycledViewPool(sharedPool)
      binding.more.isVisible = category.more != null

      viewModel.viewModelScope.launch {
        category.more?.toFlow()?.collect { pagingData ->
          adapter.submitData(pagingData)
        }
      }
    }

    val layoutManager get() = binding.recyclerView.layoutManager
    override val clickView: View = binding.titleCard
    override val transitionView: View = binding.titleCard

    companion object {
      fun create(
        parent: ViewGroup,
        viewModel: StateViewModel,
        sharedPool: RecyclerView.RecycledViewPool,
        clientId: String?,
        listener: MediaItemAdapter.Listener,
      ): MediaContainerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Category(
          ContainerCategoryBinding.inflate(layoutInflater, parent, false),
          viewModel,
          sharedPool,
          clientId,
          listener
        )
      }
    }
  }

  class Media(
    val binding: ContainerItemBinding,
    private val clientId: String?,
    val listener: MediaItemAdapter.Listener,
  ) : MediaContainerViewHolder(binding) {
    override fun bind(container: MediaItemsContainer) {
      val item = (container as? MediaItemsContainer.Item)?.media ?: return
      binding.bind(item)
      binding.more.setOnClickListener { listener.onLongClick(clientId, item, transitionView) }
    }

    override val transitionView: View = binding.root

    companion object {
      fun create(
        parent: ViewGroup,
        clientId: String?,
        listener: MediaItemAdapter.Listener
      ): MediaContainerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Media(
          ContainerItemBinding.inflate(layoutInflater, parent, false),
          clientId,
          listener,
        )
      }

      fun ContainerItemBinding.bind(item: AVPMediaItem) {
        title.text = item.title
        subtitle.text = item.subtitle
        subtitle.isVisible = item.subtitle.isNullOrBlank().not()

        item.poster.loadInto(imageView, item.placeHolder())
      }
    }
  }
}
