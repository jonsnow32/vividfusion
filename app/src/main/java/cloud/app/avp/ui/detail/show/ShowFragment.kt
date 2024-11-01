package cloud.app.avp.ui.detail.show

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentShowBinding
import cloud.app.avp.ui.detail.show.episode.EpisodeAdapter
import cloud.app.avp.ui.detail.show.episode.EpisodePagingSource
import cloud.app.avp.ui.media.MediaClickListener
import cloud.app.avp.ui.media.MediaItemAdapter
import cloud.app.avp.ui.paging.toFlow
import cloud.app.avp.utils.TimeUtils.toLocalMonthYear
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.loadWith
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.roundTo
import cloud.app.avp.utils.setTextWithVisibility
import cloud.app.avp.utils.setupTransition
import cloud.app.avp.utils.tv.setLinearListLayout
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.movie.Season
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
class ShowFragment : Fragment() {

  private var binding by autoCleared<FragmentShowBinding>()
  private val viewModel by viewModels<ShowViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val shortShowItem by lazy { args.getParcel<AVPMediaItem.ShowItem>("mediaItem")!! }

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

    setupTransition(binding.headerBackground)
    applyInsets {
      binding.topBar.setPadding(0, it.top, 0, 0)
      binding.scrollView.setPadding(0, it.top, 0, 0)
    }
    bind(shortShowItem)

