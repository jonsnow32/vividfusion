package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.BuildConfig
import cloud.app.vvf.R
import cloud.app.vvf.ads.AdTestFragment
import cloud.app.vvf.utils.navigate
import dagger.hilt.android.AndroidEntryPoint

class DevelopmentSettingFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.download)
  override val transitionName = "develop_settings"
  override val container = { UiPreference() }

  @AndroidEntryPoint
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
        title = getString(R.string.firebase)
        key = "fire_base"
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.pref_use_firebase_analytics)
          title = getString(R.string.firebase_analytics)
          summary = getString(R.string.firebase_analytics_summary)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          icon = AppCompatResources.getDrawable(context, R.drawable.ic_firebase)
          setDefaultValue(true)
          screen.addPreference(this)
        }

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.pref_use_show_app_crash_log)
          title = getString(R.string.show_app_crash_log)
          summary = getString(R.string.show_app_crash_log_summary)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          icon = AppCompatResources.getDrawable(context, R.drawable.outline_bug_report_24)
          setDefaultValue(true)
          screen.addPreference(this)
        }

        if(BuildConfig.DEBUG) {
          Preference(context).apply {
            key = "ads_test"
            title = "ads_test"
            summary = "goto ads test fragment"
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
              parentFragment?.navigate(AdTestFragment.newInstance())
              true
            }
            layoutResource = R.layout.preference
            icon = AppCompatResources.getDrawable(context, R.drawable.outline_ads_click_24)
            screen.addPreference(this)
          }
        }

      }
    }
  }

}
