package cloud.app.avp.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import cloud.app.avp.ExceptionActivity
import cloud.app.avp.R
import cloud.app.avp.utils.navigate


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
        title = getString(R.string.about)
        key = "about"
        summary = getString(R.string.about_summary)
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_info)
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
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_extension_24dp)
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
        "about" -> R.id.aboutFragment
        "extension" -> R.id.manageExtensionsFragment
        "ui" -> R.id.manageExtensionsFragment
        else -> return false
      }
      val view = listView.findViewById<View>(preference.key.hashCode())
      navigate(fragment, view)
      return true
    }
  }

}

