package cloud.app.vvf.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.vvf.R
import cloud.app.vvf.databinding.DialogLoginUserBinding
import cloud.app.vvf.databinding.ItemLoginUserBinding
import cloud.app.vvf.datastore.app.helper.CurrentUserItem
import cloud.app.vvf.ui.setting.SettingsRootFragment
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.loadInto
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.observe

class LoginUserBottomSheet : DockingDialog() {

    var binding by autoCleared<DialogLoginUserBinding>()
    val viewModel by activityViewModels<LoginUserViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogLoginUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.settings.setOnClickListener {
            dismiss()
            requireActivity().navigate(SettingsRootFragment())
        }
        binding.downloads.setOnClickListener {
            dismiss()
            //requireActivity().navigate(DownloadingFragment())
        }
//        val extension = viewModel.extensionFlow.value ?: return
//        viewModel.currentExtension.value =  extension
//        binding.userContainer.bind(this, ::dismiss)
    }

    companion object {

        fun ItemLoginUserBinding.bind(fragment: Fragment, dismiss: () -> Unit) = with(fragment) {
            val viewModel by activityViewModels<LoginUserViewModel>()
            val binding = this@bind

            binding.switchAccount.setOnClickListener {
                dismiss()
                LoginUserListBottomSheet().show(parentFragmentManager)
            }
            observe(viewModel.currentUser) { (extensionData, user) ->
                binding.login.isVisible = user == null
                binding.notLoggedInContainer.isVisible = user == null

                binding.logout.isVisible = user != null
                binding.userContainer.isVisible = user != null

                val metadata = extensionData?.metadata
                binding.login.setOnClickListener {
                    metadata?.run {
                        requireActivity().navigate(
                            LoginFragment.newInstance(this.className, name, extensionData.metadata.types[0].name)
                        )
                    }
                    dismiss()
                }

                binding.logout.setOnClickListener {
                    val id = metadata?.className ?: return@setOnClickListener
                    viewModel.logout(id, user?.id)
                    viewModel.setLoginUser(CurrentUserItem(id, null))
                }


                binding.currentUserName.text = user?.name
                binding.currentUserSubTitle.text = user?.name ?: metadata?.name
                user?.cover.loadInto(binding.currentUserAvatar, R.drawable.ic_person)
            }
        }
    }
}
