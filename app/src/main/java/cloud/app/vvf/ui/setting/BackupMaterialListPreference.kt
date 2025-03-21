package cloud.app.vvf.ui.setting

import android.content.Context
import android.net.Uri
import androidx.preference.ListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cloud.app.vvf.R
import cloud.app.vvf.utils.FileFolderPicker
import cloud.app.vvf.utils.KUniFile

class BackupMaterialListPreference(context: Context, val newFolder: () -> Unit) :
  ListPreference(context) {

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
      .setNegativeButton(R.string.cancel) { dialog, _ ->
        dialog.dismiss()
      }
      .setNeutralButton(R.string.new_folder) { dialog, _ ->
        newFolder.invoke()
        // Note: Dialog won't dismiss here; it waits for pathPicker callback to update UI
      }
      .setTitle(title)
      .create()
      .show()
  }

  override fun getSummary(): CharSequence {
    // Avoid default formatting; use the current value directly
    val currentValue = value ?: return "Not set"
    val uri = KUniFile.fromUri(context, Uri.parse(currentValue))
    return uri?.filePath ?: "Not set"
  }

  private fun updateSummary() {
    summary = getSummary() // Trigger summary update
  }
}
