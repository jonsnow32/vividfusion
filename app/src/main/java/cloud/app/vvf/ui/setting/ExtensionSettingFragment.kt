package cloud.app.vvf.ui.setting

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.VVFApplication.Companion.noClient
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentExtensionBinding
import cloud.app.vvf.extension.getExtension
import cloud.app.vvf.extension.run
import cloud.app.vvf.ui.extension.ExtensionViewModel
import cloud.app.vvf.utils.EMULATOR
import cloud.app.vvf.utils.MaterialListPreference
import cloud.app.vvf.utils.MaterialMultipleChoicePreference
import cloud.app.vvf.utils.MaterialTextInputPreference
import cloud.app.vvf.utils.TV
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.isLayout
import cloud.app.vvf.utils.loadWith
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.settings.Setting
import cloud.app.vvf.common.settings.SettingCategory
import cloud.app.vvf.common.settings.SettingItem
import cloud.app.vvf.common.settings.SettingList
import cloud.app.vvf.common.settings.SettingMultipleChoice
import cloud.app.vvf.common.settings.SettingSwitch
import cloud.app.vvf.common.settings.SettingTextInput
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.launch

class ExtensionSettingFragment : Fragment() {

  companion object {
    fun newInstance(
      clientId: String, clientName: String, extensionType: ExtensionType
    ) = ExtensionSettingFragment().apply {
      arguments = Bundle().apply {
        putString("clientId", clientId)
        putString("clientName", clientName)
        putString("extensionType", extensionType.name)
      }
    }

    fun newInstance(extension: Extension<*>) =
      newInstance(extension.id, extension.name, extension.type)
  }

  private var binding by autoCleared<FragmentExtensionBinding>()
  private val viewModel by activityViewModels<ExtensionViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val clientName by lazy { args.getString("clientName")!! }
  private val extensionType by lazy {
    ExtensionType.valueOf(args.getString("extensionType")!!)
  }
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
      title = clientName
      setNavigationIcon(R.drawable.ic_back)
      setNavigationOnClickListener {
        activity?.onBackPressedDispatcher?.onBackPressed()
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

    val extension = when (extensionType) {
      ExtensionType.DATABASE -> viewModel.databaseExtensionListFlow.getExtension(clientId)
      ExtensionType.STREAM -> viewModel.streamExtensionListFlow.getExtension(clientId)
      ExtensionType.SUBTITLE -> viewModel.subtitleExtensionListFlow.getExtension(clientId)
    }

    if (extension == null) {
      createSnack(requireContext().noClient())
      parentFragmentManager.popBackStack()
      return
    }
    val extensionMetadata = extension.metadata

    extensionMetadata.iconUrl?.toImageHolder()
      .loadWith(binding.extensionIcon, R.drawable.ic_extension_24dp) {
        binding.extensionIcon.setImageDrawable(it)
      }

    binding.extensionDetails.text =
      "${extensionMetadata.version} • ${extensionMetadata.importType.name}"

    val byAuthor = getString(R.string.by_author, extensionMetadata.author)
    val typeString = getString(R.string.name_extension, extensionMetadata.importType.name)
    binding.extensionDescription.text =
      "$typeString\n\n${extensionMetadata.description}\n\n$byAuthor"

//    when (extensionMetadata.loginType) {
//      LoginType.API_KEY -> binding.extensionApiKey.root.visibility = View.VISIBLE
//      LoginType.USERNAME_PASSWORD -> binding.extensionLoginUser.root.visibility = View.VISIBLE
//      LoginType.WEBAUTH -> binding.extensionWebLogin.root.visibility = View.VISIBLE
//      LoginType.NONE -> {
//        //nothing to show
//      }
//    }

    childFragmentManager.beginTransaction()
      .add(R.id.settingsFragment, creator())
      .commit()
  }


  val creator =
    { ExtensionPreference.newInstance(clientId, extensionType.name) }

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
      viewModel.apply {
        val client = when (extensionType) {
          ExtensionType.DATABASE -> databaseExtensionListFlow.getExtension(extensionId)
          ExtensionType.STREAM -> streamExtensionListFlow.getExtension(extensionId)
          ExtensionType.SUBTITLE -> subtitleExtensionListFlow.getExtension(extensionId)
        }

        viewModelScope.launch {
          client?.run(throwableFlow) {
            defaultSettings.forEach { setting ->
              setting.addPreferenceTo(screen)
            }
//            val prefs = preferenceManager.sharedPreferences ?: return@run
//            val settings = toSettings(prefs)

          }
        }
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
            it.summary = this.summary
            defaultEntryIndices?.let { indices ->
              val defaultValue = indices.mapNotNull { index ->
                entryValues.getOrNull(index)
              }.toSet()
              it.setDefaultValue(defaultValue)
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
