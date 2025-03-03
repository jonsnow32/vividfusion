package cloud.app.vvf.ui.widget.dialog

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import cloud.app.vvf.R
import cloud.app.vvf.databinding.DialogConfirmExitBinding
import cloud.app.vvf.utils.autoCleared
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExitConfirmDialog: DialogFragment(){
  @Inject
  lateinit var sharedPreferences: SharedPreferences
  var binding by autoCleared<DialogConfirmExitBinding>()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    // Create the dialog with the specified theme
    return Dialog(requireContext(), com.google.android.material.R.style.Theme_Material3_DayNight_Dialog)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogConfirmExitBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.yesBtn.setOnClickListener {
      System.exit(0)
    }
    binding.noBtn.setOnClickListener {
      dismiss()
    }
    binding.checkboxDontShowAgain.setOnCheckedChangeListener { buttonView, isChecked ->
      sharedPreferences.edit().putBoolean(getString(R.string.pref_show_exit_confirm), !isChecked).apply()
    }
    binding.noBtn.requestFocus()
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.setLayout(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }
}
