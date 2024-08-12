package cloud.app.avp.ui.setting

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import cloud.app.avp.R
import cloud.app.avp.ui.extension.ExtensionViewModel
import cloud.app.avp.utils.MaterialListPreference
import cloud.app.avp.utils.MaterialMultipleChoicePreference
import cloud.app.avp.utils.MaterialTextInputPreference
import cloud.app.common.models.ExtensionType
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingCategory
import cloud.app.common.settings.SettingItem
import cloud.app.common.settings.SettingList
import cloud.app.common.settings.SettingMultipleChoice
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.SettingTextInput
import cloud.app.plugger.PluginMetadata

class ExtensionFragment : BaseSettingsFragment() {
    private val args by lazy { requireArguments() }
    private val name by lazy { args.getString("name")!! }
    private val id by lazy { args.getString("id")!! }
    private val type by lazy { args.getString("type")!! }
    override val title get() = getString(R.string.name_settings, name)
    override val transitionName get() = id
    override val creator = { ExtensionPreference.newInstance(id, type) }

    class ExtensionPreference : PreferenceFragmentCompat() {

        private val args by lazy { requireArguments() }
        private val extensionId by lazy { args.getString("extensionId")!! }
        private val extensionType: ExtensionType by lazy {
            val type = args.getString("extensionType")!!
            ExtensionType.valueOf(type)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = "$extensionType-$extensionId"
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            val viewModel by activityViewModels<ExtensionViewModel>()
//            val client = viewModel.run {
//                when (extensionType) {
//                    ExtensionType.MUSIC -> extensionListFlow.getExtension(extensionId)?.client
//                    ExtensionType.TRACKER -> trackerListFlow.getExtension(extensionId)?.client
//                    ExtensionType.LYRICS -> lyricsListFlow.getExtension(extensionId)?.client
//                }
//            }
//
//            client ?: return
//            client.settingItems.forEach { setting ->
//                setting.addPreferenceTo(screen)
//            }
        }

        private fun Setting.addPreferenceTo(preferenceGroup: PreferenceGroup) {
            when (this) {
                is SettingCategory -> {
                    PreferenceCategory(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference_category
                        preferenceGroup.addPreference(it)

                        this.items.forEach { item ->
                            item.addPreferenceTo(it)
                        }
                    }
                }

                is SettingItem -> {
                    Preference(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingSwitch -> {
                    SwitchPreferenceCompat(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary
                        it.setDefaultValue(this.defaultValue)

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference_switch
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingList -> {
                    MaterialListPreference(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key
                        defaultEntryIndex?.let { index ->
                            it.setDefaultValue(this.entryValues[index])
                        }
                        it.entries = this.entryTitles.toTypedArray()
                        it.entryValues = this.entryValues.toTypedArray()

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingMultipleChoice -> {
                    MaterialMultipleChoicePreference(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key
                        defaultEntryIndices?.let { indices ->
                            it.setDefaultValue(indices.mapNotNull { index ->
                                entryValues.getOrNull(index)
                            }.toSet())
                        }
                        it.entries = this.entryTitles.toTypedArray()
                        it.entryValues = this.entryValues.toTypedArray()

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingTextInput -> {
                    MaterialTextInputPreference(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary
                        it.text = this.defaultValue

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }

                else -> throw IllegalArgumentException("Unsupported setting type")
            }
        }

        companion object {
            fun newInstance(id: String, type: String): ExtensionPreference {
                val bundle = Bundle().apply {
                    putString("extensionId", id)
                    putString("extensionType", type)
                }
                return ExtensionPreference().apply {
                    arguments = bundle
                }
            }
        }
    }
}
