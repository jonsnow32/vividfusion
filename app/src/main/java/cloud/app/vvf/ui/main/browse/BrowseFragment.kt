package cloud.app.vvf.ui.main.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentBrowseBinding
import cloud.app.vvf.ui.media.MediaClickListener
import cloud.app.vvf.ui.media.MediaItemAdapter
import cloud.app.vvf.utils.FastScrollerHelper
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.configure
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt


@AndroidEntryPoint
class BrowseFragment : Fragment() {
  private var binding by autoCleared<FragmentBrowseBinding>()
  private val activityViewModel by activityViewModels<BrowseViewModel>()
  private val viewModel by viewModels<BrowseViewModel>()

  private val args by lazy { requireArguments() }
  private val extensionId by lazy { args.getString("extensionId")!! }

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
      binding.topBar.setPadding(0, it.top, 0, 0)
      binding.recyclerView.setPadding(0, 0, 0, it.bottom)
    }

    FastScrollerHelper.applyTo(binding.recyclerView)

    if (viewModel.moreFlow == null) {
      val category = activityViewModel.moreFlow ?: return
      activityViewModel.moreFlow = null
      viewModel.setDataFlow(category)
      viewModel.title = activityViewModel.title;
    }

    binding.backBtn.setOnClickListener {
      parentFragmentManager.popBackStack()
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

      val adapter = MediaItemAdapter(this,
        view.transitionName,
        extensionId,
        itemWidth.roundToInt(),
        itemHeight.roundToInt()
      )
     val concatAdapter = adapter.withLoaders()
      binding.recyclerView.adapter = concatAdapter

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
}
