package cloud.app.vvf.ui.widget.dialog.itemOption

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.extension.builtIn.local.MediaUtils
import timber.log.Timber

class MediaDeleteFragment : Fragment() {
    private var pendingMedia: AVPMediaItem? = null
    private var pendingContext: Context? = null
    private var onDeleteResult: ((Boolean) -> Unit)? = null

    private val deleteLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest> = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        handleDeleteResult(result)
    }

    private fun handleDeleteResult(result: androidx.activity.result.ActivityResult) {
        Timber.d("handleDeleteResult called with resultCode: ${result.resultCode}, pendingMedia: $pendingMedia, pendingContext: $pendingContext")

        val context = pendingContext
        val media = pendingMedia
        if (result.resultCode == Activity.RESULT_OK && context != null && media != null) {
            Timber.d("User granted permission, retrying deletion...")
            // Retry deletion after user consent
            try {
                val success = MediaUtils.deleteMedia(context, media) { intentSender ->
                    Timber.d("Another permission needed during retry, launching dialog again...")
                    // If another permission is needed, launch again
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    deleteLauncher.launch(request)
                }
                if (success) {
                    Timber.d("Media deletion successful after user consent")
                    onDeleteResult?.invoke(true)
                } else {
                    Timber.w("Media deletion failed even after user consent")
                    onDeleteResult?.invoke(false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during retry deletion after user consent")
                onDeleteResult?.invoke(false)
            }
        } else {
            Timber.w("User denied delete permission or context/media is null. Result code: ${result.resultCode}, context: $context, media: $media")
            onDeleteResult?.invoke(false)
        }
        pendingMedia = null
        pendingContext = null
        onDeleteResult = null
    }

    fun deleteMediaWithPermission(
        context: Context,
        media: AVPMediaItem,
        onResult: (Boolean) -> Unit
    ) {
        try {
            val success = MediaUtils.deleteMedia(context, media) { intentSender ->
                // Save state for retry
                pendingMedia = media
                pendingContext = context
                onDeleteResult = onResult
                // Launch system dialog for user consent
                try {
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    deleteLauncher.launch(request)
                    Timber.d("Launched user consent dialog for media deletion")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to launch user consent dialog")
                    onResult(false)
                    pendingMedia = null
                    pendingContext = null
                }
            }
            if (success) {
                // Notify success immediately if no permission was needed
                Timber.d("Media deleted successfully without requiring user consent")
                onResult(true)
            } else if (pendingMedia == null) {
                // No RecoverableSecurityException was triggered, deletion failed for other reasons
                onResult(false)
            }
            // If pendingMedia is not null, it means RecoverableSecurityException was triggered
            // and the user consent dialog will handle the result via deleteLauncher
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during media deletion")
            onResult(false)
        }
    }
}
