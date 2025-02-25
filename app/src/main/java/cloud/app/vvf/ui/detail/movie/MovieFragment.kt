package cloud.app.vvf.ui.detail.movie

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
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.movie.Movie
import cloud.app.vvf.common.models.movie.Show
import cloud.app.vvf.databinding.FragmentMovieBinding
import cloud.app.vvf.datastore.helper.BookmarkItem
import cloud.app.vvf.ui.detail.TrailerDialog
import cloud.app.vvf.ui.detail.bind
import cloud.app.vvf.ui.media.MediaClickListener
import cloud.app.vvf.ui.media.MediaItemAdapter
import cloud.app.vvf.ui.paging.toFlow
import cloud.app.vvf.ui.stream.StreamFragment
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
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
  private val clientId by lazy { args.getString("clientId")!! }
  private val shortItem by lazy { args.getSerialized<AVPMediaItem.MovieItem>("mediaItem")!! }

  @Inject
  lateinit var preferences: SharedPreferences

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMovieBinding.inflate(inflater, container, false)
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


    binding.buttonMovieStreamingSearch.setOnClickListener {
      viewModel.fullMediaItem.value?.let {
        navigate(StreamFragment.newInstance(it))
      }
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

      bind(mediaItem)
      setupActorAdapter(mediaItem)
      setupRecommendationAdapter(mediaItem)
      setupTrailerAdapter(mediaItem)
    }
  }

  private fun setupTrailerAdapter(mediaItem: AVPMediaItem) {
    binding.buttonShowTrailer.setOnClickListener {
      val dialog = TrailerDialog.newInstance(mediaItem.generalInfo?.videos, clientId)
      dialog.show(parentFragmentManager, "")
    }
  }

  fun List<AVPMediaItem>.toPagedList() = PagedData.Single { this }


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
          MediaClickListener(parentFragmentManager), "", clientId
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
