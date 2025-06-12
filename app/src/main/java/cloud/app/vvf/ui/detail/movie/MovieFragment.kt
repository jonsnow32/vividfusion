package cloud.app.vvf.ui.detail.movie

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.MainActivityViewModel
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.movie.Movie
import cloud.app.vvf.common.models.movie.Show
import cloud.app.vvf.databinding.FragmentMovieBinding
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.ui.detail.TrailerDialog
import cloud.app.vvf.ui.detail.bind
import cloud.app.vvf.ui.media.MediaClickListener
import cloud.app.vvf.ui.media.MediaItemAdapter
import cloud.app.vvf.ui.paging.toFlow
import cloud.app.vvf.ui.stream.StreamFragment
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MovieFragment : Fragment() {
  private var binding by autoCleared<FragmentMovieBinding>()
  private val viewModel by viewModels<MovieViewModel>()
  private val mainViewModel by activityViewModels<MainActivityViewModel>()
  private val args by lazy { requireArguments() }
  private val extensionId by lazy { args.getString("extensionId")!! }
  private val shortItem by lazy { args.getSerialized<AVPMediaItem.MovieItem>("mediaItem")!! }


  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMovieBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(binding.header.imagePoster)
    applyInsets {
      binding.scrollView.setPadding(0, it.top, it.end, it.bottom)
    }
    bind(shortItem)

    setupListeners()
    setupObservers()
    loadInitialData()

  }

  private fun setupListeners() {
    binding.header.backBtn.setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    binding.header.imgBtnFavourite.setOnClickListener {
      viewModel.toggleFavoriteStatus {
        val messageResId = if (viewModel.favoriteStatus.value) {
          R.string.favorite_added
        } else {
          R.string.favorite_removed
        }
        createSnack(getString(messageResId, shortItem.title))
      }
    }


    binding.header.buttonStreamingSearch.setOnClickListener {
      viewModel.fullMediaItem.value?.let {
        StreamFragment.newInstance(it).show(parentFragmentManager) {

        }
      }
    }
  }

  private fun setupObservers() {
    observe(viewModel.favoriteStatus) { isFavorite ->
      var favoriteIconRes = R.drawable.favorite_24dp
      var favoriteStringnRes = R.string.action_remove_from_favorites
      if (!isFavorite)  {
        favoriteIconRes = R.drawable.favorite_border_24dp
        favoriteStringnRes = R.string.action_add_to_favorites
      }
      binding.header.imgBtnFavourite.setImageResource(favoriteIconRes)
    }


    observe(viewModel.fullMediaItem) { mediaItem ->
      if (mediaItem == null) return@observe

      bind(mediaItem)
      setupActorAdapter(mediaItem)
      setupRecommendationAdapter(mediaItem)
      setupTrailerAdapter(mediaItem)
    }
  }

  private fun setupTrailerAdapter(mediaItem: AVPMediaItem) {
    binding.header.buttonShowTrailer.setOnClickListener {
      val dialog = TrailerDialog.newInstance(mediaItem.generalInfo?.videos, extensionId)
      dialog.show(parentFragmentManager)
    }
  }

  fun List<AVPMediaItem>.toPagedList() = PagedData.Single { this }


  private fun loadInitialData() {
    viewModel.getItemDetails(shortItem, extensionId)
  }

  private fun setupActorAdapter(mediaItem: AVPMediaItem) {
    val actorAdapter = MediaItemAdapter(this, "", extensionId)
    binding.rvActors.adapter = actorAdapter

    val actorList = mediaItem.generalInfo?.actors?.map { AVPMediaItem.ActorItem(it) }
    actorList?.let {
      lifecycleScope.launch {
        it.toPagedList().toFlow().collectLatest(actorAdapter::submit)
      }
    }
  }

  private fun setupRecommendationAdapter(item: AVPMediaItem) {
    val recommendations = item.recommendations?.mapNotNull {
      when (it) {
        is Movie -> AVPMediaItem.MovieItem(it)
        is Show -> AVPMediaItem.ShowItem(it)
        else -> null
      }
    }
    binding.recommendedTitle.isGone = recommendations.isNullOrEmpty()

    recommendations?.let {
      val recommendationAdapter =
        MediaItemAdapter(
          this, "", extensionId
        )
      binding.rvRecommendedMedia.adapter = recommendationAdapter

      lifecycleScope.launch {
        it.toPagedList().toFlow().collectLatest(recommendationAdapter::submit)
      }
    }
  }


  fun bind(item: AVPMediaItem?) {
    if (item == null) return
    binding.header.bind(item, this)
  }

}
