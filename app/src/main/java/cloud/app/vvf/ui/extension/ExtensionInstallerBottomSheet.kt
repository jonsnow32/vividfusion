package cloud.app.vvf.ui.extension

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.ExtensionOpenerActivity.Companion.EXTENSION_INSTALLER
import cloud.app.vvf.R
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.databinding.DialogExtensionInstallerBinding
import cloud.app.vvf.extension.getID
import cloud.app.vvf.extension.plugger.ApkManifestParser
import cloud.app.vvf.extension.plugger.ApkPluginSource
import cloud.app.vvf.extension.plugger.ApkFileInfo
import cloud.app.vvf.extension.plugger.FileManifestParser
import cloud.app.vvf.utils.ApkLinkParser
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.loadWith
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ExtensionInstallerBottomSheet : BottomSheetDialogFragment() {

  companion object {
    fun newInstance(
      file: String,
    ) = ExtensionInstallerBottomSheet().apply {
      arguments = Bundle().apply {
        putString("file", file)
      }
    }
  }

  private var binding by autoCleared<DialogExtensionInstallerBinding>()
  private val args by lazy { requireArguments() }
  private val file by lazy { args.getString("file")!!.toUri().toFile() }
  private val supportedLinks by lazy { if(file.path.endsWith(".apk")) ApkLinkParser.getSupportedLinks(file) else emptyList() }
  private val pair by lazy {
    runCatching {
      if (file.path.endsWith("apk")) {
        val packageInfo = requireActivity().packageManager
          .getPackageArchiveInfo(file.path, ApkPluginSource.PACKAGE_FLAGS)!!
        val id = getID(packageInfo)
        val metadata = ApkManifestParser(ImportType.App).parseManifest(
          ApkFileInfo(file.path, packageInfo.applicationInfo!!)
        )
        id to metadata
      } else {
        val metadata = FileManifestParser(requireActivity().packageManager).parseManifest(file)
        metadata.className to metadata
      }


    }
  }

  override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
    binding = DialogExtensionInstallerBinding.inflate(inflater, parent, false)
    return binding.root
  }

  private var install = false
  private var installAsApk = false

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    binding.topAppBar.setNavigationOnClickListener { dismiss() }
    val value = pair.getOrElse {
      val viewModel by activityViewModels<ExtensionViewModel>()
      lifecycleScope.launch {
        //viewModel.throwableFlow.emit(ExtensionLoadingException(ExtensionType.DATABASE, it))
      }
      dismiss()
      return
    }
//        if (value == null) {
//            createSnack(R.string.invalid_extension)
//            dismiss()
//            return
//        }
    val (extensionType, metadata) = value

    binding.extensionTitle.text = metadata.name
    metadata.iconUrl?.toImageHolder()
      .loadWith(binding.extensionIcon, R.drawable.ic_extension_24dp) {
        binding.extensionIcon.setImageDrawable(it)
      }
    binding.extensionDetails.text = metadata.version

    val byAuthor = getString(R.string.by_author, metadata.author)
    val types = metadata.types.toString()

    val typeString = getString(R.string.name_extension, types, byAuthor)
    binding.extensionDescription.text = "$typeString\n\n${metadata.description}\n\n$byAuthor"

    val isSupported = supportedLinks.isNotEmpty()
    binding.installationTypeTitle.isVisible = isSupported
    binding.installationTypeGroup.isVisible = isSupported
    binding.installationTypeSummary.isVisible = isSupported
    binding.installationTypeLinks.isVisible = isSupported
    binding.installationTypeWarning.isVisible = false

    installAsApk = isSupported
    if (isSupported) {
      binding.installationTypeLinks.text = supportedLinks.joinToString("\n")
      binding.installationTypeGroup.addOnButtonCheckedListener { group, _, _ ->
        installAsApk = group.checkedButtonId == R.id.appInstall
        binding.installationTypeWarning.isVisible = !installAsApk
      }
    }

    binding.installButton.setOnClickListener {
      install = true
      dismiss()
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    requireActivity().supportFragmentManager.setFragmentResult(
      EXTENSION_INSTALLER,
      Bundle().apply {
        putString("file", file.toUri().toString())
        putBoolean("install", install)
        putBoolean("installAsApk", installAsApk)
      }
    )
  }
}
