package cloud.app.vvf.utils

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * Utility object for handling file-related operations, including folder picking and URI management.
 */
object FileFolderPicker {

  /**
   * Creates a file picker launcher for a Fragment with specified file types.
   * @param mimeTypes Array of MIME types to filter files (e.g., "text/plain", "application/json")
   * @param onFileSelected Callback invoked with the selected Uri (null if none selected).
   * @return ActivityResultLauncher for launching the file picker.
   */
  fun Fragment.getChooseFileLauncher(
    onFileSelected: (Uri?) -> Unit
  ) = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri == null) {
      onFileSelected(null)
      return@registerForActivityResult
    }
    val context = context ?: return@registerForActivityResult

    // Persist read permission for the URI
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    try {
      context.contentResolver.takePersistableUriPermission(uri, flags)
      onFileSelected(uri)
    } catch (e: SecurityException) {
      onFileSelected(null) // Permission failed
    }
  }

  /**
   * Creates a folder picker launcher for a Fragment.
   * @param onFolderSelected Callback invoked with the selected Uri (null if none selected).
   * @return ActivityResultLauncher for launching the folder picker.
   */
  fun Fragment.getChooseFolderLauncher(onFolderSelected: (Uri?) -> Unit) =
    registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri == null) {
        onFolderSelected(null)
        return@registerForActivityResult
      }
      val context = context ?: return@registerForActivityResult

      // Persist read/write permissions for the URI
      val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      try {
        context.contentResolver.takePersistableUriPermission(uri, flags)
        onFolderSelected(uri)
      } catch (e: SecurityException) {
        onFolderSelected(null) // Permission failed
      }
    }

}
