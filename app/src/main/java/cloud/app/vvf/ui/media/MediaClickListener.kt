package cloud.app.vvf.ui.media

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import cloud.app.vvf.VVFApplication.Companion.noClient
import cloud.app.vvf.R
import cloud.app.vvf.ui.main.browse.BrowseFragment
import cloud.app.vvf.ui.main.browse.BrowseViewModel
import cloud.app.vvf.ui.detail.movie.MovieFragment
import cloud.app.vvf.ui.detail.show.ShowFragment
import cloud.app.vvf.ui.detail.show.season.SeasonFragment
import cloud.app.vvf.ui.widget.dialog.itemOption.ItemOptionDialog
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.tryWith
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.ui.stream.StreamFragment

class MediaClickListener(
  private val fragmentManager: FragmentManager,
  private val afterOpening: (() -> Unit)? = null,
  private val afterFocusChange: ((AVPMediaItem, Boolean) -> Unit)? = null,
) : MediaContainerAdapter.Listener {

  val fragment get() = fragmentManager.findFragmentById(R.id.navHostFragment)!!
  private fun noClient() = fragment.createSnack(fragment.requireContext().noClient())

  override fun onItemClick(
    extensionId: String?, item: AVPMediaItem, transitionView: View?
  ) {
    val bundle = Bundle()
    bundle.putString("extensionId", extensionId)
    bundle.putSerialized("mediaItem", item)

    when (item) {
      is AVPMediaItem.MovieItem -> {
        val movieFragment = MovieFragment()
        movieFragment.arguments = bundle;
        fragment.navigate(movieFragment, transitionView)
      }

      is AVPMediaItem.ShowItem -> {
        val showFragment = ShowFragment()
        showFragment.arguments = bundle;
        fragment.navigate(showFragment, transitionView)
      }

      is AVPMediaItem.SeasonItem -> {
        val seasonFragment = SeasonFragment();
        seasonFragment.arguments = bundle;
        fragment.navigate(
          seasonFragment,
          transitionView
        )
      }

      is AVPMediaItem.ActorItem -> {
        onItemLongClick(extensionId, item, transitionView)
      }

      is AVPMediaItem.EpisodeItem,
      is AVPMediaItem.StreamItem -> {
        fragment.createSnack(fragment.getString(R.string.not_implemented))
      }

      is AVPMediaItem.TrailerItem -> TODO()
      is AVPMediaItem.PlaybackProgressItem -> onItemClick(extensionId, item.item, transitionView)
    }
  }

  override fun onItemLongClick(
    extensionId: String?, item: AVPMediaItem, transitionView: View?
  ): Boolean {
    extensionId?.let {
      ItemOptionDialog.newInstance(extensionId, item)
        .show(fragmentManager)
      return true
    }
    return false
  }

  override fun onItemFocusChange(extensionId: String?, item: AVPMediaItem, hasFocus: Boolean) {
    afterFocusChange?.invoke(item, hasFocus)
  }

  override fun onContainerClick(extensionId: String?, container: MediaItemsContainer, holder: MediaContainerViewHolder) {
    when (container) {
      is MediaItemsContainer.Item -> tryWith {
        onItemClick(
          extensionId,
          container.media,
          holder.transitionView
        )
      }

      is MediaItemsContainer.Category -> openContainer(
        extensionId,
        container.title,
        holder.transitionView,
        container.more
      )

      is MediaItemsContainer.PageView -> {
        val extras = holder.extras
        val selectedPosition = extras?.getInt("selected_position") ?: 0
        fragment.navigate(StreamFragment.newInstance(container.items[selectedPosition]))
      }
    }
  }

  override fun onContainerLongClick(
    extensionId: String?, container: MediaItemsContainer, holder: MediaContainerViewHolder
  ) = when (container) {
    is MediaItemsContainer.Item -> onItemLongClick(extensionId, container.media, holder.transitionView)
    else -> {
      onContainerClick(extensionId, container, holder)
      true
    }
  }


  private fun openContainer(
    extensionId: String?,
    title: String,
    transitionView: View?,
    pagedData: PagedData<AVPMediaItem>?
  ) {
    extensionId ?: return noClient()
    val bundle = Bundle()
    bundle.putString("extensionId", extensionId)

    val viewModel by fragment.activityViewModels<BrowseViewModel>()
    viewModel.moreFlow = pagedData
    viewModel.title = title

    val browseFragment = BrowseFragment();
    browseFragment.arguments = bundle
    fragment.navigate(browseFragment, transitionView)
    afterOpening?.invoke()
  }

}
