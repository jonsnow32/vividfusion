package cloud.app.vvf.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import cloud.app.vvf.R
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.databinding.ButtonExtensionBinding
import cloud.app.vvf.databinding.DialogLoginUserListBinding
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.loadWith
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe
import com.google.android.material.button.MaterialButtonToggleGroup

class LoginUserListBottomSheet : DockingDialog() {

    var binding by autoCleared<DialogLoginUserListBinding>()
    val viewModel by activityViewModels<LoginUserViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogLoginUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.accountListLogin.isEnabled = false

        observe(viewModel.allUsers) { (metadata, list) ->
            binding.accountListLoading.root.isVisible = list == null
            binding.accountListToggleGroup.isVisible = list != null
            metadata ?: return@observe
            list ?: return@observe
            binding.addAccount.setOnClickListener {
                dismiss()
                requireActivity().navigate(
                    LoginFragment.newInstance(
                        metadata.className, metadata.name, ExtensionType.DATABASE.name
                    )
                )
            }
            binding.accountListToggleGroup.removeAllViews()
            val listener = MaterialButtonToggleGroup.OnButtonCheckedListener { _, id, isChecked ->
                if (isChecked) {
                    val user = list[id]
                    binding.accountListLogin.isEnabled = true
                    binding.accountListLogin.setOnClickListener {
                        viewModel.setLoginUser(user)
                        dismiss()
                    }
                }
            }
            binding.accountListToggleGroup.addOnButtonCheckedListener(listener)
            list.forEachIndexed { index, user ->
                val button = ButtonExtensionBinding.inflate(
                    layoutInflater, binding.accountListToggleGroup, false
                ).root
                button.text = user.name
                binding.accountListToggleGroup.addView(button)
                user.cover.loadWith(button, R.drawable.ic_person) { button.icon = it }
                button.id = index
            }
        }
    }

}
