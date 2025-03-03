package cloud.app.vvf.ui.media


import android.content.res.Configuration
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import cloud.app.vvf.MainActivityViewModel
import cloud.app.vvf.R
import cloud.app.vvf.base.StateViewModel
import cloud.app.vvf.base.ViewHolderState
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.databinding.ContainerCategoryBinding
import cloud.app.vvf.databinding.ContainerItemBinding
import cloud.app.vvf.databinding.HomePreviewViewpagerBinding
import cloud.app.vvf.databinding.PreviewItemBinding
import cloud.app.vvf.datastore.helper.BookmarkItem
import cloud.app.vvf.datastore.helper.BookmarkItem.Companion.getStringIds
import cloud.app.vvf.ui.media.MediaItemViewHolder.Companion.placeHolder
import cloud.app.vvf.ui.paging.toFlow
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.loadInto
import cloud.app.vvf.utils.navigate
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
    val extensionId: String?,
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
      val mainViewModel by fragment.activityViewModels<MainActivityViewModel>()


      binding.homePreviewBookmark.setOnClickListener {
        val item = items[selectedPosition]
        val status = mainViewModel.getBookmark(item)
        val bookmarks = BookmarkItem.getBookmarkItemSubclasses().toMutableList().apply {
          add("None")
        }
        val selectedIndex =
          if (status == null) (bookmarks.size - 1) else bookmarks.indexOf(status.javaClass.simpleName);
        SelectionDialog.single(
          bookmarks,
          selectedIndex,
          fragment.getString(R.string.add_to_bookmark),
          false
        ).show(fragment.parentFragmentManager) { result ->
          result?.let {
            val selected = it.getIntegerArrayList("selected_items")?.get(0)
            if (selected != null) {
              mainViewModel.addToBookmark(item, bookmarks[selected]);
              val newValue = mainViewModel.getBookmark(item)
              binding.homePreviewBookmark.setCompoundDrawablesWithIntrinsicBounds(
                null,
                ContextCompat.getDrawable(
                  binding.homePreviewBookmark.context,
                  if (newValue == null) R.drawable.ic_add_20dp else R.drawable.ic_bookmark_filled
                ),
                null,
                null
              )
              binding.homePreviewBookmark.text = fragment.getString(getStringIds(newValue))
            }

          }
        }
      }

      binding.homePreviewInfo.setOnClickListener {
        val item = items[selectedPosition]
        fragment.navigate(item, extensionId)
      }


      binding.homePreviewInfo.setOnFocusChangeListener { _, hasFocus ->
        binding.nextFocus?.isFocusable = hasFocus
      }

      binding.homePreviewBookmark.setOnFocusChangeListener { _, hasFocus ->
        binding.prevFocus?.isFocusable = hasFocus
      }

      binding.nextFocus?.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) return@setOnFocusChangeListener
        binding.previewViewpager.setCurrentItem(binding.previewViewpager.currentItem + 1, true)
        binding.homePreviewInfo.requestFocus()
      }

      binding.prevFocus?.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) return@setOnFocusChangeListener
        if (binding.previewViewpager.currentItem <= 0) {
          //Focus the Home item as the default focus will be the header item
          //(fragment.activity as? MainActivity)?.binding?.navRailView?.findViewById<NavigationBarItemView>(R.id.navigation_home)?.requestFocus()
        } else {
          binding.previewViewpager.setCurrentItem(binding.previewViewpager.currentItem - 1, true)
          binding.homePreviewBookmark.requestFocus()
        }
      }
    }

    override fun onViewAttachedToWindow() {
      binding.previewViewpager.registerOnPageChangeCallback(previewCallback)
    }

    override fun onViewDetachedFromWindow() {
      binding.previewViewpager.unregisterOnPageChangeCallback(previewCallback)
    }

    var selectedPosition = 0
    private val previewCallback: ViewPager2.OnPageChangeCallback =
      object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)
          // Handle page change
          selectedPosition = position
          onSelected(selectedPosition)

        }
      }

    fun onSelected(position: Int) {
      val previewAdapter = binding.previewViewpager.adapter as PreviewAdapter
      val item = previewAdapter.getItemOrNull(position) ?: return
      val mainViewModel by fragment.activityViewModels<MainActivityViewModel>()
      val status = mainViewModel.getBookmark(item)
      binding.homePreviewBookmark.setCompoundDrawablesWithIntrinsicBounds(
        null,
        ContextCompat.getDrawable(
          binding.homePreviewBookmark.context,
          if (status == null) R.drawable.ic_add_20dp else R.drawable.ic_bookmark_filled
        ),
        null,
        null
      )
      binding.homePreviewBookmark.text =
        if (status == null) fragment.getString(R.string.none) else fragment.getString(
          getStringIds(status)
        )
    }

    override val clickView: View = binding.homePreviewPlay
    override val transitionView: View = binding.homePreviewPlay

    companion object {
      fun create(
        extensionId: String,
        parent: ViewGroup,
        viewModel: StateViewModel,
        fragment: Fragment,
        listener: MediaItemAdapter.Listener,
      ): MediaContainerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PageView(
          extensionId,
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

      inner class PreviewViewHolder(val viewBinding: ViewBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PreviewItemBinding.inflate(inflater, parent, false)
        return PreviewViewHolder(binding)
      }

      fun getItemOrNull(position: Int): AVPMediaItem? {
        return items.getOrNull(position)
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
    private val extensionId: String?,
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
        extensionId,
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
        extensionId: String?,
        listener: MediaItemAdapter.Listener,
      ): MediaContainerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Category(
          ContainerCategoryBinding.inflate(layoutInflater, parent, false),
          viewModel,
          sharedPool,
          extensionId,
          listener
        )
      }
    }
  }

  class Media(
    val binding: ContainerItemBinding,
    private val extensionId: String?,
    val listener: MediaItemAdapter.Listener,
  ) : MediaContainerViewHolder(binding) {
    override fun bind(container: MediaItemsContainer) {
      val item = (container as? MediaItemsContainer.Item)?.media ?: return
      binding.bind(item)
      binding.more.setOnClickListener { listener.onLongClick(extensionId, item, transitionView) }
    }

    override val transitionView: View = binding.imageView

    companion object {
      fun create(
        parent: ViewGroup,
        extensionId: String?,
        listener: MediaItemAdapter.Listener
      ): MediaContainerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Media(
          ContainerItemBinding.inflate(layoutInflater, parent, false),
          extensionId,
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
