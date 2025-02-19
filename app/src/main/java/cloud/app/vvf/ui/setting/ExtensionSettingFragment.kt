package cloud.app.vvf.ui.setting

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
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
import cloud.app.vvf.common.clients.user.LoginClient
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting
import cloud.app.vvf.common.settings.SettingCategory
import cloud.app.vvf.common.settings.SettingItem
import cloud.app.vvf.common.settings.SettingList
import cloud.app.vvf.common.settings.SettingMultipleChoice
import cloud.app.vvf.common.settings.SettingSwitch
import cloud.app.vvf.common.settings.SettingTextInput
import cloud.app.vvf.extension.isClient
import cloud.app.vvf.ui.login.LoginUserBottomSheet.Companion.bind
import cloud.app.vvf.ui.login.LoginUserViewModel
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.loadInto
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.launch

class ExtensionSettingFragment : Fragment() {

  companion object {
    fun newInstance(
      clientId: String, clientName: String
    ) = ExtensionSettingFragment().apply {
      arguments = Bundle().apply {
        putString("clientId", clientId)
      }
    }

    fun newInstance(extension: Extension<*>) =
      newInstance(extension.id, extension.name)
  }

  private var binding by autoCleared<FragmentExtensionBinding>()
  private val viewModel by activityViewModels<ExtensionViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val extension by lazy { viewModel.extensionListFlow.getExtension(clientId) }
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
      binding.appBarLayout.setPadding(0, it.top, 0, 0)
      binding.nestedScrollView.setPadding(0, it.top, 0, it.bottom)
    }

    if (context?.isLayout(TV or EMULATOR) == true) {
      binding.toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
      }
    }


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

    binding.toolbar.apply {
      title = extensionMetadata.name
      setNavigationIcon(R.drawable.ic_back)
      setNavigationOnClickListener {
        activity?.onBackPressedDispatcher?.onBackPressed()
      }
      setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menu_like -> {
            val listItems = extensionMetadata.types.map { it.name }
            SelectionDialog.single(
              listItems,
              -1,
              getString(R.string.select_type_you_vote),
              false,
              {}) { itemId ->
              viewModel.vote(extensionMetadata, extensionMetadata.types.get(itemId))
            }.show(parentFragmentManager, null)
            true
          }
          else -> false
        }
      }
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

//    when (extensionMetadata.loginType) {
//      LoginType.API_KEY -> binding.extensionApiKey.root.visibility = View.VISIBLE
//      LoginType.USERNAME_PASSWORD -> binding.extensionLoginUser.root.visibility = View.VISIBLE
//      LoginType.WEBAUTH -> binding.extensionWebLogin.root.visibility = View.VISIBLE
//      LoginType.NONE -> {
//        //nothing to show
//      }
//    }

    if (extension?.isClient<LoginClient>() == true) {
      val loginViewModel by activityViewModels<LoginUserViewModel>()
      loginViewModel.currentExtension.value = extension
      binding.extensionLoginUser.bind(this@ExtensionSettingFragment) {}
    } else binding.extensionLoginUser.root.isVisible = false

    childFragmentManager.beginTransaction()
      .add(R.id.settingsFragment, creator())
      .commit()
  }


  val creator = { ExtensionPreference.newInstance(clientId) }

  class ExtensionPreference : PreferenceFragmentCompat() {
    private val args by lazy { requireArguments() }
    private val extensionId by lazy { args.getString("extensionId")!! }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      preferenceManager.sharedPreferencesName = extensionId
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val screen = preferenceManager.createPreferenceScreen(context)
      preferenceScreen = screen

      val viewModel by activityViewModels<ExtensionViewModel>()
      viewModel.apply {
        val client = viewModel.extensionListFlow.getExtension(extensionId)

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

    private fun onSettingChanged() =
      Preference.OnPreferenceChangeListener { pref, new ->
        val viewModel by activityViewModels<ExtensionViewModel>()
        val client = viewModel.extensionListFlow.getExtension(extensionId)
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
