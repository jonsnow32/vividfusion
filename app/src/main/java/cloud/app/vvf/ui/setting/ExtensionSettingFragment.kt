package cloud.app.vvf.ui.setting

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.R
import cloud.app.vvf.VVFApplication.Companion.noClient
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.clients.user.LoginClient
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.settings.Setting
import cloud.app.vvf.common.settings.SettingCategory
import cloud.app.vvf.common.settings.SettingItem
import cloud.app.vvf.common.settings.SettingList
import cloud.app.vvf.common.settings.SettingMultipleChoice
import cloud.app.vvf.common.settings.SettingSwitch
import cloud.app.vvf.common.settings.SettingTextInput
import cloud.app.vvf.databinding.FragmentExtensionBinding
import cloud.app.vvf.extension.getExtension
import cloud.app.vvf.extension.isClient
import cloud.app.vvf.extension.runClient
import cloud.app.vvf.ui.extension.ExtensionViewModel
import cloud.app.vvf.ui.login.LoginUserBottomSheet.Companion.bind
import cloud.app.vvf.ui.login.LoginUserViewModel
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.MaterialListPreference
import cloud.app.vvf.utils.MaterialMultipleChoicePreference
import cloud.app.vvf.utils.MaterialTextInputPreference
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.loadWith
import cloud.app.vvf.utils.setupTransition
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack
import kotlinx.coroutines.launch

class ExtensionSettingFragment : BaseSettingsFragment() {

  override val title: String?
    get() = extensionName
  override val transitionName: String?
    get() = "extension_setting"
  override val container = { ExtensionDetail.newInstance(extensionId, extensionName) }
  private val args by lazy { requireArguments() }
  private val extensionId by lazy { args.getString("extensionId")!! }
  private val extensionName by lazy { args.getString("extensionName") }
  private val viewModel by activityViewModels<ExtensionViewModel>()

  companion object {
    fun newInstance(extensionId: String, extensionName: String? = null) =
      ExtensionSettingFragment().apply {
        arguments = Bundle().apply {
          putString("extensionId", extensionId)
          putString("extensionName", extensionName)
        }
      }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val extensionMetadata =
      viewModel.extListFlow.getExtension(extensionId)?.metadata ?: return
    setMenuToolbar(R.menu.extension_menu) {
      when (it.itemId) {
        R.id.menu_like -> {
          val listItems = extensionMetadata.types.map { it.name }
          SelectionDialog.single(
            listItems,
            -1,
            getString(R.string.select_type_you_vote),
            false
          ).show(parentFragmentManager) {
            val itemId = it?.getIntegerArrayList("selected_items")?.get(0)
            itemId?.let {
              viewModel.vote(extensionMetadata, extensionMetadata.types.get(itemId))
            }
          }
          true
        }
        else -> false
      }
    }
  }


  class ExtensionDetail : Fragment() {
    companion object {
      fun newInstance(
        extensionId: String, extensionName: String? = null
      ) = ExtensionDetail().apply {
        arguments = Bundle().apply {
          putString("extensionId", extensionId)
          putString("extensionName", extensionName)
        }
      }

      fun newInstance(extension: cloud.app.vvf.common.clients.Extension<*>) =
        newInstance(extension.id, extension.name)
    }

    private var binding by autoCleared<FragmentExtensionBinding>()
    private val viewModel by activityViewModels<ExtensionViewModel>()

    private val args by lazy { requireArguments() }
    private val extensionId by lazy { args.getString("extensionId")!! }
    private val extension by lazy { viewModel.extListFlow.getExtension(extensionId) }
    override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
      binding = FragmentExtensionBinding.inflate(inflater, container, false)
      return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      setupTransition(view)

      if (extension == null) {
        createSnack(requireContext().noClient())
        parentFragmentManager.popBackStack()
        return
      }

      val extensionMetadata = extension?.metadata ?: return

      fun updateText(enabled: Boolean) {
        binding.enabledText.text = getString(
          if (enabled) R.string.enabled else R.string.disabled
        )
      }
      binding.enabledSwitch.apply {
        updateText(extensionMetadata.enabled)
        isChecked = extensionMetadata.enabled
        setOnCheckedChangeListener { _, isChecked ->
          updateText(isChecked)
          viewModel.setExtensionEnabled(extensionMetadata.className, isChecked)
        }
        binding.enabledCont.setOnClickListener { toggle() }
      }




      extensionMetadata.iconUrl?.toImageHolder().loadWith(binding.extensionIcon)

      binding.extensionDetails.text = getString(
        R.string.extension_title,
        extensionMetadata.version,
        extensionMetadata.author,
        extensionMetadata.className
      )

      binding.extensionTypes.text = extensionMetadata.types.toString()
      binding.extensionDescription.text = extensionMetadata.description

      if (extension?.isClient<LoginClient>() == true) {
        val loginViewModel by activityViewModels<LoginUserViewModel>()
        loginViewModel.currentExtension.value = extension
        binding.extensionLoginUser.bind(this@ExtensionDetail) {}
      } else binding.extensionLoginUser.root.isVisible = false

      childFragmentManager.beginTransaction()
        .add(R.id.settingsFragment, creator())
        .commit()
    }


    val creator = { ExtensionPreference.newInstance(extensionId) }

    class ExtensionPreference : PreferenceFragmentCompat() {
      private val args by lazy { requireArguments() }
      private val extensionId by lazy { args.getString("extensionId")!! }

      override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceManager.sharedPreferencesName = context.packageName
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        val screen = preferenceManager.createPreferenceScreen(context)
        val preferences = preferenceManager.sharedPreferences ?: return
        preferenceScreen = screen

        val viewModel by activityViewModels<ExtensionViewModel>()
        viewModel.apply {
          viewModelScope.launch {
            viewModel.extListFlow.value?.runClient<BaseClient, Unit>(
              extensionId,
              throwableFlow
            ) {
              defaultSettings.forEach { setting ->
                setting.addPreferenceTo(screen)
              }
//            val prefs = preferenceManager.sharedPreferences ?: return@run
//            val settings = toSettings(prefs)

            }
          }
        }
      }

      private fun onSettingChanged() =
        Preference.OnPreferenceChangeListener { pref, new ->
          val viewModel by activityViewModels<ExtensionViewModel>()
          val client = viewModel.extListFlow.getExtension(extensionId)
          client?.instance?.value?.getOrNull()?.onSettingsChanged(pref.key, new)
          true
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
              it.onPreferenceChangeListener = onSettingChanged()
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
              it.onPreferenceChangeListener = onSettingChanged()
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
              it.onPreferenceChangeListener = onSettingChanged()
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
              it.onPreferenceChangeListener = onSettingChanged()
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
              it.onPreferenceChangeListener = onSettingChanged()
              preferenceGroup.addPreference(it)
            }
          }

          else -> throw IllegalArgumentException("Unsupported setting type")
        }
      }

      companion object {
        fun newInstance(id: String): ExtensionPreference {
          val bundle = Bundle().apply {
            putString("extensionId", id)
          }
          return ExtensionPreference().apply {
            arguments = bundle
          }
        }
      }
    }
  }


}
