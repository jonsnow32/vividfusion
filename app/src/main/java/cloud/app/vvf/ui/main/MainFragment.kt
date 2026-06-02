package cloud.app.vvf.ui.main

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.MainActivityViewModel
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentMainBinding
import cloud.app.vvf.ui.download.DownloadsFragment
import cloud.app.vvf.ui.main.files.FilesFragment
import cloud.app.vvf.ui.main.networkstream.NetworkStreamFragment
import cloud.app.vvf.ui.setting.SettingsRootFragment
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.setupTransition
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
  private var binding by autoCleared<FragmentMainBinding>()
  private lateinit var mainActivityViewModel: MainActivityViewModel
  private var selectedItemId: Int = R.id.networkStreamFragment

  private var navInsets: MainActivityViewModel.Insets = MainActivityViewModel.Insets(0, 0, 0, 0)

  override fun onAttach(context: Context) {
    super.onAttach(context)
    mainActivityViewModel = ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
  }

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

    selectedItemId = savedInstanceState?.getInt("selectedItemId", R.id.networkStreamFragment)
      ?: R.id.networkStreamFragment

    // Show the selected tab (add/show/hide keeps fragments alive so launchers stay registered)
    showTab(selectedItemId)

    navView.setOnItemSelectedListener { menuItem ->
      if (menuItem.itemId != selectedItemId) {
        selectedItemId = menuItem.itemId
        showTab(selectedItemId)
      }
      true
    }

    val isRail = binding.navView is NavigationRailView
    navView.post {
      val insets = requireContext().resources.run {
        val height = getDimensionPixelSize(R.dimen.nav_height)
        if (!isRail) MainActivityViewModel.Insets(bottom = height)
        else MainActivityViewModel.Insets(start = height)
      }
      navInsets = insets
      mainActivityViewModel.setNavInsets(navInsets)
    }
    navView.requestFocus()
  }

  /**
   * Uses add/show/hide so each tab's Fragment is never destroyed when switching tabs.
   * This keeps ActivityResultLaunchers registered throughout the session.
   */
  @OptIn(UnstableApi::class)
  private fun showTab(@IdRes id: Int) {
    val transaction = childFragmentManager.beginTransaction()

    val target = childFragmentManager.findFragmentByTag(id.toString())
      ?: createFragment(id).also { transaction.add(R.id.vpContainer, it, id.toString()) }

    // Hide every other tab fragment
    childFragmentManager.fragments.forEach { fragment ->
      if (fragment.tag != id.toString()) transaction.hide(fragment)
    }

    transaction.show(target).commitNow()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt("selectedItemId", selectedItemId)
  }

  @OptIn(UnstableApi::class)
  private fun createFragment(@IdRes id: Int): Fragment = when (id) {
    R.id.networkStreamFragment -> NetworkStreamFragment()
    R.id.downloadsFragment -> DownloadsFragment()
    R.id.filesFragment -> FilesFragment()
    R.id.settingsFragment -> SettingsRootFragment()
    else -> throw IllegalArgumentException("Invalid nav item: $id")
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
  }
}
