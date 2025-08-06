package cloud.app.vvf.ui.main.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.R
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.databinding.FragmentLibraryBinding
import cloud.app.vvf.ui.main.configureFeedUI
import cloud.app.vvf.ui.widget.dialog.account.AccountDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.firstVisible
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.scrollTo
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LibraryFragment : Fragment() {
  private val parent get() = parentFragment as Fragment

  private var binding by autoCleared<FragmentLibraryBinding>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentLibraryBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val viewModel by parent.viewModels<LibraryViewModel>()

    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.appBarLayoutCustom, binding.recyclerView)
    configureFeedUI<DatabaseClient>(
      R.string.home,
      viewModel,
      binding.recyclerView,
      binding.swipeRefresh,
      binding.tabLayout
    )

    binding.recyclerView.scrollTo(viewModel.recyclerPosition, viewModel.recyclerOffset) {

    }

    binding.accountBtn.setOnClickListener {
      AccountDialog.newInstance(getString(R.string.account)) {

      }.show(parentFragmentManager)
    }

    observe(viewModel.dataFlow) {
      binding.accountImage.setImageResource(it.account.avatar)
    }
  }
  override fun onStop() {
    val viewModel by parent.viewModels<LibraryViewModel>()
    val position = binding.recyclerView.firstVisible();
    viewModel.recyclerPosition = position
    viewModel.recyclerOffset = binding.recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.top ?: 0
    super.onStop()
  }
}
