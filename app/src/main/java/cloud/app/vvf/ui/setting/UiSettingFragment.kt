package cloud.app.vvf.ui.setting

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.VVFApplication.Companion.applyUiChanges
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.utils.ColorListPreference
import cloud.app.vvf.utils.MaterialListPreference
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UiSettingFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.ui)
  override val transitionName = "ui_settings"
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
      val screen = preferenceManager.createPreferenceScreen(context)
      val preferences = preferenceManager.sharedPreferences ?: return
      preferenceScreen = screen
      fun uiListener(block: (Any) -> Unit = {}) =
        Preference.OnPreferenceChangeListener { pref, new ->
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
        onPreferenceChangeListener = uiListener {
          activity?.application?.applyUiChanges(preferences, it as String, currentActivity = activity)
        }
        screen.addPreference(this)
      }

      SwitchPreferenceCompat(context).apply {
        key = getString(R.string.enable_dynamic_color_key)
        title = getString(R.string.enable_dynamic_color)
        summary = getString(R.string.dynamic_color_summary)
        layoutResource = R.layout.preference_switch
        isIconSpaceReserved = false
        setDefaultValue(false)
        onPreferenceChangeListener = uiListener {
          activity?.application?.applyUiChanges(preferences, currentActivity = activity)
          screen.findPreference<Preference>(getString(R.string.dynamic_color_key))?.isEnabled = it as Boolean
        }
        screen.addPreference(this)
      }

      ColorListPreference(this@UiPreference).apply {
        key = getString(R.string.dynamic_color_key)
        isEnabled = preferences.getBoolean(getString(R.string.enable_dynamic_color_key), false)
        listener = ColorListPreference.Listener {
          val themeColor = preferences.getString(getString(R.string.pref_theme_key), "system")
          activity?.application?.applyUiChanges(preferences, themeColor, it, currentActivity = activity)
        }
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
