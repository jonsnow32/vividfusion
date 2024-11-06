package cloud.app.avp.ui.setting

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.avp.AVPApplication.Companion.applyUiChanges
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContentSettingFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.ui)
  override val transitionName = "content_settings"
  override val creator = { ContentPreference() }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupTransition(view)

    applyInsets {
      binding.appBarLayout.setPadding(0, it.top, 0, 0)
      binding.fragmentContainer.setPadding(0, 0, 0, it.bottom)
    }
    setToolBarScrollFlags()
    setUpToolbar(title)

    childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, creator())
      .commit()
  }

  class ContentPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      val screen = preferenceManager.createPreferenceScreen(context)
      val preferences = preferenceManager.sharedPreferences ?: return
      preferenceScreen = screen

      fun uiListener(block: (Any) -> Unit = {}) =
        Preference.OnPreferenceChangeListener { pref, new ->
          block(new)
          true
        }

      PreferenceCategory(context).apply {
        title = getString(R.string.shows)
        key = getString(R.string.tv_show_settings_key)
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.show_special_season_key)
          title = getString(R.string.show_special_season)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          setDefaultValue(true)
          onPreferenceChangeListener = uiListener()
          addPreference(this)
        }
      }
    }
  }
}
