package cloud.app.vvf.ui.widget.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cloud.app.vvf.databinding.DialogConfirmExitBinding
import cloud.app.vvf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddExtensionDialog : DockingDialog(){
  var binding by autoCleared<DialogConfirmExitBinding>()
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogConfirmExitBinding.inflate(inflater, container, false)
    return binding.root
  }

}
