package cloud.app.avp.ui.detail.show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentMovieBinding
import cloud.app.avp.ui.detail.movie.MovieFragment
import cloud.app.avp.ui.media.MediaItemAdapter
import cloud.app.avp.ui.paging.SingleSource
import cloud.app.avp.ui.paging.toFlow
import cloud.app.avp.ui.stream.StreamFragment
import cloud.app.avp.utils.TimeUtils.toLocalYear
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.loadWith
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.setupTransition
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ShowFragment : Fragment(), MediaItemAdapter.Listener {
  private var binding by autoCleared<FragmentMovieBinding>()
  private val viewModel by activityViewModels<ShowViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val shortShowItem by lazy { args.getParcel<AVPMediaItem.ShowItem>("mediaItem")!! }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMovieBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupTransition(binding.headerBackground)
    bind(shortShowItem)

    viewModel.getFullShowItem(shortShowItem)
    observe(viewModel.fullMediaItem) {
      it?.let { bind(it as AVPMediaItem.ShowItem) }
      val actorAdapter = MediaItemAdapter(this, "", "clientID")
      binding.rvActors.adapter = actorAdapter;
      val actorList =
        (it as? AVPMediaItem.ShowItem)?.show?.generalInfo?.actors?.map { actorData ->
          AVPMediaItem.ActorItem(actorData)
        }

      actorList?.let {
        val flow = it.toPagedList().toFlow();
        flow.collectLatest { pagingData ->
          actorAdapter.submit(pagingData)
        }
      }

      val recommendationAdapter = MediaItemAdapter(this, "", "clientID")
      binding.rvRecommendedMedia.adapter = recommendationAdapter
      val movieList =
        (it as? AVPMediaItem.ShowItem)?.show?.recommendations?.map { recommendationData ->
          AVPMediaItem.ShowItem(recommendationData)
        }
      movieList?.apply {
        val flow = toPagedList().toFlow();
        flow.collectLatest { pagingData ->
          recommendationAdapter.submit(pagingData)
        }
      }
    }
  }

  fun List<AVPMediaItem>.toPagedList() = PagedData.Single { this }
  fun bind(item: AVPMediaItem.ShowItem) {
    item.backdrop.loadWith(binding.headerBackground, item.poster)
    binding.title.text = item.title
    binding.mediaOverview.text = item.show.generalInfo.overview

    binding.genre1.text = item.show.generalInfo.genres?.firstOrNull()
    binding.genre2.text = item.show.generalInfo.genres?.getOrNull(1)
    binding.genre3.text = item.show.generalInfo.genres?.getOrNull(2)
    binding.contentRating.text = item.show.generalInfo.contentRating
    binding.mediaReleaseYear.text =
      item.show.generalInfo.releaseDateMsUTC?.toLocalYear().toString()
    binding.mediaRating.text = item.show.generalInfo.rating.toString()
    binding.mediaRuntime.text = item.show.generalInfo.runtime.toString()

    binding.watchNow.setOnClickListener {
      it.transitionName = "watchNow" + item.id
      val streamFragment = StreamFragment()
      streamFragment.arguments = bundleOf("mediaItem" to item, "clientId" to "clientID")
      navigate(
        streamFragment,
        it
      )
    }
  }

  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {
    val movieFragment = MovieFragment()
    movieFragment.arguments = bundleOf("mediaItem" to item, "clientId" to "clientID")
    navigate(
      movieFragment,
      transitionView
    )
  }

  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }
}
