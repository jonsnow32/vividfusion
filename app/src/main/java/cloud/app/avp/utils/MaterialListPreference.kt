package cloud.app.avp.utils

import android.content.Context
import androidx.preference.ListPreference
import cloud.app.avp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialListPreference(context: Context) : ListPreference(context) {

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        updateSummary()
    }

    override fun onClick() {
        MaterialAlertDialogBuilder(context)
            .setSingleChoiceItems(entries, entryValues.indexOf(value)) { dialog, index ->
                if (callChangeListener(entryValues[index].toString())) {
                    setValueIndex(index)
                    updateSummary()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }.setTitle(title)
            .create()
            .show()
    }

    private fun updateSummary() {
        summary = entry
    }
}
