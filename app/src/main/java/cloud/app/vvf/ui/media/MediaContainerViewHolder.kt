package cloud.app.vvf.ui.media


import android.content.res.Configuration
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import cloud.app.vvf.R
import cloud.app.vvf.base.StateViewModel
import cloud.app.vvf.base.ViewHolderState
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.databinding.ContainerCategoryBinding
import cloud.app.vvf.databinding.ContainerItemBinding
import cloud.app.vvf.databinding.HomePreviewViewpagerBinding
import cloud.app.vvf.databinding.PreviewItemBinding
import cloud.app.vvf.ui.media.MediaItemViewHolder.Companion.placeHolder
import cloud.app.vvf.ui.paging.toFlow
import cloud.app.vvf.utils.loadInto
import cloud.app.vvf.utils.setTextWithVisibility
import cloud.app.vvf.utils.tv.FOCUS_SELF
import cloud.app.vvf.utils.tv.setLinearListLayout
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

  class PageView(
    val binding: HomePreviewViewpagerBinding,
    val viewModel: StateViewModel,
    val fragment: Fragment,
    val listener: MediaItemAdapter.Listener
  ) : MediaContainerViewHolder(binding) {


    override fun bind(container: MediaItemsContainer) {
      binding.previewViewpager.setPageTransformer(HeaderPageTransformer())
      val items = (container as MediaItemsContainer.PageView).items
      val adapter = PreviewAdapter(items)
      binding.previewViewpager.adapter = adapter
      binding.previewViewpager.registerOnPageChangeCallback(previewCallback)
    }

    override fun onViewAttachedToWindow() {
      binding.previewViewpager.registerOnPageChangeCallback(previewCallback)
    }
    override fun onViewDetachedFromWindow() {
      binding.previewViewpager.unregisterOnPageChangeCallback(previewCallback)
    }

    private val previewCallback: ViewPager2.OnPageChangeCallback =
      object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)
          // Handle page change
          println("Page selected: $position")
        }
      }
    override val clickView: View = binding.homePreviewPlay
    override val transitionView: View = binding.homePreviewPlay

    companion object {
      fun create(
        parent: ViewGroup,
        viewModel: StateViewModel,
        fragment: Fragment,
        listener: MediaItemAdapter.Listener,
      ): MediaContainerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PageView(
          HomePreviewViewpagerBinding.inflate(layoutInflater, parent, false),
          viewModel,
          fragment,
          listener
        )
      }
    }

    inner class HeaderPageTransformer : ViewPager2.PageTransformer {
      override fun transformPage(page: View, position: Float) {
        val padding = (-position * page.width / 2).toInt()
        page.setPadding(
          padding, 0,
          -padding, 0
        )
      }
    }

    class PreviewAdapter(
      private val items: List<AVPMediaItem>,
    ) : RecyclerView.Adapter<PreviewAdapter.PreviewViewHolder>() {

      inner class PreviewViewHolder(val viewBinding: ViewBinding) : RecyclerView.ViewHolder(viewBinding.root)

      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =  PreviewItemBinding.inflate(inflater, parent, false)
        return PreviewViewHolder(binding)
      }

      override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.viewBinding
        val posterUrl = if (isHorizontal(holder)) item.backdrop else item.poster

        when (binding) {
          is PreviewItemBinding -> {
            posterUrl.loadInto(binding.previewImage, item.placeHolder())
            binding.tags.apply {
              text = "sample" //item.tags?.joinToString(" • ") ?: ""
              maxLines = 2
            }
            binding.title.setTextWithVisibility(item.title)
          }
        }
      }

      override fun getItemCount(): Int = items.size

      private fun isHorizontal(holder: PreviewViewHolder): Boolean {
        return holder.viewBinding.root.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
      }
    }
  }

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

    override val transitionView: View = binding.imageView

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
