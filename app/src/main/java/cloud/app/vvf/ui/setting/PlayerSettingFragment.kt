package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.R
import cloud.app.vvf.VVFApplication.Companion.applyUiChanges
import cloud.app.vvf.utils.MaterialListPreference
import cloud.app.vvf.utils.MaterialSliderPreference
import cloud.app.vvf.utils.SubtitleHelper
import cloud.app.vvf.utils.TV
import cloud.app.vvf.utils.isLayout
import kotlin.math.max


class PlayerSettingFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.playback)
  override val transitionName = "player_settings"
  override val container = { UiPreference() }

  class UiPreference : PreferenceFragmentCompat() {
    @OptIn(UnstableApi::class)
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
        title = getString(R.string.player)
        key = "player_ui"
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)

        MaterialListPreference(context).apply {
          key = getString(R.string.pref_prefer_device_decoders)
          title = getString(R.string.prefer_device_decoders)
          layoutResource = R.layout.preference
          isIconSpaceReserved = false
          entries = context.resources.getStringArray(R.array.refer_decoder_names)
          entryValues = context.resources.getStringArray(R.array.refer_decoder_values)
          value = preferences.getString(
            key,
            if (context.isLayout(TV)) "device_decoders_only" else "prefer_device_decoders"
          ).toString()
          summary = getString(R.string.prefer_device_decoders)
          screen.addPreference(this)
        }

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
          suffixSummary = getString(R.string.seconds)
          isIconSpaceReserved = false
          setDefaultValue(20)
          screen.addPreference(this)
        }

        PreferenceCategory(context).apply {
          title = getString(R.string.cache)
          key = "player_cache"
          isIconSpaceReserved = false
          layoutResource = R.layout.preference_category
          screen.addPreference(this)

          MaterialSliderPreference(context, 100, 1000, 1, allowOverride = true).apply {
            key = getString(R.string.pref_video_cache_size_on_disk)
            title = getString(R.string.video_cache_size_on_disk)
            summary = getString(R.string.video_cache_disk_description)
            suffixSummary = "MB"
            isIconSpaceReserved = false
            setDefaultValue(0)
            screen.addPreference(this)
          }

          MaterialSliderPreference(context, 100, 1000, 1, allowOverride = true).apply {
            key = getString(R.string.pref_video_cache_size_on_ram)
            title = getString(R.string.video_cache_size_on_disk)
            summary = getString(R.string.video_cache_disk_description)
            suffixSummary = "MB"
            isIconSpaceReserved = false
            setDefaultValue(0)
            screen.addPreference(this)
          }

          MaterialSliderPreference(context, 10, 600, 10, allowOverride = true).apply {
            key = getString(R.string.pref_buffer_second)
            title = getString(R.string.video_buffer_second)
            summary = getString(R.string.video_buffer_second_description)
            suffixSummary = getString(R.string.seconds)
            isIconSpaceReserved = false
            setDefaultValue(DefaultLoadControl.DEFAULT_MAX_BUFFER_MS / 1000)
            screen.addPreference(this)
          }
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

        MaterialListPreference(context).apply {
          val tempLangs = appLanguages.toMutableList()
          val current = getCurrentLocale(context)
          val languageCodes = tempLangs.map { (_, _, iso) -> iso }
          val languageNames = tempLangs.map { (emoji, name, iso) ->
            val flag = emoji.ifBlank { SubtitleHelper.getFlagFromIso(iso) ?: "ERROR" }
            "$flag $name"
          }
          val index = max(languageCodes.indexOf(current), 0)

          key = getString(R.string.pref_subtitle_lang)
          title = getString(R.string.subtitle_language)
          summary = preferences.getString(key, languageNames[index])
          layoutResource = R.layout.preference
          isIconSpaceReserved = false

          entries = languageNames.toTypedArray()
          entryValues = languageCodes.toTypedArray()
          value = preferences.getString(key, languageNames[index])
          onPreferenceChangeListener = uiListener {
            activity?.application?.applyUiChanges(
              preferences,
              it as String,
              currentActivity = activity
            )
          }
          icon = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_public_24)
          screen.addPreference(this)
        }
      }
    }
  }
}
