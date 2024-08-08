package cloud.app.avp.ui.main.media

import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.paging.PagingData
import androidx.paging.map
import cloud.app.avp.AVPApplication.Companion.noClient
import cloud.app.avp.R
import cloud.app.avp.ui.main.movies.MoviesViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.avp.utils.navigate
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaClickListener(
  private val fragmentManager: FragmentManager, private val afterOpening: (() -> Unit)? = null
) : MediaContainerAdapter.Listener {

  val fragment get() = fragmentManager.findFragmentById(R.id.navHostFragment)!!
  private fun noClient() = fragment.createSnack(fragment.requireContext().noClient())

  override fun onClick(
    clientId: String?, item: AVPMediaItem, transitionView: View?
  ) {
    TODO("Not yet implemented")
  }

  override fun onLongClick(
    clientId: String?, item: AVPMediaItem, transitionView: View?
  ): Boolean {
    TODO("Not yet implemented")
  }

  override fun onClick(clientId: String?, container: MediaItemsContainer, transitionView: View) {
    when (container) {
      is MediaItemsContainer.Item -> onClick(clientId, container.media, transitionView)
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
    val viewModel by fragment.activityViewModels<MoviesViewModel>()
    viewModel.moreFlow = pagedData
    fragment.navigate(R.id.moviesFragment, transitionView)
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
