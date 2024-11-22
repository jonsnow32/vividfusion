package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.utils.setupTransition

class AboutFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.about)
    override val transitionName = "about"
    override val creator = { AboutPreference() }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupTransition(view)

    applyInsets {
      binding.appBarLayout.setPadding(0,it.top, 0,0)
      binding.fragmentContainer.setPadding(0, 0, 0, it.bottom)
    }
    setToolBarScrollFlags()
    setUpToolbar(title)

    childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, creator())
      .commit()
  }
    class AboutPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = context.packageName
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            Preference(context).apply {
                val version = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                title = getString(R.string.version)
                summary = version
                layoutResource = R.layout.custom_preference
                isIconSpaceReserved = false
                isSelectable = false
                screen.addPreference(this)
            }
        }
    }
}
