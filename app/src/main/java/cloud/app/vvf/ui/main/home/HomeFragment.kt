package cloud.app.vvf.ui.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentHomeBinding
import cloud.app.vvf.ui.main.configureFeedUI
import cloud.app.vvf.ui.setting.SettingsFragment
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.firstVisible
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.scrollTo
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
  private var binding by autoCleared<FragmentHomeBinding>()
  private val viewModel by activityViewModels<HomeViewModel>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.appBarLayoutCustom, binding.recyclerView)
    configureFeedUI(
      R.string.home,
      viewModel,
      binding.recyclerView,
      binding.swipeRefresh,
      binding.tabLayout
    )

    binding.recyclerView.scrollTo(viewModel.recyclerPosition, viewModel.recyclerOffset) {

    }

    binding.btnSettings.setOnClickListener {
      navigate(SettingsFragment())
    }

    binding.searchHome.setOnClickListener {
      //findNavController().navigate(R.id.action_mainFragment_to_searchFragment)
    }
  }
  override fun onStop() {
    val position = binding.recyclerView.firstVisible();
    viewModel.recyclerPosition = position
    viewModel.recyclerOffset = binding.recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.top ?: 0
    super.onStop()
  }
}
