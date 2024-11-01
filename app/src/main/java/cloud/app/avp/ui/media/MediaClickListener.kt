package cloud.app.avp.ui.media

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavArgs
import cloud.app.avp.AVPApplication.Companion.noClient
import cloud.app.avp.R
import cloud.app.avp.ui.browse.BrowseFragment
import cloud.app.avp.ui.browse.BrowseViewModel
import cloud.app.avp.ui.detail.ItemFragment
import cloud.app.avp.ui.detail.actor.ActorFragment
import cloud.app.avp.ui.detail.show.ShowFragment
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.tryWith
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer

class MediaClickListener(
  private val fragmentManager: FragmentManager,
  private val afterOpening: (() -> Unit)? = null,
  private val afterFocusChange: ((AVPMediaItem, Boolean) -> Unit)? = null,
) : MediaContainerAdapter.Listener {

  val fragment get() = fragmentManager.findFragmentById(R.id.navHostFragment)!!
  private fun noClient() = fragment.createSnack(fragment.requireContext().noClient())

  override fun onClick(
    clientId: String?, item: AVPMediaItem, transitionView: View?
  ) {
    val bundle = Bundle()
    bundle.putString("clientId", clientId)
    bundle.putParcelable("mediaItem", item)
    val movieFragment = ItemFragment()
    movieFragment.arguments = bundle;
    fragment.navigate(movieFragment, transitionView)
  }

  override fun onLongClick(
    clientId: String?, item: AVPMediaItem, transitionView: View?
  ): Boolean {
    return false
  }

  override fun onFocusChange(clientId: String?, item: AVPMediaItem, hasFocus: Boolean) {
    afterFocusChange?.invoke(item, hasFocus)
  }

  override fun onClick(clientId: String?, container: MediaItemsContainer, transitionView: View) {
    when (container) {
      is MediaItemsContainer.Item -> tryWith {  onClick(clientId, container.media, transitionView) }
      is MediaItemsContainer.Category -> openContainer(
        clientId,
        container.title,
        transitionView,
        container.more
      )
    }
  }

  private fun openContainer(
    clientId: String?,
    title: String,
    transitionView: View?,
    pagedData : PagedData<AVPMediaItem>?
  ) {
    clientId ?: return noClient()
    val viewModel by fragment.activityViewModels<BrowseViewModel>()
    viewModel.moreFlow = pagedData
    viewModel.title = title
    fragment.navigate(BrowseFragment(), transitionView)
    afterOpening?.invoke()
  }

  override fun onLongClick(
    clientId: String?, container: MediaItemsContainer, transitionView: View
  ) = when (container) {
    is MediaItemsContainer.Item -> onLongClick(clientId, container.media, transitionView)
    else -> {
      onClick(clientId, container, transitionView)
      true
    }
  }

}
