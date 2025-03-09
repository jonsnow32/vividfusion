package cloud.app.vvf.ui.widget.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cloud.app.vvf.databinding.DialogBottomInputBinding
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputDialog : DockingDialog() {
  var binding by autoCleared<DialogBottomInputBinding>()
  private val args by lazy { requireArguments() }
  val inputText: String? by lazy { args.getString("inputText", null) }
  val title: String? by lazy { args.getString("title", null) }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogBottomInputBinding.inflate(inflater, container, false)
    return binding.root
  }

  companion object {
    fun newInstance(title: String, textInput: String, callback: (String) -> Unit): InputDialog {
      val args = Bundle()
      args.putString("inputText", textInput)
      args.putString("title", title)
      val fragment = InputDialog()
      fragment.callback = callback
      fragment.arguments = args
      return fragment
    }
  }

  private var callback: ((String) -> Unit)? = null
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    binding.apply {

      text1.text = title
      nginxTextInput.setText(inputText, TextView.BufferType.EDITABLE)
      applyBtt.setOnClickListener {
        callback?.invoke(nginxTextInput.text.toString())  // try to save the setting, using callback
        dialog.dismissSafe(activity)
      }

      cancelBtt.setOnClickListener {  // just dismiss
        dialog.dismissSafe(activity)
      }

      dialog?.setOnDismissListener {

      }
    }
    super.onViewCreated(view, savedInstanceState)
  }
}
