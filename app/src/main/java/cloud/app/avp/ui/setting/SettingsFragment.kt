package cloud.app.avp.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import cloud.app.avp.MainViewModel.Companion.openFragment
import cloud.app.avp.R


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
                layoutResource = R.layout.preference
                screen.addPreference(this)
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
                else -> return false
            }

            val view = listView.findViewById<View>(preference.key.hashCode())
            parentFragment?.openFragment(fragment, view)
            return true
        }
    }

}

