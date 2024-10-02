package cloud.app.avp.ui.detail.movie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentMovieBinding
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
class MovieFragment : Fragment(), MediaItemAdapter.Listener {
  private var binding by autoCleared<FragmentMovieBinding>()
  private val viewModel by viewModels<MovieViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val shortMovieItem by lazy { args.getParcel<AVPMediaItem.MovieItem>("mediaItem")!! }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMovieBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(binding.headerBackground)
    bind(shortMovieItem)
    viewModel.getFullMovieItem(shortMovieItem)

    observe(viewModel.fullMediaItem) {

      it?.let { bind(it as AVPMediaItem.MovieItem) }
      val actorAdapter = MediaItemAdapter(this, "", "clientID")
      binding.rvActors.adapter = actorAdapter;
      val actorList =
        (it as? AVPMediaItem.MovieItem)?.movie?.generalInfo?.actors?.map { actorData ->
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
        (it as? AVPMediaItem.MovieItem)?.movie?.recommendations?.map { recommendationData ->
          AVPMediaItem.MovieItem(recommendationData)
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
  fun bind(item: AVPMediaItem.MovieItem) {
    item.backdrop.loadWith(binding.headerBackground, item.poster)
    binding.title.text = item.title
    binding.mediaOverview.text = item.movie.generalInfo.overview

    val genre1 = item.movie.generalInfo.genres?.firstOrNull();
    binding.genre1.text = genre1
    binding.genre1.isGone = genre1.isNullOrEmpty()

    val genre2 = item.movie.generalInfo.genres?.getOrNull(1);
    binding.genre2.text =genre2
    binding.genre2.isGone = genre2.isNullOrEmpty()


    val genre3 = item.movie.generalInfo.genres?.getOrNull(2);
    binding.genre3.text =genre3
    binding.genre3.isGone = genre3.isNullOrEmpty()

    val contentRating = item.movie.generalInfo.contentRating
    binding.contentRating.text = contentRating
    binding.contentRating.isGone = contentRating.isNullOrEmpty()

    binding.mediaReleaseYear.text =
      item.movie.generalInfo.releaseDateMsUTC?.toLocalYear().toString()
    binding.mediaRating.text = item.movie.generalInfo.rating.toString()
    binding.mediaRuntime.text = item.movie.generalInfo.runtime.toString()

    binding.watchNow.setOnClickListener {
      it.transitionName = "watchNow" + item.id
      val streamFragment = StreamFragment();
      streamFragment.arguments = bundleOf("mediaItem" to item, "clientId" to "clientID")
      navigate(
        streamFragment,
        it
      )
    }
  }

  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {
    val movieFragment = MovieFragment();
    movieFragment.arguments = bundleOf("mediaItem" to item, "clientId" to "clientID")
    navigate(
      movieFragment,
      transitionView
    )
  }

  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }

  override fun onPause() {
    super.onPause()
  }
}
