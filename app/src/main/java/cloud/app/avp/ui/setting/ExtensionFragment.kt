package cloud.app.avp.ui.setting

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.MainActivityViewModel.Companion.applyInsetsMain
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentExtensionBinding
import cloud.app.avp.ui.extension.ExtensionViewModel
import cloud.app.avp.utils.EMULATOR
import cloud.app.avp.utils.MaterialListPreference
import cloud.app.avp.utils.MaterialMultipleChoicePreference
import cloud.app.avp.utils.MaterialTextInputPreference
import cloud.app.avp.utils.PHONE
import cloud.app.avp.utils.TV
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.isLayout
import cloud.app.avp.utils.loadWith
import cloud.app.avp.utils.setupTransition
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.common.models.LoginType
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingCategory
import cloud.app.common.settings.SettingItem
import cloud.app.common.settings.SettingList
import cloud.app.common.settings.SettingMultipleChoice
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.SettingTextInput
import com.google.android.material.appbar.AppBarLayout

class ExtensionFragment : Fragment() {

  private var binding by autoCleared<FragmentExtensionBinding>()
  private val viewModel by activityViewModels<ExtensionViewModel>()

  private val args by lazy { requireArguments() }
  private val extensionClassName by lazy { args.getString("extensionClassName")!! }
  private val extensionMetadata by lazy { args.getParcel<ExtensionMetadata>("extensionMetadata")!! }


  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentExtensionBinding.inflate(inflater, container, false)
    return binding.root
  }

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupTransition(view)
    applyInsets {
      binding.appBarLayout.setPadding(0,it.top, 0,0)
    }

    if (context?.isLayout(TV or EMULATOR) == true) {
      binding.toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
      }
    }

    binding.toolbar.apply {
      title = extensionMetadata.name
      if (context.isLayout(PHONE or EMULATOR)) {
        setNavigationIcon(R.drawable.ic_back)
        setNavigationOnClickListener {
          activity?.onBackPressedDispatcher?.onBackPressed()
        }
      }

      setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menu_refresh -> {
            viewModel.refresh()
            true
          }
          else -> false
        }
      }
    }

    extensionMetadata.icon.toImageHolder()
      .loadWith(binding.extensionIcon, R.drawable.ic_extension_24dp) {
        binding.extensionIcon.setImageDrawable(it)
      }

    binding.extensionDetails.text =
      "${extensionMetadata.version} • ${extensionMetadata.type.name}"

    val byAuthor = getString(R.string.by_author, extensionMetadata.author)
    val typeString = getString(R.string.name_extension, extensionMetadata.type.name)
    binding.extensionDescription.text =
      "$typeString\n\n${extensionMetadata.description}\n\n$byAuthor"

    when (extensionMetadata.loginType) {
      LoginType.API_KEY -> binding.extensionApiKey.root.visibility = View.VISIBLE
      LoginType.USERNAME_PASSWORD -> binding.extensionLoginUser.root.visibility = View.VISIBLE
      LoginType.WEBAUTH -> binding.extensionWebLogin.root.visibility = View.VISIBLE
      LoginType.NONE -> {
        //nothing to show
      }
    }

    childFragmentManager.beginTransaction()
      .add(R.id.settingsFragment, creator())
      .commit()
  }


  val creator =
    { ExtensionPreference.newInstance(extensionClassName, extensionMetadata.type.name) }

  class ExtensionPreference : PreferenceFragmentCompat() {
    private val args by lazy { requireArguments() }
    private val className by lazy { args.getString("className")!! }
    private val extensionType: ExtensionType by lazy {
      val type = args.getString("extensionType")!!
      ExtensionType.valueOf(type)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      preferenceManager.sharedPreferencesName = "$extensionType-$className"
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val screen = preferenceManager.createPreferenceScreen(context)
      preferenceScreen = screen

      val viewModel by activityViewModels<ExtensionViewModel>()
      val client = viewModel.run {
        extensionFlowList.value.find { it.javaClass.toString() == className }
      }
      client ?: return
      client.defaultSettings.forEach { setting ->
        setting.addPreferenceTo(screen)
      }
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
          putString("className", id)
          putString("extensionType", type)
        }
        return ExtensionPreference().apply {
          arguments = bundle
        }
      }
    }
  }

}
