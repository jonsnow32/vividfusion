package cloud.app.avp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import cloud.app.avp.MainActivityViewModel
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentMainBinding
import cloud.app.avp.ui.main.home.HomeFragment
import cloud.app.avp.ui.main.library.LibraryFragment
import cloud.app.avp.ui.main.search.SearchFragment
import cloud.app.avp.utils.SlideInPageTransformer
import cloud.app.avp.utils.autoCleared
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

    val adapter = MainAdapter(this)
    binding.vpContainer.adapter = adapter
    binding.vpContainer.setPageTransformer(SlideInPageTransformer())
    binding.vpContainer.isUserInputEnabled = false

    val navView = binding.navView as NavigationBarView
    binding.vpContainer.registerOnPageChangeCallback(object : OnPageChangeCallback() {
      override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        when (position) {
          0 -> navView.selectedItemId = R.id.homeFragment
          1 -> navView.selectedItemId = R.id.searchFragment
          2 -> navView.selectedItemId = R.id.libraryFragment
        }
      }
    })

    binding.vpContainer.setCurrentItem(0, false)
    configureSnackBar(binding.navView)

    navView.setOnItemSelectedListener {
      when (it.itemId) {
        R.id.homeFragment -> binding.vpContainer.setCurrentItem(0, false)
        R.id.searchFragment -> binding.vpContainer.setCurrentItem(1, false)
        R.id.libraryFragment -> binding.vpContainer.setCurrentItem(2, false)

      }
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

  class MainAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
      return when (position) {

        0 -> HomeFragment()
        1 -> SearchFragment()
        2 -> LibraryFragment()
        else -> {
          throw IllegalArgumentException("Invalid position")
        }
      }
    }
  }

}
