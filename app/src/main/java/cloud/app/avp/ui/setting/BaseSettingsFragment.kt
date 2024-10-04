package cloud.app.avp.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import cloud.app.avp.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentSettingsBinding
import cloud.app.avp.utils.EMULATOR
import cloud.app.avp.utils.PHONE
import cloud.app.avp.utils.TV
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.isLayout
import cloud.app.avp.utils.setupTransition
import com.google.android.material.appbar.AppBarLayout

abstract class BaseSettingsFragment : Fragment() {

  abstract val title: String?
  abstract val transitionName: String?
  abstract val creator: () -> Fragment

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

    childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, creator())
      .commit()
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
      if (context.isLayout(PHONE or EMULATOR)) {
        setNavigationIcon(R.drawable.ic_back)
        setNavigationOnClickListener {
          activity?.onBackPressedDispatcher?.onBackPressed()
        }
      }
    }
  }


}
