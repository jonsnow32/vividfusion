package cloud.app.avp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.avp.MainActivityViewModel
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentMainBinding
import cloud.app.avp.ui.main.home.HomeFragment
import cloud.app.avp.ui.main.library.LibraryFragment
import cloud.app.avp.ui.main.search.SearchFragment
import cloud.app.avp.ui.setting.SettingsFragment
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.setupTransition
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
  private var binding by autoCleared<FragmentMainBinding>()
  private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()

  private var selectedItemId: Int = R.id.homeFragment

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMainBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTransition(view)
    val navView = binding.navView as NavigationBarView

    if (savedInstanceState != null) {
      selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.homeFragment)
      val currentFragment = childFragmentManager.findFragmentById(R.id.vpContainer)
      if (currentFragment == null) {
        showFragment(selectedItemId)
      }
    } else {
      showFragment(selectedItemId) // Show initial fragment
    }

    navView.setOnItemSelectedListener { menuItem ->
      if (menuItem.itemId != selectedItemId) {
        selectedItemId = menuItem.itemId
        showFragment(selectedItemId)
      }

//      navView.selectedItemId = R.id.homeFragment
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
    navView.requestFocus()
  }

  private fun showFragment(@IdRes id: Int) {
    childFragmentManager.beginTransaction()
      .replace(R.id.vpContainer, createFragment(id))
      .commit()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt("selectedItemId", selectedItemId)
  }

  val fragments = mutableMapOf<Int, Fragment>()
  private fun createFragment(@IdRes id: Int): Fragment {
    if (fragments.containsKey(id)) return fragments[id]!!

    val fragment = when (id) {
      R.id.homeFragment -> HomeFragment()
      R.id.searchFragment -> SearchFragment()
      R.id.libraryFragment -> LibraryFragment()
      R.id.settingsFragment -> SettingsFragment()
      else -> {
        throw IllegalArgumentException("Invalid position")
      }
    }
    fragments[id] = fragment
    return fragment
  }


}
