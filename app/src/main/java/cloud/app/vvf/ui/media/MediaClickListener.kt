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
import cloud.app.vvf.ui.widget.dialog.ItemOptionDialog
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.tryWith
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.MediaItemsContainer

class MediaClickListener(
  private val fragmentManager: FragmentManager,
  private val afterOpening: (() -> Unit)? = null,
  private val afterFocusChange: ((AVPMediaItem, Boolean) -> Unit)? = null,
) : MediaContainerAdapter.Listener {

  val fragment get() = fragmentManager.findFragmentById(R.id.navHostFragment)!!
  private fun noClient() = fragment.createSnack(fragment.requireContext().noClient())

  override fun onClick(
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
        onLongClick(extensionId, item, transitionView)
      }

      is AVPMediaItem.EpisodeItem,
      is AVPMediaItem.StreamItem -> {
        fragment.createSnack(fragment.getString(R.string.not_implemented))
      }

      is AVPMediaItem.TrailerItem -> TODO()
    }
  }

  override fun onLongClick(
    extensionId: String?, item: AVPMediaItem, transitionView: View?
  ): Boolean {
    extensionId?.let {
      ItemOptionDialog.newInstance(extensionId, item)
        .show(fragmentManager)
      return true
    }
    return false
  }

  override fun onFocusChange(extensionId: String?, item: AVPMediaItem, hasFocus: Boolean) {
    afterFocusChange?.invoke(item, hasFocus)
  }

  override fun onClick(extensionId: String?, container: MediaItemsContainer, transitionView: View) {
    when (container) {
      is MediaItemsContainer.Item -> tryWith {
        onClick(
          extensionId,
          container.media,
          transitionView
        )
      }

      is MediaItemsContainer.Category -> openContainer(
        extensionId,
        container.title,
        transitionView,
        container.more
      )

      is MediaItemsContainer.PageView -> fragment.createSnack("Coming soon")
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

  override fun onLongClick(
    extensionId: String?, container: MediaItemsContainer, transitionView: View
  ) = when (container) {
    is MediaItemsContainer.Item -> onLongClick(extensionId, container.media, transitionView)
    else -> {
      onClick(extensionId, container, transitionView)
      true
    }
  }

}
