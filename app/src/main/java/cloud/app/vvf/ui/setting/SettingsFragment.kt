package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import cloud.app.vvf.R
import cloud.app.vvf.utils.navigate


class SettingsFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.settings)
  override val transitionName = "settings"
  override val creator = { SettingsPreference() }

  class SettingsPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      preferenceManager.sharedPreferencesName = context.packageName
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val screen = preferenceManager.createPreferenceScreen(context)
      preferenceScreen = screen
      fun Preference.add(block: Preference.() -> Unit = {}) {
        block()
        layoutResource = R.layout.custom_preference
        screen.addPreference(this)
      }

      TransitionPreference(context).add {
        title = getString(R.string.extensions)
        key = "extension"
        summary = getString(R.string.extension_summary)
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_extension_24dp)
      }

      TransitionPreference(context).add {
        title = getString(R.string.ui)
        key = "ui"
        summary = getString(R.string.ui_summary)
        icon = AppCompatResources.getDrawable(context, R.drawable.tune_24dp)
      }

      TransitionPreference(context).add {
        title = getString(R.string.contents)
        key = "content"
        summary = getString(R.string.content_setting_summary)
        icon = AppCompatResources.getDrawable(context, R.drawable.database_24dp)
      }

      TransitionPreference(context).add {
        title = getString(R.string.about)
        key = "about"
        summary = getString(R.string.about_summary)
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_info)
      }

    }

    class TransitionPreference(
      context: Context
    ) : Preference(context) {
      override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.id = key.hashCode()
        holder.itemView.transitionName = key
      }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
      val fragment = when (preference.key) {
        "about" -> AboutFragment()
        "extension" -> ManageExtensionsFragment()
        "ui" -> UiSettingFragment()
        "content" -> ContentSettingFragment()
        else -> null
      }
      fragment ?: return false

      val view = listView.findViewById<View>(preference.key.hashCode())
      parentFragment?.parentFragment?.navigate(fragment, view)
      return true
    }
  }

}

