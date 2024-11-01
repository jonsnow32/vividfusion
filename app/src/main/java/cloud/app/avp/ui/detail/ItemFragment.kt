package cloud.app.avp.ui.detail

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentItemBinding
import cloud.app.avp.ui.detail.show.season.SeasonAdapter
import cloud.app.avp.ui.media.MediaItemAdapter
import cloud.app.avp.ui.paging.toFlow
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.setupTransition
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.AVPMediaItem.Companion.toMediaItem
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.movie.Season
import cloud.app.common.models.movie.Show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
class ItemFragment : Fragment(), MediaItemAdapter.Listener {
  private var binding by autoCleared<FragmentItemBinding>()
  private val viewModel by viewModels<ItemViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val shortItem by lazy { args.getParcel<AVPMediaItem>("mediaItem")!! }

  @Inject
  lateinit var preferences: SharedPreferences

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentItemBinding.inflate(inflater, container, false)
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
      setupRecommendationAdapter(mediaItem)
    }

    observe(viewModel.seasons) { seasons ->
      setUpSeasons(seasons)
    }
  }

  fun List<AVPMediaItem>.toPagedList() = PagedData.Single { this }


  private fun setUpSeasons(seasons: List<Season>?) {
    binding.seasonHolder.isGone = seasons.isNullOrEmpty()
    seasons?.let {
      setupSortOptions(seasons)
    }
  }

  private fun setupSortOptions(seasons: List<Season>) {
    binding.seasonSortTxt.isGone = (seasons.size < 2)
    val oldSortMode =
      preferences.getInt(getString(R.string.episode_sort_key), R.string.ascending_sort)
    binding.seasonSortTxt.text = getString(oldSortMode)
    binding.seasonSortTxt.setCompoundDrawablesWithIntrinsicBounds(
      0, 0,
      if (oldSortMode == R.string.ascending_sort) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up,
      0
    )
    when (oldSortMode) {
      R.string.descending_sort -> binding.rvSeason.adapter =
        SeasonAdapter(seasons.sortedByDescending { it.number })
      else -> binding.rvSeason.adapter = SeasonAdapter(seasons.sortedBy { it.number })
    }

    binding.seasonSortTxt.setOnClickListener {
      val sortMode =
        preferences.getInt(getString(R.string.episode_sort_key), R.string.ascending_sort)
      val newSortMode =
        if (sortMode == R.string.descending_sort) R.string.ascending_sort else R.string.descending_sort

      when (newSortMode) {
        R.string.descending_sort ->
          (binding.rvSeason.adapter as SeasonAdapter).submitList(seasons.sortedByDescending { it.number })

        else ->
          (binding.rvSeason.adapter as SeasonAdapter).submitList(seasons.sortedBy { it.number })
      }

      binding.seasonSortTxt.setCompoundDrawablesWithIntrinsicBounds(
        0, 0,
        if (newSortMode == R.string.ascending_sort) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up,
        0
      )
      preferences.edit().putInt(getString(R.string.episode_sort_key), newSortMode).apply()
      binding.seasonSortTxt.text = getString(newSortMode)
    }
  }

  private fun loadInitialData() {
    viewModel.getItemDetails(shortItem)
    when (shortItem) {
      is AVPMediaItem.MovieItem -> {
        //viewModel.checkResume(shortItem.movie)
      }

      is AVPMediaItem.ShowItem -> {
      }

      is AVPMediaItem.ActorItem -> {

      }

      is AVPMediaItem.EpisodeItem -> {

      }

      else -> {

      }
    }
  }

  private fun setupActorAdapter(mediaItem: AVPMediaItem) {
    val actorAdapter = MediaItemAdapter(this, "", clientId)
    binding.rvActors.adapter = actorAdapter

    val actorList = mediaItem.generalInfo?.actors?.map { AVPMediaItem.ActorItem(it) }
    actorList?.let {
      lifecycleScope.launch {
        it.toPagedList().toFlow().collectLatest(actorAdapter::submit)
      }
    }
  }


  private fun setupRecommendationAdapter(item: AVPMediaItem) {
    val recommendations = item.recommendations?.map {
      when (it) {
        is Movie -> AVPMediaItem.MovieItem(it)
        is Show -> AVPMediaItem.ShowItem(it)
        else -> null
      }
    }?.filterNotNull()
    binding.recommendedTitle.isGone = recommendations.isNullOrEmpty()

    recommendations?.let {
      val recommendationAdapter =
        MediaItemAdapter(
          this, "", clientId
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

  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {
    val movieFragment = ItemFragment();
    movieFragment.arguments = bundleOf("mediaItem" to item, "clientId" to "clientID")
    navigate(
      movieFragment,
      transitionView
    )
  }


  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }

  override fun onFocusChange(clientId: String?, item: AVPMediaItem, hasFocus: Boolean) {
    //bind(item = item)
  }

}
