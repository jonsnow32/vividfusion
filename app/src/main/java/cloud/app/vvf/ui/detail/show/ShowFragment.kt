package cloud.app.vvf.ui.detail.show

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentShowBinding
import cloud.app.vvf.ui.detail.bind
import cloud.app.vvf.ui.media.MediaClickListener
import cloud.app.vvf.ui.media.MediaItemAdapter
import cloud.app.vvf.ui.paging.toFlow
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setTextWithVisibility
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.movie.Movie
import cloud.app.vvf.common.models.movie.Show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShowFragment : Fragment() {
  private var binding by autoCleared<FragmentShowBinding>()
  private val viewModel by viewModels<ShowViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val shortItem by lazy { args.getSerialized<AVPMediaItem.ShowItem>("mediaItem")!! }

  @Inject
  lateinit var preferences: SharedPreferences

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentShowBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(binding.header.imagePoster)
    applyInsets { binding.header.topBar.setPadding(0, it.top, 0, 0) }
    bind(shortItem)
    setupListeners()
    setupObservers()
    loadInitialData()
  }

  private fun setupListeners() {
    binding.header.backBtn.setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    binding.header.showFavorite.setOnClickListener {
      viewModel.toggleFavoriteStatus {
        val messageResId = if (viewModel.favoriteStatus.value) {
          R.string.favorite_added
        } else {
          R.string.favorite_removed
        }
        createSnack(getString(messageResId, shortItem.title))
      }
    }
  }

  private fun setupObservers() {
    observe(viewModel.favoriteStatus) { isFavorite ->
      val favoriteIconRes = if (isFavorite) {
        R.drawable.favorite_24dp
      } else {
        R.drawable.favorite_border_24dp
      }
      binding.header.showFavorite.setImageResource(favoriteIconRes)
    }

    observe(viewModel.fullMediaItem) { mediaItem ->
      if (mediaItem == null) return@observe
      setupActorAdapter(mediaItem)
      viewModel.loadRecommended()
    }
    observe(viewModel.recommendations) { paging ->
      paging?.let {
        binding.recommendedHolder.isGone = false
        if (binding.rvRecommendedMedia.adapter == null) {
          val recommendationAdapter =
            MediaItemAdapter(
              MediaClickListener(fragmentManager = parentFragmentManager), "", clientId
            )
          binding.rvRecommendedMedia.adapter = recommendationAdapter
        }
        (binding.rvRecommendedMedia.adapter as MediaItemAdapter).submit(paging)
      }
    }
    observe(viewModel.lastWatchedEpisode) { lastWatchedEpisode ->
      binding.btnResume.isGone = lastWatchedEpisode == null
      binding.btnResume.setTextWithVisibility(lastWatchedEpisode?.item?.title)
    }

    observe(viewModel.watchedSeasons) {
      setUpSeasons(it)
    }
  }

  fun List<AVPMediaItem>.toPagedList() = PagedData.Single { this }

  private fun setUpSeasons(seasons: List<AVPMediaItem.SeasonItem>?) {
    binding.seasonHolder.isGone = seasons.isNullOrEmpty()
    seasons?.let {
      binding.rvSeason.adapter =
        MediaItemAdapter(MediaClickListener(fragmentManager = parentFragmentManager), "", clientId)
      setupSortOptions(seasons)
    }
  }

  private enum class SortMode { ASCENDING, DESCENDING }

  private fun setupSortOptions(seasons: List<AVPMediaItem.SeasonItem>) {
    binding.seasonSortTxt.isGone = (seasons.size < 2)

    val key = preferences.getInt(
      getString(R.string.season_sort_key),
      SortMode.ASCENDING.ordinal
    )

    val currentSortMode = SortMode.entries[key]

    updateSortUI(currentSortMode)
    displaySortedSeasons(seasons, currentSortMode)

    binding.seasonSortTxt.setOnClickListener {
      val sortMode = SortMode.entries[preferences.getInt(
        getString(R.string.season_sort_key),
        SortMode.ASCENDING.ordinal
      )]
      val newSortMode = when (sortMode) {
        SortMode.DESCENDING -> SortMode.ASCENDING
        SortMode.ASCENDING -> SortMode.DESCENDING
      }
      updateSortUI(newSortMode)
      displaySortedSeasons(seasons, newSortMode)
      preferences.edit().putInt(getString(R.string.season_sort_key), newSortMode.ordinal).apply()
    }
  }

  private fun updateSortUI(sortMode: SortMode) {
    binding.seasonSortTxt.text =
      getString(if (sortMode == SortMode.ASCENDING) R.string.ascending_sort else R.string.descending_sort)
    binding.seasonSortTxt.setCompoundDrawablesWithIntrinsicBounds(
      0, 0,
      if (sortMode == SortMode.ASCENDING) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up,
      0
    )
  }

  private fun displaySortedSeasons(seasons: List<AVPMediaItem.SeasonItem>, sortMode: SortMode) {
    val sortedSeasons = sortSeasons(seasons, sortMode)
    val seasonAdapter = binding.rvSeason.adapter as MediaItemAdapter

    lifecycleScope.launch(Dispatchers.IO) {
      sortedSeasons.toPagedList().toFlow()
        .collectLatest(seasonAdapter::submit)
    }
  }

  private fun sortSeasons(
    seasons: List<AVPMediaItem.SeasonItem>,
    sortMode: SortMode
  ): List<AVPMediaItem.SeasonItem> {
    return when (sortMode) {
      SortMode.DESCENDING -> seasons.sortedByDescending { it.season.number }
      SortMode.ASCENDING -> seasons.sortedBy { it.season.number }
    }
  }

  private fun loadInitialData() {
    viewModel.getItemDetails(shortItem)
  }

  private fun setupActorAdapter(mediaItem: AVPMediaItem) {
    val actorAdapter = MediaItemAdapter(MediaClickListener(parentFragmentManager), "", clientId)
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
          MediaClickListener(fragmentManager = parentFragmentManager), "", clientId
        )
      binding.rvRecommendedMedia.adapter = recommendationAdapter

      lifecycleScope.launch {
        it.toPagedList().toFlow().collectLatest(recommendationAdapter::submit)
      }
    }
  }

  fun bind(item: AVPMediaItem?) {
    if (item == null) return
    binding.header.bind(item)
  }

}
