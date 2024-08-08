package cloud.app.avp.ui.main.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentMoviesBinding
import cloud.app.avp.ui.main.media.MediaContainerAdapter
import cloud.app.avp.ui.main.media.MediaItemAdapter
import cloud.app.avp.utils.FastScrollerHelper
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.setupTransition
import cloud.app.avp.utils.tv.FOCUS_SELF
import cloud.app.avp.utils.tv.setLinearListLayout
import cloud.app.common.models.AVPMediaItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class MoviesFragment : Fragment(), MediaItemAdapter.Listener {
  private var binding by autoCleared<FragmentMoviesBinding>()
  private val activityViewModel by activityViewModels<MoviesViewModel>()
  private val viewModel by viewModels<MoviesViewModel>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMoviesBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)

    binding.btnSettings.setOnClickListener {
      navigate(R.id.settingsFragment)
    }
    FastScrollerHelper.applyTo(binding.recyclerView)
    if (viewModel.moreFlow == null) {
      val category = activityViewModel.moreFlow ?: return
      activityViewModel.moreFlow = null
      viewModel.moreFlow = category
      viewModel.initialize()
    }

    val adapter = MediaItemAdapter(this, view.transitionName, "")
    val concatAdapter = adapter.withLoaders()
    binding.recyclerView.adapter = adapter

    context?.let { ctx ->
      var viewWidth = view.width
      if (viewWidth <= 0) {
        val displayMetrics = ctx.resources.displayMetrics
        viewWidth = displayMetrics.widthPixels
      }
      val itemWidth =
        ctx.resources.getDimension(R.dimen.item_poster_width) + ctx.resources.getDimension(R.dimen.item_poster_margin) * 2
      val span = (viewWidth / itemWidth).roundToInt()
      (binding.recyclerView.layoutManager as GridLayoutManager).spanCount = span
    }

    observe(viewModel.flow) { data ->
      adapter.submit(data)
    }
  }


  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {
    TODO("Not yet implemented")
  }

  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }


}
