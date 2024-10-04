package cloud.app.avp.ui.detail.show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentShowBinding
import cloud.app.avp.ui.detail.movie.MovieFragment
import cloud.app.avp.ui.media.MediaItemAdapter
import cloud.app.avp.ui.paging.toFlow
import cloud.app.avp.utils.TimeUtils.toLocalMonthYear
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.loadWith
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.onAppBarChangeListener
import cloud.app.avp.utils.roundTo
import cloud.app.avp.utils.setTextWithVisibility
import cloud.app.avp.utils.setupTransition
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShowFragment : Fragment(), MediaItemAdapter.Listener {
  private var binding by autoCleared<FragmentShowBinding>()
  private val viewModel by viewModels<ShowViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val shortShowItem by lazy { args.getParcel<AVPMediaItem.ShowItem>("mediaItem")!! }

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
      binding.appBarLayout.setPadding(0, it.top, 0, 0)
    }
    bind(shortShowItem)
    binding.appBarLayout.onAppBarChangeListener { offset ->
      binding.appbarOutline.alpha = offset
      binding.appbarOutline.isVisible = offset > 0
      //binding.toolBar.alpha = offset
    }

    binding.toolBar.setNavigationOnClickListener {
      parentFragmentManager.popBackStack()
    }

    binding.toolBar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.menu_home -> {
          parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
          true
        }

        else -> false
      }
    }

    viewModel.getFullShowItem(shortShowItem)
    observe(viewModel.fullMediaItem) {
      if (it == null || it !is AVPMediaItem.ShowItem) return@observe
      val showItem = it as AVPMediaItem.ShowItem
      bind(showItem)


      val actorAdapter = MediaItemAdapter(this, "", "clientID")
      binding.rvActors.adapter = actorAdapter;
      val actorList =
        showItem.show.generalInfo.actors?.map { actorData ->
          AVPMediaItem.ActorItem(actorData)
        }

      actorList?.apply {
        lifecycleScope.launch {
          val flow = toPagedList().toFlow();
          flow.collectLatest { pagingData ->
            actorAdapter.submit(pagingData)
          }
        }
      }

      val recommendationAdapter = MediaItemAdapter(this, "", "clientID")
      binding.rvRecommendedMedia.adapter = recommendationAdapter

      val recommendations =
        showItem.show.recommendations?.map { recommendationData ->
          AVPMediaItem.ShowItem(recommendationData)
        }

      recommendations?.apply {
        lifecycleScope.launch {
          val flow = toPagedList().toFlow();
          flow.collectLatest { pagingData ->
            recommendationAdapter.submit(pagingData)
          }
        }
      }
    }
  }

  fun List<AVPMediaItem>.toPagedList() = PagedData.Single { this }
  fun bind(item: AVPMediaItem.ShowItem) {
    item.backdrop.loadWith(binding.headerBackground, item.poster)
    binding.toolBar.title = item.title
    binding.mediaOverview.text = item.show.generalInfo.overview

    binding.genre1.setTextWithVisibility(item.show.generalInfo.genres?.firstOrNull())
    binding.genre2.setTextWithVisibility(item.show.generalInfo.genres?.getOrNull(1))
    binding.genre3.setTextWithVisibility(item.show.generalInfo.genres?.getOrNull(2))
    binding.contentRating.setTextWithVisibility(item.show.generalInfo.contentRating)
    binding.tagLine.setTextWithVisibility(item.show.tagLine)
    binding.mediaStatus.setTextWithVisibility(item.show.status.capitalize())

    binding.mediaReleaseYear.setTextWithVisibility(
      item.show.generalInfo.releaseDateMsUTC?.toLocalMonthYear()
    )
    binding.mediaRating.setTextWithVisibility(item.rating?.roundTo(1).toString())
    binding.toolBar.subtitle = item.show.status

//    binding.watchNow.setOnClickListener {
//      it.transitionName = "watchNow" + item.id
//      val streamFragment = StreamFragment()
//      streamFragment.arguments = bundleOf("mediaItem" to item, "clientId" to "clientID")
//      navigate(
//        streamFragment,
//        it
//      )
//    }
  }

  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {
    when (item) {
      is AVPMediaItem.MovieItem -> {
        val movieFragment = MovieFragment()
        movieFragment.arguments = bundleOf("mediaItem" to item, "clientId" to "clientID")
        navigate(
          movieFragment,
          transitionView
        )
      }

      is AVPMediaItem.ShowItem -> {
        val showFragment = ShowFragment()
        showFragment.arguments = bundleOf("mediaItem" to item, "clientId" to "clientID")
        navigate(
          showFragment,
          transitionView
        )
      }

      else -> throw Exception("Invalid media type")
    }

  }

  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }
}
