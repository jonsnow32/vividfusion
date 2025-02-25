package cloud.app.vvf.ui.detail.show.season

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.Companion.toMediaItem
import cloud.app.vvf.common.models.movie.Episode
import cloud.app.vvf.databinding.FragmentSeasonBinding
import cloud.app.vvf.ui.detail.bind
import cloud.app.vvf.ui.detail.show.episode.EpisodeAdapter
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SeasonFragment : Fragment(), EpisodeAdapter.Listener {
  private var binding by autoCleared<FragmentSeasonBinding>()
  private val viewModel by viewModels<SeasonViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val shortItem by lazy { args.getSerialized<AVPMediaItem.SeasonItem>("mediaItem")!! }
  @Inject
  lateinit var preferences: SharedPreferences

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentSeasonBinding.inflate(inflater, container, false)
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
      bind(mediaItem)
      setupEpisodes(mediaItem.season.episodes)
    }

  }

  private enum class SortMode { ASCENDING, DESCENDING }

  private var currentSelectedRangeIndex = 0;
  private fun setupEpisodes(episodes: List<Episode>?) {
    fun getCurrentSort(): SortMode {
      val key = preferences.getInt(
        getString(R.string.pref_episode_sort),
        SortMode.ASCENDING.ordinal
      )

      return SortMode.entries[key]
    }

    binding.episodeHolder.isGone = false
    binding.skeletonEpisodes.root.isGone = true

    if (episodes.isNullOrEmpty()) return

    val rangeSize = 50
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
          false,
          {}) { itemId ->
          currentSelectedRangeIndex = itemId
          binding.episodeSelectRange.text = episodeRanges[currentSelectedRangeIndex].rangeLabel
          displaySortedEpisodes(episodeRanges[currentSelectedRangeIndex].episodes, getCurrentSort())
        }.show(parentFragmentManager, null)
      }
    }


    if (binding.rvEpisodes.adapter == null) {
      binding.rvEpisodes.adapter = EpisodeAdapter(this)
      binding.rvEpisodes.setHasFixedSize(true)
    }

    binding.episodeSortTxt.isGone = (episodes.size < 2)
    val currentSortMode = getCurrentSort()
    updateSortUI(currentSortMode)
    displaySortedEpisodes(episodeRanges[currentSelectedRangeIndex].episodes, currentSortMode)

    binding.episodeSortTxt.setOnClickListener {
      val sortMode = getCurrentSort()
      val newSortMode = when (sortMode) {
        SortMode.DESCENDING -> SortMode.ASCENDING
        SortMode.ASCENDING -> SortMode.DESCENDING
      }
      updateSortUI(newSortMode)
      displaySortedEpisodes(episodeRanges[currentSelectedRangeIndex].episodes, newSortMode)
      preferences.edit().putInt(getString(R.string.pref_episode_sort), newSortMode.ordinal).apply()
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

  private fun displaySortedEpisodes(episodes: List<Episode>, sortMode: SortMode) {
    val sortedEpisodes = sortEpisodes(episodes, sortMode)
    val seasonAdapter = binding.rvEpisodes.adapter as EpisodeAdapter
    seasonAdapter.submitList(sortedEpisodes) // Use flatMap here
  }

  private fun sortEpisodes(episodes: List<Episode>, sortMode: SortMode): List<Episode> {
    return when (sortMode) {
      SortMode.DESCENDING -> episodes.sortedByDescending { it.episodeNumber }
      SortMode.ASCENDING -> episodes.sortedBy { it.episodeNumber }
    }
  }

  private fun loadInitialData() {
    binding.episodeHolder.isGone = true;
    viewModel.getItemDetails(shortItem, clientId)
  }

  fun bind(item: AVPMediaItem?) {
    if (item == null) return
    binding.header.bind(item)
  }

  private fun divideEpisodesIntoRanges(episodes: List<Episode>, rangeSize: Int): List<EpisodeRange> {
    return episodes.chunked(rangeSize).mapIndexed { index, chunk ->
      val start = index * rangeSize + 1
      val end = start + chunk.size - 1
      EpisodeRange("$start-$end", chunk)
    }
  }

  data class EpisodeRange(
    val rangeLabel: String,
    val episodes: List<Episode>
  )

  override fun onClick(episode: Episode) {
    viewModel.saveHistory(episode.toMediaItem(shortItem as AVPMediaItem.SeasonItem))
  }
}
