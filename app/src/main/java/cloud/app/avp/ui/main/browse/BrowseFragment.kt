package cloud.app.avp.ui.main.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentBrowseBinding
import cloud.app.avp.ui.media.MediaClickListener
import cloud.app.avp.ui.media.MediaItemAdapter
import cloud.app.avp.ui.setting.SettingsFragment
import cloud.app.avp.utils.FastScrollerHelper
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.configure
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.setupTransition
import cloud.app.common.models.AVPMediaItem
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt


@AndroidEntryPoint
class BrowseFragment : Fragment(), MediaItemAdapter.Listener {
  private var binding by autoCleared<FragmentBrowseBinding>()
  private val activityViewModel by activityViewModels<BrowseViewModel>()
  private val viewModel by viewModels<BrowseViewModel>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentBrowseBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)

    applyInsets {
      binding.appBarLayoutCustom.setPadding(0, it.top, 0, 0)
      binding.recyclerView.setPadding(0, 0, 0, it.bottom)
    }


    binding.btnSettings.setOnClickListener {
      navigate(SettingsFragment())
    }
    FastScrollerHelper.applyTo(binding.recyclerView)
    if (viewModel.moreFlow == null) {
      val category = activityViewModel.moreFlow ?: return
      activityViewModel.moreFlow = null
      viewModel.moreFlow = category
      viewModel.title = activityViewModel.title;
      viewModel.initialize()
    }

    binding.title.text = viewModel.title


    context?.let { ctx ->
      var viewWidth = view.width
      if (viewWidth <= 0) {
        val displayMetrics = ctx.resources.displayMetrics
        viewWidth = displayMetrics.widthPixels
      }
      val span = viewWidth / ctx.resources.getDimension(R.dimen.media_width).roundToInt()
      (binding.recyclerView.layoutManager as GridLayoutManager).spanCount = span



      val itemWidth = (viewWidth / span) - ctx.resources.getDimension(R.dimen.media_margin) * 4

      val itemHeight = itemWidth * 3 / 2 + ctx.resources.getDimension(R.dimen.media_title_height)

      val adapter = MediaItemAdapter(MediaClickListener(this.parentFragmentManager), view.transitionName, "", itemWidth.roundToInt(), itemHeight.roundToInt())
      //val concatAdapter = adapter.withLoaders()
      binding.recyclerView.adapter = adapter

      binding.swipeRefresh.configure {
        //adapter.refresh()
        viewModel.refresh()
      }

      observe(viewModel.loading) {
        binding.swipeRefresh.isRefreshing = it
      }


      observe(viewModel.flow) { data ->
        adapter.submit(data)
      }

    }
  }


  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {
    TODO("Not yet implemented")
  }

  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }

  override fun onFocusChange(clientId: String?, item: AVPMediaItem, hasFocus: Boolean) {

  }


}
