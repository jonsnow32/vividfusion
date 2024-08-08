package cloud.app.avp.ui.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cloud.app.avp.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentHomeBinding
import cloud.app.avp.databinding.FragmentShowsBinding
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.navigate
import cloud.app.avp.utils.observe
import cloud.app.avp.utils.setupTransition
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowsFragment : Fragment() {
  private var binding by autoCleared<FragmentShowsBinding>()
  private val viewModel by viewModels<ShowsViewModel>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentShowsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    applyInsetsMain(binding.appBarLayoutCustom, binding.recyclerView)
    val tabLayout = binding.tabLayout

    val tabListener = object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        viewModel.selectedTab = tab.text.toString();
      }

      override fun onTabUnselected(tab: TabLayout.Tab) = Unit
      override fun onTabReselected(tab: TabLayout.Tab) = Unit
    }
    tabLayout.addOnTabSelectedListener(tabListener)

    binding.btnSettings.setOnClickListener {
      navigate(R.id.settingsFragment)
    }
    observe(viewModel.genres) { genres ->
      tabLayout.removeAllTabs()
      genres.forEach { genre ->
        val tab = tabLayout.newTab()
        tab.text = genre
        tabLayout.addTab(tab, viewModel.selectedTab == genre)
      }
    }
  }
}
