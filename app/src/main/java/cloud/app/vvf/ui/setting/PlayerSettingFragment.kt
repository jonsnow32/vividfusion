package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.R
import cloud.app.vvf.utils.MaterialListPreference
import cloud.app.vvf.utils.MaterialSliderPreference
import cloud.app.vvf.utils.UIHelper.hasNotch


class PlayerSettingFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.playback)
  override val transitionName = "player_settings"
  override val container = { UiPreference() }

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
          block(new)
          true
        }
      PreferenceCategory(context).apply {
        title = getString(R.string.playback)
        key = "player_ui"
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.pref_key_overlap_notch)
          title = getString(R.string.title_overlap_notch)
          summary = getString(R.string.summary_overlap_notch)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          setDefaultValue(true)
          screen.addPreference(this)
        }

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.pref_zoom_control)
          title = getString(R.string.zoom_gesture)
          summary = getString(R.string.zoom_gesture_description)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          setDefaultValue(true)
          screen.addPreference(this)
        }

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.pref_use_swipe_control)
          title = getString(R.string.swipe_gesture)
          summary = getString(R.string.swipe_gesture_description)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          setDefaultValue(true)
          screen.addPreference(this)
        }

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.pref_use_seek_control)
          title = getString(R.string.seek_gesture)
          summary = getString(R.string.seek_gesture_description)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          setDefaultValue(true)
          screen.addPreference(this)
        }

        MaterialSliderPreference(context, 10, 60, 5, allowOverride = true).apply {
          key = getString(R.string.pref_seek_speed)
          title = getString(R.string.seek_speed)
          summary = getString(R.string.seek_speed_description)
          isIconSpaceReserved = false
          setDefaultValue(20)
          screen.addPreference(this)
        }



      }
      PreferenceCategory(context).apply {
        title = getString(R.string.subtitle)
        key = "player_subtitle"
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)

        MaterialListPreference(context).apply {
          key = getString(R.string.pref_subtitle_chartset)
          title = getString(R.string.subtitle_text_encoding)
          summary = preferences.getString(key, "Auto detect")
          layoutResource = R.layout.preference
          isIconSpaceReserved = false

          entries = context.resources.getStringArray(R.array.charsets_list)
          entryValues = context.resources.getStringArray(R.array.charsets_list)
          value = preferences.getString(key, "Auto detect")
          onPreferenceChangeListener = uiListener {

          }
          screen.addPreference(this)
        }
      }
    }
  }
}
