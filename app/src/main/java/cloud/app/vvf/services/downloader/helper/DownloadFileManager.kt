package cloud.app.vvf.services.downloader.helper

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import cloud.app.vvf.R
import cloud.app.vvf.utils.KUniFile
import timber.log.Timber
import java.io.IOException
import androidx.core.content.edit

class DownloadFileManager(private val context: Context) {


  fun saveDownloadDirectoryPreference(uri: Uri) {
    val sharedPreferences =
      context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    sharedPreferences.edit {
      putString(
        context.getString(R.string.pref_download_path),
        uri.toString()
      )
    }
  }

  fun getDownloadUri(): KUniFile {
    val sharedPreferences =
      context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    val uri = sharedPreferences.getString(context.getString(R.string.pref_download_path), null)
    return if (uri != null) {
      KUniFile.fromUri(context, Uri.parse(uri))
        ?: throw IOException("Failed to create KUniFile from saved URI")
    } else {
      return try {
        // For Android 10+, use app-specific directory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val appSpecificDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
          if (appSpecificDownloads != null) {
            return KUniFile.Companion.fromFile(context, appSpecificDownloads)
              ?: throw IOException("Failed to create KUniFile from app-specific directory")
          }
        }

        // Fallback to internal directory
        val internalDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
          ?: context.filesDir

        KUniFile.Companion.fromFile(context, internalDownloads)
          ?: throw IOException("Failed to create KUniFile from fallback directory")

      } catch (e: Exception) {
        Timber.e(e, "Error accessing downloads directory, using internal storage")
        val internalDir = context.filesDir
        KUniFile.Companion.fromFile(context, internalDir)
          ?: throw IOException("Cannot access any storage directory")
      }
    }
  }




  fun createOrGetFile(
    fileName: String,
    contentType: String?,
    isResuming: Boolean = false,
    resumeBytes: Long = 0L
  ): Pair<KUniFile, Boolean> {
    val downloadsDir = getDownloadUri()
    val fullFileName = ensureFileExtension(fileName, contentType)

    return if (isResuming && resumeBytes > 0) {
      handleResumeFile(downloadsDir, fullFileName, resumeBytes)
    } else {
      val newFile = contentType?.let { downloadsDir.createFile(fullFileName, it) }
        ?: throw IOException("Failed to create media file")
      Pair(newFile, false)
    }
  }

  private fun handleResumeFile(
    downloadsDir: KUniFile,
    fileName: String,
    resumeBytes: Long
  ): Pair<KUniFile, Boolean> {
    val existingFile = downloadsDir.findFile(fileName)

    return if (existingFile != null) {
      val existingSize = existingFile.length()
      when {
        existingSize >= resumeBytes -> {
          if (existingSize > resumeBytes) {
            truncateFile(existingFile, resumeBytes)
          }
          Pair(existingFile, true)
        }

        else -> {
          existingFile.delete()
          val newFile = downloadsDir.createFile(fileName, "application/octet-stream")
            ?: throw IOException("Failed to create new file")
          Pair(newFile, false)
        }
      }
    } else {
      val newFile = downloadsDir.createFile(fileName, "application/octet-stream")
        ?: throw IOException("Failed to create new file")
      Pair(newFile, false)
    }
  }

  private fun ensureFileExtension(fileName: String, contentType: String?): String {
    if (fileName.contains(".")) return fileName

    val extension = when (contentType) {
      "video/mp4" -> ".mp4"
      "video/webm" -> ".webm"
      "video/x-matroska" -> ".mkv"
      "audio/mpeg" -> ".mp3"
      "audio/mp4" -> ".m4a"
      "audio/webm" -> ".webm"
      else -> ".mp4"
    }

    return "$fileName$extension"
  }

  private fun truncateFile(file: KUniFile, targetSize: Long) {
    try {
      val tempBuffer = ByteArray(targetSize.toInt())
      var bytesRead = 0

      file.openInputStream().use { input ->
        bytesRead = input.read(tempBuffer, 0, targetSize.toInt())
      }

      if (bytesRead > 0) {
        file.openOutputStream().use { output ->
          output.write(tempBuffer, 0, bytesRead)
          output.flush()
        }
      }
    } catch (e: Exception) {
      Timber.e(e, "Failed to truncate file")
      throw e
    }
  }
}
