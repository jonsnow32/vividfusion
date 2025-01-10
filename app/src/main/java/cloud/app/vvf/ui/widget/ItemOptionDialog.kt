package cloud.app.vvf.ui.widget

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.MainActivityViewModel
import cloud.app.vvf.R
import cloud.app.vvf.databinding.DialogMediaItemBinding
import cloud.app.vvf.databinding.ItemDialogButtonBinding
import cloud.app.vvf.databinding.ItemDialogButtonLoadingBinding
import cloud.app.vvf.ui.detail.show.ShowFragment
import cloud.app.vvf.ui.main.browse.BrowseFragment
import cloud.app.vvf.ui.main.browse.BrowseViewModel
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.loadWith
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.shareItem
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ImageHolder
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.helper.BookmarkItem
import cloud.app.vvf.utils.showBottomDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ItemOptionDialog : BottomSheetDialogFragment() {
  private var binding by autoCleared<DialogMediaItemBinding>()
  private val viewModel by viewModels<ItemOptionViewModel>()
  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val item by lazy { args.getSerialized<AVPMediaItem>("item")!! }

  companion object {
    fun newInstance(
      clientId: String, item: AVPMediaItem
    ) = ItemOptionDialog().apply {
      arguments = Bundle().apply {
        putString("clientId", clientId)
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
      item.backdrop.loadWith(binding.imageView)
      viewModel.getItemDetails(item)
    }
    binding.recyclerView.adapter =
      ConcatAdapter(ActionAdapter(getActions(item)), LoadingAdapter())

    observe(viewModel.detailItem) {
      if (it != null) {
        binding.recyclerView.adapter = ActionAdapter(getActions(it))
      }
    }
    observe(viewModel.knowFors) {
      if (it != null) {
        val browseViewModel by activityViewModels<BrowseViewModel>()
        browseViewModel.moreFlow = it.more
        browseViewModel.title = it.title
        requireActivity().navigate(BrowseFragment())
      }
    }
  }

  private fun getActions(item: AVPMediaItem): List<ItemAction> = when (item) {
    is AVPMediaItem.ShowItem -> {
      listOfNotNull(
        ItemAction.Resource(R.drawable.ic_more_horiz, R.string.action_show_details) {
          val movieFragment = ShowFragment();
          val bundle = Bundle()
          bundle.putString("clientId", clientId)
          bundle.putSerialized("mediaItem", item)
          movieFragment.arguments = bundle
          requireActivity().navigate(
            movieFragment
          )
        }, ItemAction.Resource(R.drawable.ic_bookmark_outline, R.string.bookmarks) {
          val status = viewModel.getBookmark(item)
          val bookmarks = BookmarkItem.getBookmarkItemSubclasses().toMutableList().apply {
            add("None")
          }
          val selectedIndex = if(status == null) (bookmarks.size - 1)  else  bookmarks.indexOf(status.javaClass.simpleName);
          requireActivity().showBottomDialog(
            bookmarks,
            selectedIndex,
            getString(R.string.add_to_bookmark),
            false,
            {},
            { selected ->
              viewModel.addToBookmark(item, bookmarks.get(selected));
            })
        })
    }

    is AVPMediaItem.EpisodeItem,
    is AVPMediaItem.MovieItem -> {
      listOfNotNull(ItemAction.Resource(R.drawable.play_arrow_24dp, R.string.play_now) {
        //playerViewModel.play(clientId, item, 0)
      })
    }

    is AVPMediaItem.ActorItem -> {
      listOfNotNull(ItemAction.Resource(R.drawable.playlist_play_24dp, R.string.known_for) {
        viewModel.getKnowFor(clientId, item)
      })
    }

    is AVPMediaItem.SeasonItem,
    is AVPMediaItem.StreamItem -> listOf()

    else -> listOf()
  } + listOfNotNull(
    ItemAction.Resource(
      R.drawable.share_24dp,
      R.string.share
    ) {
      requireActivity().shareItem(item)
    },

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
  )

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
      RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val binding = ItemDialogButtonLoadingBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
      )
      return ViewHolder(binding)
    }

    override fun getItemCount() = 1
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
  }
}
