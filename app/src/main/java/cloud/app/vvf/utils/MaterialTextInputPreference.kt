package cloud.app.vvf.utils

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import cloud.app.vvf.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialTextInputPreference(context: Context) : EditTextPreference(context) {

    override fun onClick() {
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(R.layout.dialog_edit_text)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setTitle(title)
            .create()

        dialog.setOnShowListener {
            val editText = dialog.findViewById<EditText>(R.id.edit_text)
            editText?.setText(text)
            editText?.hint = summary

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newText = editText?.text.toString()
                if (callChangeListener(newText)) {
                    text = newText
                    dialog.dismiss()
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
