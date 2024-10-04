package cloud.app.avp.ui.extension

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cloud.app.avp.databinding.DialogExtensionsListBinding
import cloud.app.avp.utils.autoCleared
import cloud.app.common.models.ExtensionType

class ExtensionsListBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(type: ExtensionType) = ExtensionsListBottomSheet().apply {
            arguments = Bundle().apply {
                putString("type", type.name)
            }
        }
    }

    private var binding by autoCleared<DialogExtensionsListBinding>()
    private val args by lazy { requireArguments() }
    private val type by lazy { ExtensionType.valueOf(args.getString("type")!!) }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogExtensionsListBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.addExtension.isEnabled = false
//        binding.manageExtensions.setOnClickListener {
//            dismiss()
//            requireActivity().openFragment(ManageExtensionsFragment())
//        }
//        val viewModel = when (type) {
//            ExtensionType.LYRICS -> activityViewModels<LyricsViewModel>().value
//            ExtensionType.MUSIC -> activityViewModels<ExtensionViewModel>().value
//            ExtensionType.TRACKER -> throw IllegalStateException("Tracker not supported")
//        }
//
//        val listener = object : OnButtonCheckedListener {
//            var map: Map<Int, PluginMetadata> = mapOf()
//            var enabled = false
//            override fun onButtonChecked(
//                group: MaterialButtonToggleGroup?,
//                checkedId: Int,
//                isChecked: Boolean
//            ) {
//                if (isChecked && enabled) map[checkedId]?.let {
//                    viewModel.onClientSelected(it.id)
//                    dismiss()
//                }
//            }
//        }
//        binding.buttonToggleGroup.addOnButtonCheckedListener(listener)
//        val extensionFlow = viewModel.metadataFlow
//        collect(extensionFlow) { clientList ->
//            binding.buttonToggleGroup.removeAllViews()
//            binding.progressIndicator.isVisible = clientList == null
//            listener.enabled = false
//            val list = clientList?.filter { it.enabled } ?: emptyList()
//
//            val map = list.mapIndexed { index, metadata ->
//                val button = ButtonExtensionBinding.inflate(
//                    layoutInflater,
//                    binding.buttonToggleGroup,
//                    false
//                ).root
//                button.text = metadata.name
//                binding.buttonToggleGroup.addView(button)
//                button.isChecked = metadata.id == viewModel.currentFlow.value
//                metadata.iconUrl?.toImageHolder().loadWith(button, R.drawable.ic_extension) {
//                    button.icon = it
//                }
//                button.id = index
//                index to metadata
//            }.toMap()
//
//            val checked = map.filter { it.value.id == viewModel.currentFlow.value }.keys
//                .firstOrNull()
//
//            listener.map = map
//            if (checked != null) binding.buttonToggleGroup.check(checked)
//            listener.enabled = true
//
//        }
    }

}
