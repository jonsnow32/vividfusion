package cloud.app.vvf.ui.widget.dialog.itemOption

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.MainActivityViewModel
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ImageHolder
import cloud.app.vvf.databinding.DialogMediaItemBinding
import cloud.app.vvf.databinding.ItemDialogButtonBinding
import cloud.app.vvf.databinding.ItemDialogButtonLoadingBinding
import cloud.app.vvf.datastore.app.helper.BookmarkItem
import cloud.app.vvf.datastore.app.helper.BookmarkItem.Companion.getStringIds
import cloud.app.vvf.ui.detail.show.ShowFragment
import cloud.app.vvf.ui.main.browse.BrowseFragment
import cloud.app.vvf.ui.main.browse.BrowseViewModel
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.loadWith
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.shareItem
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ItemOptionDialog : DockingDialog(0.4f) {
  private var binding by autoCleared<DialogMediaItemBinding>()
  private val viewModel by viewModels<ItemOptionViewModel>()
  private val args by lazy { requireArguments() }
  private val extensionId by lazy { args.getString("extensionId")!! }
  private val item by lazy { args.getSerialized<AVPMediaItem>("item")!! }

  companion object {
    fun newInstance(
      extensionId: String, item: AVPMediaItem
    ) = ItemOptionDialog().apply {
      arguments = Bundle().apply {
        putString("extensionId", extensionId)
        putSerialized("item", item)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogMediaItemBinding.inflate(inflater, container, false)
    return binding.root
  }

  @SuppressLint("UseCompatLoadingForDrawables")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    binding.itemContainer.run {
      if (item.backdrop != null)
        item.backdrop.loadWith(binding.imageView)
      binding.itemContainer.isGone = item.backdrop == null
      viewModel.getItemDetails(item, extensionId)
    }

    binding.recyclerView.adapter =
      ConcatAdapter(ActionAdapter(getActions(item)), LoadingAdapter())


    observe(viewModel.loading) {
      if (!it) {
        binding.recyclerView.adapter = ActionAdapter(getActions(viewModel.detailItem.value!!))
      }
    }

    observe(viewModel.knowFors) {
      if (it != null) {
        val browseViewModel by activityViewModels<BrowseViewModel>()
        browseViewModel.moreFlow = it.more
        browseViewModel.title = it.title

        val bundle = Bundle()
        bundle.putString("extensionId", extensionId)

        val browseFragment = BrowseFragment();
        browseFragment.arguments = bundle
        requireActivity().navigate(browseFragment)
      }
    }
  }

  fun getBookmarkAction(item: AVPMediaItem): ItemAction {
    val bookmarkItem = viewModel.bookmarkStatus.value;
    return ItemAction.Resource(
      if (bookmarkItem == null) R.drawable.ic_bookmark_outline else R.drawable.ic_bookmark_filled,
      getStringIds(bookmarkItem)
    ) {
      val mainViewModel by activityViewModels<MainActivityViewModel>()
      val status = mainViewModel.getBookmark(item)
      val bookmarks = BookmarkItem.getBookmarkItemSubclasses().toMutableList().apply {
        add("None")
      }
      val selectedIndex =
        if (status == null) (bookmarks.size - 1) else bookmarks.indexOf(status.javaClass.simpleName);
      SelectionDialog.Companion.single(
        bookmarks,
        selectedIndex,
        getString(R.string.add_to_bookmark), false
      ).show(parentFragmentManager)
      { result ->
        result?.let {
          result.getIntegerArrayList("selected_items")?.get(0)?.let {
            mainViewModel.addToBookmark(item, bookmarks[it])
          }
        }
      }
    }
  }

  private fun getActions(item: AVPMediaItem): List<ItemAction> = when (item) {
    is AVPMediaItem.ShowItem -> {
      listOfNotNull(
        ItemAction.Resource(R.drawable.ic_more_horiz, R.string.action_show_details) {
          val movieFragment = ShowFragment();
          val bundle = Bundle()
          bundle.putString("extensionId", extensionId)
          bundle.putSerialized("mediaItem", item)
          movieFragment.arguments = bundle
          requireActivity().navigate(
            movieFragment
          )
        }, getBookmarkAction(item)
      )
    }

    is AVPMediaItem.EpisodeItem -> {
      listOfNotNull(ItemAction.Resource(R.drawable.play_arrow_24dp, R.string.play_now) {
        //playerViewModel.play(extensionId, item, 0)
      }, ItemAction.Resource(R.drawable.ic_more_horiz, R.string.action_goto_show) {
        val movieFragment = ShowFragment();
        val bundle = Bundle()
        bundle.putString("extensionId", extensionId)
        bundle.putSerialized("mediaItem", item.seasonItem.showItem)
        movieFragment.arguments = bundle
        requireActivity().navigate(
          movieFragment
        )
      }, getBookmarkAction(item))
    }

    is AVPMediaItem.MovieItem -> {
      listOfNotNull(ItemAction.Resource(R.drawable.play_arrow_24dp, R.string.play_now) {
        //playerViewModel.play(extensionId, item, 0)
      }, getBookmarkAction(item))
    }

    is AVPMediaItem.ActorItem -> {
      listOfNotNull(ItemAction.Resource(R.drawable.playlist_play_24dp, R.string.known_for) {
        viewModel.getKnowFor(extensionId, item)
      })
    }

    is AVPMediaItem.TrackItem -> {
      listOfNotNull(ItemAction.Resource(R.drawable.ic_delete, R.string.action_delete) {
        viewModel.deleteItem(requireActivity(), item)
      }, ItemAction.Resource(R.drawable.edit_24dp, R.string.action_rename) {
        viewModel.renameItem(requireActivity(), item, "")
      })
    }

    is AVPMediaItem.VideoItem -> {
      listOfNotNull(ItemAction.Resource(R.drawable.ic_delete, R.string.action_delete) {
        viewModel.deleteItem(requireActivity(), item)
      })
    }

    else -> listOf()
  } + listOfNotNull(
    ItemAction.Resource(
      R.drawable.share_24dp,
      R.string.share
    ) {
      requireActivity().shareItem(item)
    }, getFavoritesAction(item)
  )

  private fun getFavoritesAction(item: AVPMediaItem): ItemAction? {
    return if (item is AVPMediaItem.MovieItem
      || item is AVPMediaItem.ShowItem
      || item is AVPMediaItem.ActorItem
      || item is AVPMediaItem.VideoItem
      || item is AVPMediaItem.VideoCollectionItem
      || item is AVPMediaItem.TrackItem
    ) {
      if (!viewModel.favoriteStatus.value)
        ItemAction.Resource(
          R.drawable.favorite_border_24dp,
          R.string.action_add_to_favorites
        ) {
          viewModel.toggleFavoriteStatus {
            val messageResId = if (viewModel.favoriteStatus.value) {
              R.string.favorite_added
            } else {
              R.string.favorite_removed
            }
            createSnack(getString(messageResId, item.title))
          }
        } else
        ItemAction.Resource(
          R.drawable.favorite_24dp,
          R.string.action_remove_from_favorites
        ) {
          viewModel.toggleFavoriteStatus {
            val messageResId = if (viewModel.favoriteStatus.value) {
              R.string.favorite_added
            } else {
              R.string.favorite_removed
            }
            createSnack(getString(messageResId, item.title))
          }
        }
    } else
      null
  }

  sealed class ItemAction {
    abstract val action: () -> Unit

    data class Resource(
      val resId: Int, val stringId: Int, override val action: () -> Unit
    ) : ItemAction()

    data class Custom(
      val image: ImageHolder?,
      val placeholder: Int,
      val title: String,
      override val action: () -> Unit
    ) : ItemAction()
  }

  inner class ActionAdapter(var list: List<ItemAction>) :
    RecyclerView.Adapter<ActionAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemDialogButtonBinding) :
      RecyclerView.ViewHolder(binding.root) {
      init {
        binding.root.setOnClickListener {
          list[bindingAdapterPosition].action()
          dismiss()
        }
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val binding = ItemDialogButtonBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
      )
      return ViewHolder(binding)
    }

    override fun getItemCount() = list.count()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val action = list[position]
      val binding = holder.binding
      val colorState = ColorStateList.valueOf(
        binding.root.context.getColor(R.color.button_item)
      )
      when (action) {
        is ItemAction.Resource -> {
          binding.textView.setText(action.stringId)
          binding.imageView.setImageResource(action.resId)
          binding.imageView.imageTintList = colorState
        }

        is ItemAction.Custom -> {
          binding.textView.text = action.title
          action.image.loadWith(binding.root) {
            if (it == null) {
              binding.imageView.imageTintList = colorState
              binding.imageView.setImageResource(action.placeholder)
            } else binding.imageView.setImageDrawable(it)
          }
        }
      }
    }
  }

  class LoadingAdapter : RecyclerView.Adapter<LoadingAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemDialogButtonLoadingBinding) :
      RecyclerView.ViewHolder(binding.root) {

      fun bind() {
        // Simulate a delay effect for the loading state
        binding.progressBar.visibility = View.GONE // Initially hidden
        binding.root.postDelayed({
          binding.progressBar.visibility = View.VISIBLE // Show after delay
        }, 500) // 500ms delay, adjust as needed
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val binding = ItemDialogButtonLoadingBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
      )
      return ViewHolder(binding)
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      holder.bind() // Trigger the delay effect on bind
    }
  }
}
