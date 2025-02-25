package cloud.app.vvf.ui.detail.show

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.MainActivityViewModel
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
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.movie.Movie
import cloud.app.vvf.common.models.movie.Show
import cloud.app.vvf.datastore.helper.BookmarkItem
import cloud.app.vvf.ui.detail.TrailerDialog
import cloud.app.vvf.ui.stream.StreamFragment
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.Utils.getEpisodeShortTitle
import cloud.app.vvf.utils.navigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShowFragment : Fragment() {
  private var binding by autoCleared<FragmentShowBinding>()
  private val viewModel by viewModels<ShowViewModel>()
  private val mainViewModel by activityViewModels<MainActivityViewModel>()

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

    binding.buttonShowTrailer.setOnClickListener {
      val dialog = DockingDialog().show(parentFragmentManager, null)

    }
    binding.buttonMovieStreamingSearch.setOnClickListener {
      viewModel.fullMediaItem.value?.let {
        navigate(StreamFragment.newInstance(it))
      }
    }

    binding.buttonShowShare.setOnClickListener {

    }

    binding.buttonBookmark.setOnClickListener {
      val item = viewModel.fullMediaItem.value ?: shortItem

      val status = mainViewModel.getBookmark(item)
      val bookmarks = BookmarkItem.getBookmarkItemSubclasses().toMutableList().apply {
        add("None")
      }
      val selectedIndex =
        if (status == null) (bookmarks.size - 1) else bookmarks.indexOf(status.javaClass.simpleName);

      SelectionDialog.single(
        bookmarks, selectedIndex,
        getString(R.string.add_to_bookmark),
        false,
        {},
        { selected ->
          mainViewModel.addToBookmark(item, bookmarks[selected]);

          val bookmarkStatus = mainViewModel.getBookmark(item)
          binding.buttonBookmark.setText(BookmarkItem.getStringIds(bookmarkStatus))
          if (bookmarkStatus != null) {
            binding.buttonBookmark.icon =
              ContextCompat.getDrawable(requireActivity(), R.drawable.ic_bookmark_filled)
          }

        }).show(parentFragmentManager, null)
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
      viewModel.loadRecommended(clientId)
      setupTrailerAdapter(mediaItem)
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
      binding.buttonLastWatchedEpisode.isGone = lastWatchedEpisode == null
      binding.buttonLastWatchedEpisode.text = context?.getEpisodeShortTitle(lastWatchedEpisode?.getEpisode())
    }

    observe(viewModel.watchedSeasons) {
      setUpSeasons(it)
    }
  }

  private fun setupTrailerAdapter(mediaItem: AVPMediaItem) {
    binding.buttonShowTrailer.setOnClickListener {
      val dialog = TrailerDialog.newInstance(mediaItem.generalInfo?.videos, clientId)
      dialog.show(parentFragmentManager, "")
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
      getString(R.string.pref_season_sort),
      SortMode.ASCENDING.ordinal
    )

    val currentSortMode = SortMode.entries[key]

    updateSortUI(currentSortMode)
    displaySortedSeasons(seasons, currentSortMode)

    binding.seasonSortTxt.setOnClickListener {
      val sortMode = SortMode.entries[preferences.getInt(
        getString(R.string.pref_season_sort),
        SortMode.ASCENDING.ordinal
      )]
      val newSortMode = when (sortMode) {
        SortMode.DESCENDING -> SortMode.ASCENDING
        SortMode.ASCENDING -> SortMode.DESCENDING
      }
      updateSortUI(newSortMode)
      displaySortedSeasons(seasons, newSortMode)
      preferences.edit().putInt(getString(R.string.pref_season_sort), newSortMode.ordinal).apply()
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
    viewModel.getItemDetails(shortItem, clientId)
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

    binding.buttonShowComments.isGone = true
    binding.buttonShowShare.isGone = true
    binding.buttonShowWebSearch.isGone = true

    viewModel.fullMediaItem.value?.let {
      val bookmarkStatus = mainViewModel.getBookmark(it)
      binding.buttonBookmark.setText(BookmarkItem.getStringIds(bookmarkStatus))
      if (bookmarkStatus != null) {
        binding.buttonBookmark.icon =
          ContextCompat.getDrawable(requireActivity(), R.drawable.ic_bookmark_filled)
      }
    }

    binding.buttonShowTrailer.isGone = item.generalInfo?.videos.isNullOrEmpty()
  }

}
