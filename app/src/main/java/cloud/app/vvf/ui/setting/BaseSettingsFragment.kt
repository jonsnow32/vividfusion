package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentSettingsBinding
import cloud.app.vvf.utils.EMULATOR
import cloud.app.vvf.utils.TV
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.isLayout
import cloud.app.vvf.utils.setupTransition
import com.google.android.material.appbar.AppBarLayout
import java.util.prefs.Preferences

abstract class BaseSettingsFragment : Fragment() {

  abstract val title: String?
  abstract val transitionName: String?
  abstract val container: () -> Fragment

  protected var binding: FragmentSettingsBinding by autoCleared()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentSettingsBinding.inflate(inflater, container, false)
    return binding.root
  }


  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupTransition(view)
    applyInsetsMain(binding.appBarLayout, binding.fragmentContainer)
    setToolBarScrollFlags()
    setUpToolbar(title ?: resources.getString(R.string.settings))

    childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, container())
      .commit()

//    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
//      override fun handleOnBackPressed() {
//        // Handle the back button press in your fragment
//        parentFragmentManager.popBackStack() // or custom behavior
//      }
//    })
  }

  protected fun setToolBarScrollFlags() {
    if (context?.isLayout(TV or EMULATOR) == true) {
      binding.toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
      }
    }
  }

  protected fun setUpToolbar(title: String) {
    binding.toolbar.apply {
      setTitle(title)
      val canNavigateBack = (this@BaseSettingsFragment !is SettingsRootFragment) && (parentFragment?.childFragmentManager?.backStackEntryCount ?: 0) > 0
      if (canNavigateBack) {
        setNavigationIcon(R.drawable.ic_back)
        setNavigationOnClickListener {
          parentFragment?.childFragmentManager?.popBackStack()
        }
      } else {
        navigationIcon = null
      }
    }
  }

  protected fun setMenuToolbar(menuID: Int, onMenuItemClickListener: Toolbar.OnMenuItemClickListener) {
    binding.toolbar.apply {
      menu.clear() // Clear existing menu items to prevent duplicates
      if (menuID != 0) {
        inflateMenu(menuID) // Inflate the provided menu resource
        setOnMenuItemClickListener(onMenuItemClickListener) // Set click listener
      }
    }
  }

}
