package cloud.app.avp.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.avp.AVPApplication.Companion.applyUiChanges
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.utils.MaterialListPreference
import cloud.app.avp.utils.setupTransition
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.createSnack

class UiFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.ui)
  override val transitionName = "about"
  override val creator = { UiPreference() }

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

  class UiPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      preferenceManager.sharedPreferencesName = context.packageName
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val screen = preferenceManager.createPreferenceScreen(context)
      val preferences = preferenceManager.sharedPreferences ?: return
      preferenceScreen = screen
      fun uiListener(block: (Any) -> Unit = {}) =
        Preference.OnPreferenceChangeListener { pref, new ->
          if(pref.key.equals(getString(R.string.pref_theme_key)))
            applyUiChanges(new as String)
          //createSnack(message)
          block(new)
          true
        }


      MaterialListPreference(context).apply {
        key = getString(R.string.pref_theme_key)
        title = getString(R.string.theme)
        summary = getString(R.string.theme_summary)
        layoutResource = R.layout.preference
        isIconSpaceReserved = false

        entries = context.resources.getStringArray(R.array.themes)
        entryValues = arrayOf("light", "dark", "system")
        value = preferences.getString(key, "system")
        onPreferenceChangeListener = uiListener()
        screen.addPreference(this)
      }


      PreferenceCategory(context).apply {
        title = getString(R.string.animations)
        key = "animation"
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.pref_animations_key)
          title = getString(R.string.animations)
          summary = getString(R.string.animations_summary)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          setDefaultValue(true)
          onPreferenceChangeListener = uiListener()
          addPreference(this)
        }

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.pref_shared_element_animations_key)
          title = getString(R.string.shared_element_transitions)
          summary = getString(R.string.shared_element_transitions_summary)
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
