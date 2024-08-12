package cloud.app.avp.ui.extension

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.avp.AVPApplication.Companion.noClient
import cloud.app.avp.MainActivityViewModel.Companion.applyContentInsets
import cloud.app.avp.MainActivityViewModel.Companion.applyInsets
import cloud.app.avp.R
import cloud.app.avp.databinding.FragmentExtensionBinding
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.loadWith
import cloud.app.avp.utils.onAppBarChangeListener
import cloud.app.avp.utils.setupTransition
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.createSnack
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.plugger.PluginMetadata

class ExtensionInfoFragment : Fragment() {

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
      binding.iconContainer.updatePadding(top = it.top)
      binding.nestedScrollView.applyContentInsets(it)
    }
    binding.appBarLayout.onAppBarChangeListener { offset ->
      binding.toolbarOutline.alpha = offset
    }
    binding.toolBar.setNavigationOnClickListener {
      parentFragmentManager.popBackStack()
    }
    binding.toolBar.title = extensionMetadata.name
    extensionMetadata.icon.toImageHolder()
      .loadWith(binding.extensionIcon, R.drawable.ic_extension_24dp) {
        binding.extensionIcon.setImageDrawable(it)
      }
//        val pair = when (extensionType) {
//            ExtensionType.DATABASE -> {
//                val extension = viewModel.extensionListFlow.getExtension(clientId)
//                if (extension == null) null else extension.metadata to extension.client
//            }
//
//            ExtensionType.STREAM -> {
//                val extension = viewModel.trackerListFlow.getExtension(clientId)
//                if (extension == null) null else extension.metadata to extension.client
//            }
//
//            ExtensionType.SUBTITLE -> {
//                val extension = viewModel.lyricsListFlow.getExtension(clientId)
//                if (extension == null) null else extension.metadata to extension.client
//            }
//        }
//
//        if (pair == null) {
//            createSnack(requireContext().noClient())
//            parentFragmentManager.popBackStack()
//            return
//        }
//
//        val (metadata, client) = pair
//

//        binding.extensionDetails.text =
//            "${metadata.version} • ${metadata.importType.name}"
//
//        val byAuthor = getString(R.string.by_author, metadata.author)
//        val type = when (extensionType) {
//            ExtensionType.MUSIC -> R.string.music
//            ExtensionType.TRACKER -> R.string.tracker
//            ExtensionType.LYRICS -> R.string.lyrics
//        }
//        val typeString = getString(R.string.name_extension, getString(type))
//        binding.extensionDescription.text = "$typeString\n\n${metadata.description}\n\n$byAuthor"
//
//        fun updateText(enabled: Boolean) {
//            binding.enabledText.text = getString(
//                if (enabled) R.string.enabled else R.string.disabled
//            )
//        }
//        binding.enabledSwitch.apply {
//            updateText(metadata.enabled)
//            isChecked = metadata.enabled
//            setOnCheckedChangeListener { _, isChecked ->
//                updateText(isChecked)
//                viewModel.setExtensionEnabled(extensionType, metadata.id, isChecked)
//            }
//            binding.enabledCont.setOnClickListener { toggle() }
//        }
//
//        if (client is LoginClient) {
//            val loginViewModel by activityViewModels<LoginUserViewModel>()
//            loginViewModel.currentExtension.value = LoginUserViewModel.ExtensionData(
//                extensionType, metadata, client
//            )
//            binding.extensionLoginUser.bind(this) {}
//        } else binding.extensionLoginUser.root.isVisible = false
//
//        binding.extensionSettings.transitionName = "setting_${metadata.id}"
//        binding.extensionSettings.setOnClickListener {
//            openFragment(ExtensionFragment.newInstance(metadata, extensionType), it)
//        }
  }

}
