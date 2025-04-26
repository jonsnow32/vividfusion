package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.utils.setupTransition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class ContentSettingFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.contents)
  override val transitionName = "content_settings"
  override val container = { ContentPreference() }

  @AndroidEntryPoint
  class ContentPreference : PreferenceFragmentCompat() {
    @Inject lateinit var dataFlow: MutableStateFlow<AppDataStore>
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      preferenceManager.sharedPreferencesName = context.packageName
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val screen = preferenceManager.createPreferenceScreen(context)
      preferenceScreen = screen

      fun uiListener(block: (Any) -> Unit = {}) =
        Preference.OnPreferenceChangeListener { pref, new ->
          block(new)
          true
        }
      PreferenceCategory(context).apply {
        title = getString(R.string.tv_shows)
        key = getString(R.string.pref_tv_show_settings)
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)

        SwitchPreferenceCompat(context).apply {
          key = getString(R.string.show_special_season)
          title = getString(R.string.show_special_season)
          layoutResource = R.layout.preference_switch
          isIconSpaceReserved = false
          setDefaultValue(true)
          onPreferenceChangeListener = uiListener()
          addPreference(this)
        }
      }

      ListPreference(context).apply {
        key = getString(R.string.pref_region)
        title = getString(R.string.region)
        layoutResource = R.layout.preference
        screen.addPreference(this)
      }
    }
  }
}
