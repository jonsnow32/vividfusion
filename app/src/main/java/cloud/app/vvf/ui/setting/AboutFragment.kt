package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cloud.app.vvf.R
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject


class AboutFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.about)
  override val transitionName = "about"
  override val container = { AboutPreference() }

  class AboutPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      preferenceManager.sharedPreferencesName = context.packageName
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val preferences = preferenceManager.sharedPreferences ?: return

      val screen = preferenceManager.createPreferenceScreen(context)
      preferenceScreen = screen

      Preference(context).apply {
        val version = context.packageManager
          .getPackageInfo(context.packageName, 0)
          .versionName
        title = getString(R.string.version)
        summary = version
        icon = AppCompatResources.getDrawable(context, R.drawable.outline_numbers_24)
        layoutResource = R.layout.preference
        isSelectable = false
        screen.addPreference(this)
      }

      Preference(context).apply {
        key = getString(R.string.pref_github)
        title = getString(R.string.github)
        summary = preferences.getString(key, "https://github.com/jonsnow32/vividfusion")
        layoutResource = R.layout.preference
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
          Utils.launchBrowser(context, summary.toString())
          true
        }
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_github_logo)
        screen.addPreference(this)
      }
      Preference(context).apply {
        key = getString(R.string.pref_privacy_policy)
        title = getString(R.string.privacy_policy)
        summary = preferences.getString(key, "https://jonsnow32.github.io/vividfusion/privacy.html")
        layoutResource = R.layout.preference
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
          Utils.launchBrowser(context, summary.toString())
          true
        }
        icon = AppCompatResources.getDrawable(context, R.drawable.round_privacy_tip_24)
        screen.addPreference(this)
      }
    }
  }
}
