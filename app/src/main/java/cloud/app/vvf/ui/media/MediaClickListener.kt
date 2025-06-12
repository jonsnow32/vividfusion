package cloud.app.vvf.ui.media

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.R
import cloud.app.vvf.VVFApplication.Companion.noClient
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.VideoItem
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.video.Video
import cloud.app.vvf.extension.builtIn.local.MediaUtils
import cloud.app.vvf.features.player.PlayerFragment
import cloud.app.vvf.ui.detail.movie.MovieFragment
import cloud.app.vvf.ui.detail.show.ShowFragment
import cloud.app.vvf.ui.detail.show.season.SeasonFragment
import cloud.app.vvf.ui.main.browse.BrowseFragment
import cloud.app.vvf.ui.main.browse.BrowseViewModel
import cloud.app.vvf.ui.stream.StreamFragment
import cloud.app.vvf.ui.widget.dialog.itemOption.ItemOptionDialog
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.tryWith
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaClickListener(
  private val fragmentManager: FragmentManager,
  private val afterOpening: (() -> Unit)? = null,
  private val afterFocusChange: ((AVPMediaItem, Boolean) -> Unit)? = null,
) : MediaContainerAdapter.Listener {

  val fragment get() = fragmentManager.findFragmentById(R.id.navHostFragment)!!
  private fun noClient() = fragment.createSnack(fragment.requireContext().noClient())

  @OptIn(UnstableApi::class)
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
      is AVPMediaItem.TrailerItem -> TODO()

      is AVPMediaItem.PlaybackProgress -> onItemClick(extensionId, item.item, transitionView)
      is AVPMediaItem.VideoCollectionItem -> TODO()
      is AVPMediaItem.VideoItem -> {
        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

          val context = fragment.context ?: return@launch
          val localVideos = when (item.video) {
            is Video.LocalVideo -> MediaUtils.getAllVideosByAlbum(
              context,
              (item.video as Video.LocalVideo).album
            ).map {
              VideoItem(it)
            }

            is Video.RemoteVideo -> listOf(item)
          }

          withContext(Dispatchers.Main) {
            fragment.navigate(
              PlayerFragment.newInstance(
                mediaItems = localVideos,
                //this is testing
//                subtitles = listOf(
//                  SubtitleData(
//                    name = "WebVTT positioning",
//                    mimeType = "text/vtt",
//                    languageCode = "en",
//                    origin = SubtitleOrigin.URL,
//                    url = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/numeric-lines.vtt",
//                    headers = mapOf()
//                  ),
//                ),
                selectedMediaIdx = localVideos.indexOfFirst { it -> it.id == item.id })
            )

          }
        }

      }

      is AVPMediaItem.TrackItem -> {
        fragment.navigate(
          PlayerFragment.newInstance(
            mediaItems = listOf(item),
            selectedMediaIdx = 0,
            //this is testing
//            subtitles = listOf(
//              SubtitleData(
//                name = "WebVTT positioning",
//                mimeType = "text/vtt",
//                languageCode = "en",
//                origin = SubtitleOrigin.URL,
//                url = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/numeric-lines.vtt",
//                headers = mapOf()
//              ),
//            ),
          )
        )
      }
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

  override fun onContainerClick(
    extensionId: String?,
    container: MediaItemsContainer,
    holder: MediaContainerViewHolder
  ) {
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
//        fragment.navigate(StreamFragment.newInstance(container.items[selectedPosition]))
        StreamFragment.newInstance(container.items[selectedPosition]).show(fragmentManager)
      }
    }
  }

  override fun onContainerLongClick(
    extensionId: String?, container: MediaItemsContainer, holder: MediaContainerViewHolder
  ) = when (container) {
    is MediaItemsContainer.Item -> onItemLongClick(
      extensionId,
      container.media,
      holder.transitionView
    )

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