    setupListeners()
    setupObservers()
    loadInitialData()
  }

  private fun setupListeners() {
    binding.backBtn.setOnClickListener {
      parentFragmentManager.popBackStack()
    }

    binding.showFavorite.setOnClickListener {
      viewModel.toggleFavoriteStatus {
        val messageResId = if (viewModel.favoriteStatus.value) {
          R.string.favorite_added
        } else {
          R.string.favorite_removed
        }
        createSnack(getString(messageResId, shortShowItem.title))
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
      binding.showFavorite.setImageResource(favoriteIconRes)
    }

    observe(viewModel.fullMediaItem) { item ->
      if (item is AVPMediaItem.ShowItem) {
        bind(item)
        setupActorAdapter(item)
        setupRecommendationAdapter(item)
      }
    }

    observe(viewModel.seasons) { seasons ->
      println("Coroutine Context observe $coroutineContext")
      binding.episodeLoading.isGone = seasons != null
      seasons?.let { setupEpisodeAdapter(seasons) }
    }
  }

  private fun loadInitialData() {
    viewModel.getFullShowItem(shortShowItem)
  }



  private fun setupActorAdapter(showItem: AVPMediaItem.ShowItem) {
    val actorAdapter = MediaItemAdapter(MediaClickListener(parentFragmentManager), "", clientId)
    binding.rvActors.adapter = actorAdapter

    val actorList = showItem.show.generalInfo.actors?.map { AVPMediaItem.ActorItem(it) }
    actorList?.let {
      lifecycleScope.launch {
        it.toPagedList().toFlow().collectLatest(actorAdapter::submit)
      }
    }
  }

  private fun setupRecommendationAdapter(showItem: AVPMediaItem.ShowItem) {
    val recommendations = showItem.show.recommendations?.map { AVPMediaItem.ShowItem(it) }
    binding.recommendedTitle.isGone = recommendations.isNullOrEmpty()

    recommendations?.let {
      val recommendationAdapter =
        MediaItemAdapter(MediaClickListener(parentFragmentManager), "", clientId)
      binding.rvRecommendedMedia.adapter = recommendationAdapter

      lifecycleScope.launch {
        it.toPagedList().toFlow().collectLatest(recommendationAdapter::submit)
      }
    }
  }
  private fun setupEpisodeAdapter(seasons: List<Season>) {
    binding.episodeList.episodes.apply {
      adapter = EpisodeAdapter()
      //setHasFixedSize(true)
      setLinearListLayout(isHorizontal = false, nextUp = R.id.sortText, nextRight = R.id.sortText)
    }
    setupSeasonTabs(seasons)
    setupSortOptions(seasons)
  }
  private fun setupSeasonTabs(seasons: List<Season>) {
    val currentSort =
      preferences.getInt(getString(R.string.episode_sort_key), R.string.ascending_sort)

    binding.episodeList.sortText.text = getString(currentSort)
    binding.episodeList.tabLayout.removeAllTabs()

    seasons.forEachIndexed { index, season ->
      binding.episodeList.tabLayout.addTab(
        binding.episodeList.tabLayout.newTab().apply {
          var name = "${getString(R.string.season_short)}${String.format("%02d", season.number)}"
          if (season.number <= 0)
            name = season.title ?: name;
          text = name
          view.nextFocusDownId = R.id.sortText
        },
        index == 0
      )
    }

    val tabListener = createTabListener(seasons, currentSort)
    binding.episodeList.tabLayout.addOnTabSelectedListener(tabListener)
    binding.episodeList.tabLayout.getTabAt(0)?.let {
      tabListener.onTabSelected(it)
    }
  }

  private fun setupSortOptions(seasons: List<Season>) {
    binding.episodeList.sortText.setOnClickListener {
      val currentSort = toggleSortMode()
      updateEpisodeList(seasons[binding.episodeList.tabLayout.selectedTabPosition], currentSort)
      binding.episodeList.sortText.text = getString(currentSort)
    }
  }

  private fun toggleSortMode(): Int {
    val sortMode = preferences.getInt(getString(R.string.episode_sort_key), R.string.ascending_sort)
    val newSortMode =
      if (sortMode == R.string.descending_sort) R.string.ascending_sort else R.string.descending_sort
    binding.episodeList.sortText.setCompoundDrawablesWithIntrinsicBounds(
      0,
      0,
      if (newSortMode == R.string.ascending_sort) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up,
      0
    )
    preferences.edit().putInt(getString(R.string.episode_sort_key), newSortMode).apply()
    return newSortMode
  }

  private fun createTabListener(seasons: List<Season>, currentSort: Int) =
    object : TabLayout.OnTabSelectedListener {
      var enabled = true
      override fun onTabSelected(tab: TabLayout.Tab) {
        if (enabled) {
          val season = seasons[tab.position]
          updateEpisodeList(season, currentSort)
        }
      }

      override fun onTabUnselected(tab: TabLayout.Tab) = Unit
      override fun onTabReselected(tab: TabLayout.Tab) = Unit
    }

  private fun updateEpisodeList(season: Season, sortMode: Int) {
    val episodeList = season.episodes?.map { AVPMediaItem.EpisodeItem(it, shortShowItem.show) }
    val sortedList = if (sortMode == R.string.ascending_sort) {
      episodeList?.sortedBy { it.episode.episodeNumber }
    } else {
      episodeList?.sortedByDescending { it.episode.episodeNumber }
    }

    sortedList?.let {
      val pager = Pager(
        config = PagingConfig(pageSize = 20, initialLoadSize = 20), // Set your desired page size
        pagingSourceFactory = { EpisodePagingSource(sortedList) }
      )

      lifecycleScope.launch(Dispatchers.IO) {
        pager.flow.collectLatest {
          (binding.episodeList.episodes.adapter as EpisodeAdapter).submit(it)
        }
      }
    }
  }

  fun List<AVPMediaItem>.toPagedList() = PagedData.Single { this }

  private fun bind(item: AVPMediaItem.ShowItem) {
    with(binding) {
      item.backdrop.loadWith(headerBackground, item.poster)
      title.text = item.title
      mediaOverview.text = item.show.generalInfo.overview

      genre1.setTextWithVisibility(item.show.generalInfo.genres?.firstOrNull())
      genre2.setTextWithVisibility(item.show.generalInfo.genres?.getOrNull(1))
      genre3.setTextWithVisibility(item.show.generalInfo.genres?.getOrNull(2))
      tagLine.setTextWithVisibility(item.show.tagLine)

      mediaReleaseYear.setTextWithVisibility(item.show.generalInfo.releaseDateMsUTC?.toLocalMonthYear())
      mediaRating.setTextWithVisibility(getString(R.string.rating_format, item.rating?.roundTo(1)))
      mediaStatus.setTextWithVisibility(item.show.status)
      contentRating.setTextWithVisibility(item.show.generalInfo.contentRating)
    }
  }
}

