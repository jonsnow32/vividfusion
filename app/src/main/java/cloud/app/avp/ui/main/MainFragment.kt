package cloud.app.avp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import cloud.app.avp.MainActivityViewModel
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentMainBinding
import cloud.app.avp.ui.main.home.HomeFragment
import cloud.app.avp.ui.main.library.LibraryFragment
import cloud.app.avp.ui.main.search.SearchFragment
import cloud.app.avp.ui.setting.SettingsFragment
import cloud.app.avp.utils.SlideInPageTransformer
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.tv.FOCUS_SELF
import cloud.app.avp.utils.tv.setLinearListLayout
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.configureSnackBar
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
  private var binding by autoCleared<FragmentMainBinding>()
  private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMainBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val navView = binding.navView as NavigationBarView
    navView.setOnItemSelectedListener {
      childFragmentManager.beginTransaction()
        .replace(R.id.vpContainer, createFragment(it.itemId))
        .commit()
      true
    }

    val isRail = binding.navView is NavigationRailView
    navView.post {
      val insets = requireContext().resources.run {
        val height = getDimensionPixelSize(R.dimen.nav_height)
        if (!isRail) return@run MainActivityViewModel.Insets(bottom = height)
        else
          return@run MainActivityViewModel.Insets(start = height)
      }
      mainActivityViewModel.setNavInsets(insets)
    }
    navView.selectedItemId = R.id.homeFragment
    navView.requestFocus()
  }

  companion object {
    fun createFragment(@IdRes id: Int): Fragment {
      return when (id) {
        R.id.homeFragment -> HomeFragment()
        R.id.searchFragment -> SearchFragment()
        R.id.libraryFragment -> LibraryFragment()
        R.id.settingsFragment -> SettingsFragment()
        else -> {
          throw IllegalArgumentException("Invalid position")
        }
      }
    }
  }


}
