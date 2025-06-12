package cloud.app.vvf.ui.detail.show.season

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import cloud.app.vvf.MainActivityViewModel
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.databinding.FragmentSeasonBinding
import cloud.app.vvf.ui.detail.bind
import cloud.app.vvf.ui.detail.show.episode.EpisodeAdapter
import cloud.app.vvf.ui.detail.show.episode.EpisodeAdapter.EpisodeData
import cloud.app.vvf.ui.stream.StreamFragment
import cloud.app.vvf.ui.widget.dialog.itemOption.ItemOptionDialog
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.collect
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class SeasonFragment : Fragment(), EpisodeAdapter.Listener {
  private var binding by autoCleared<FragmentSeasonBinding>()
  private val viewModel by viewModels<SeasonViewModel>()
  private val mainViewModel by activityViewModels<MainActivityViewModel>()
  private val args by lazy { requireArguments() }
  private val extensionId by lazy { args.getString("extensionId")!! }
  private val shortItem by lazy { args.getSerialized<AVPMediaItem.SeasonItem>("mediaItem")!! }

  @Inject
  lateinit var sharedPreferences: SharedPreferences
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentSeasonBinding.inflate(inflater, container, false)
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
  }

  private fun setupObservers() {
    collect(viewModel.fullSeasonItem) { mediaItem ->
      if (mediaItem == null) return@collect
      bind(mediaItem)
      setupEpisodes(mediaItem.season.episodes?.map { episode -> EpisodeData(episode, 0, 0) })
    }

    collect(viewModel.favoriteStatus) { isFavorite ->
      var favoriteIconRes = R.drawable.favorite_24dp
      var favoriteStringnRes = R.string.action_remove_from_favorites
      if (!isFavorite) {
        favoriteIconRes = R.drawable.favorite_border_24dp
        favoriteStringnRes = R.string.action_add_to_favorites
      }
      binding.header.imgBtnFavourite.setImageResource(favoriteIconRes)
    }

    collect(viewModel.episodeData) {
      if (it == null) return@collect
      bind(viewModel.fullSeasonItem.value)
      displaySortedEpisodes(it, getCurrentSort())
    }
  }

  enum class SortMode { ASCENDING, DESCENDING }

  fun getCurrentSort(): SortMode {
    val key = sharedPreferences.getInt(
      getString(R.string.pref_episode_sort),
      SortMode.ASCENDING.ordinal
    )
    return SortMode.entries[key]
  }

  private var currentSelectedRangeIndex = 0;
  private fun setupEpisodes(episodes: List<EpisodeData>?) {

    binding.episodeHolder.isGone = false
    binding.skeletonEpisodes.root.isGone = true

    if (episodes.isNullOrEmpty()) return

    val rangeSize = 20
    val episodeRanges = divideEpisodesIntoRanges(episodes, rangeSize)
    if (episodeRanges.size > 1) {
      binding.episodeSelectRange.text = episodeRanges[currentSelectedRangeIndex].rangeLabel
      binding.episodeSelectRange.isGone = false
      binding.episodeSelectRange.setOnClickListener {
        val listItems = episodeRanges.map { it.rangeLabel }
        SelectionDialog.single(
          listItems,
          currentSelectedRangeIndex,
          getString(R.string.select_episode_range),
          false
        ).show(parentFragmentManager) { result ->
          result?.let {
            currentSelectedRangeIndex = it.getIntegerArrayList("selected_items")?.get(0) ?: 0
            binding.episodeSelectRange.text = episodeRanges[currentSelectedRangeIndex].rangeLabel
            viewModel.loadPlayback(episodeRanges[currentSelectedRangeIndex].episodes)
          }
        }
      }
    }


    if (binding.rvEpisodes.adapter == null) {
      binding.rvEpisodes.adapter = EpisodeAdapter(this)
      binding.rvEpisodes.setHasFixedSize(true)
    }

    binding.episodeSortTxt.isGone = (episodes.size < 2)
    val currentSortMode = getCurrentSort()
    updateSortUI(currentSortMode)
    viewModel.loadPlayback(episodeRanges[currentSelectedRangeIndex].episodes)
    binding.episodeSortTxt.setOnClickListener {
      val sortMode = getCurrentSort()
      val newSortMode = when (sortMode) {
        SortMode.DESCENDING -> SortMode.ASCENDING
        SortMode.ASCENDING -> SortMode.DESCENDING
      }
      sharedPreferences.edit().putInt(getString(R.string.pref_episode_sort), newSortMode.ordinal)
        .apply()
      updateSortUI(newSortMode)
      viewModel.loadPlayback(episodeRanges[currentSelectedRangeIndex].episodes)

    }
  }

  private fun updateSortUI(sortMode: SortMode) {
    binding.episodeSortTxt.text =
      getString(if (sortMode == SortMode.ASCENDING) R.string.ascending_sort else R.string.descending_sort)
    binding.episodeSortTxt.setCompoundDrawablesWithIntrinsicBounds(
      0, 0,
      if (sortMode == SortMode.ASCENDING) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up,
      0
    )
  }

  private fun displaySortedEpisodes(episodes: List<EpisodeData>, sortMode: SortMode) {
    val sortedEpisodes = sortEpisodes(episodes, sortMode)
    val seasonAdapter = binding.rvEpisodes.adapter as EpisodeAdapter
    seasonAdapter.submitList(sortedEpisodes) // Use flatMap here
  }

  private fun sortEpisodes(episodes: List<EpisodeData>, sortMode: SortMode): List<EpisodeData> {
    return when (sortMode) {
      SortMode.DESCENDING -> episodes.sortedByDescending { it.episode.episodeNumber }
      SortMode.ASCENDING -> episodes.sortedBy { it.episode.episodeNumber }
    }
  }

  private fun loadInitialData() {
    binding.episodeHolder.isGone = true;
    viewModel.getItemDetails(shortItem, extensionId)
  }

  fun bind(item: AVPMediaItem?) {
    if (item == null) return
    binding.header.bind(item, this)
    binding.header.buttonBookmark.isGone = true;
  }

  private fun divideEpisodesIntoRanges(
    episodes: List<EpisodeData>,
    rangeSize: Int
  ): List<EpisodeRange> {
    return episodes.chunked(rangeSize).mapIndexed { index, chunk ->
      val start = index * rangeSize + 1
      val end = start + chunk.size - 1
      EpisodeRange("$start-$end", chunk)
    }
  }

  data class EpisodeRange(
    val rangeLabel: String,
    val episodes: List<EpisodeData>
  )

  override fun onClick(episode: EpisodeData) {
    StreamFragment.newInstance(
      AVPMediaItem.EpisodeItem(
        episode.episode,
        seasonItem = shortItem
      )
    ).show(parentFragmentManager) {

    }
  }

  override fun onLongClick(episode: EpisodeData): Boolean {
    ItemOptionDialog.newInstance(
      extensionId,
      AVPMediaItem.EpisodeItem(
        episode = episode.episode,
        seasonItem = viewModel.fullSeasonItem.value ?: shortItem
      )
    ).show(parentFragmentManager)
    return true
  }
}
