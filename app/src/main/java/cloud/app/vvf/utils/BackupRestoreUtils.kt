package cloud.app.vvf.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import cloud.app.vvf.R
import cloud.app.vvf.utils.KUniFile.Companion.DocumentKUniFile
import cloud.app.vvf.utils.KUniFile.Companion.FileKUniFile
import cloud.app.vvf.utils.KUniFile.Companion.MediaCollection
import cloud.app.vvf.utils.KUniFile.Companion.MediaKUniFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import androidx.core.content.edit
import androidx.core.net.toUri

// Data class to represent a single SharedPreferences entry
@Serializable
data class PrefEntry(
  val value: String,
  val type: String // To store type information
)

// Data class to hold all SharedPreferences data
@Serializable
data class SharedPrefsBackup(
  val preferences: Map<String, Map<String, PrefEntry>>
)

class FileHelper(val context: Context) {

  private val json = Json { prettyPrint = true } // Configure JSON serialization

  /**
   * Backs up multiple SharedPreferences to a JSON file using KUniFile.
   * @param prefNames List of SharedPreferences names to back up.
   * @param backupFile KUniFile representing the destination file.
   * @return True if backup succeeds, false otherwise.
   */
  fun backupSharedPreferencesToJson(prefNames: List<String>, backupFile: KUniFile): Boolean {
    return try {
      val backupData = mutableMapOf<String, Map<String, PrefEntry>>()

      prefNames.forEach { prefName ->
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val allEntries = prefs.all
        val prefMap = mutableMapOf<String, PrefEntry>()

        allEntries.forEach { (key, value) ->
          when (value) {
            is Boolean -> prefMap[key] = PrefEntry(value.toString(), "Boolean")
            is Float -> prefMap[key] = PrefEntry(value.toString(), "Float")
            is Int -> prefMap[key] = PrefEntry(value.toString(), "Int")
            is Long -> prefMap[key] = PrefEntry(value.toString(), "Long")
            is String -> prefMap[key] = PrefEntry(value, "String")
            is Set<*> -> prefMap[key] = PrefEntry(
              json.encodeToString(value as Set<String>),
              "StringSet"
            )
          }
        }
        backupData[prefName] = prefMap
      }

      val backup = SharedPrefsBackup(backupData)
      val jsonString = json.encodeToString(backup)

      // Ensure parent directory exists based on KUniFile type
      ensureParentDirectory(backupFile)

      // Write JSON to the file
      backupFile.openOutputStream().use { outputStream ->
        outputStream.write(jsonString.toByteArray())
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Ensures the parent directory exists for the given KUniFile.
   * @param file The KUniFile to check and prepare the parent directory for.
   */
  private fun ensureParentDirectory(file: KUniFile) {
    when (file) {
      is FileKUniFile -> {
        // For filesystem, use mkdirs() on the parent directory
        file.filePath?.let { path ->
          File(path).parentFile?.mkdirs()
        }
      }

      is DocumentKUniFile -> {
        // For SAF, create parent directories recursively if possible
        file.name?.let { name ->
          val parentUri = file.uri.pathSegments.dropLast(1).joinToString("/")
          val parent = KUniFile.fromUri(context, Uri.parse(parentUri))
          parent?.createDirectory(name)
        }
      }

      is MediaKUniFile -> {
        // For MediaStore, ensure the relativePath includes the parent structure
        // MediaStore automatically handles this when creating files
        // No explicit mkdirs needed; relativePath is sufficient
      }

      else -> {
        // Assets and Resources are read-only, so no action needed
      }
    }
  }

  /**
   * Restores SharedPreferences from a JSON file using KUniFile.
   * @param backupFileUri Uri of the backup file to restore from.
   * @return True if restore succeeds, false otherwise.
   */
  fun restoreSharedPreferencesFromJson(backupFileUri: Uri): Boolean {
    return try {
      val backupFile = KUniFile.fromUri(context, backupFileUri) ?: return false

      // Read and decode JSON from the file
      val jsonString = backupFile.openInputStream().use { inputStream ->
        inputStream.readBytes().toString(Charsets.UTF_8)
      }

      val backup = json.decodeFromString<SharedPrefsBackup>(jsonString)

      backup.preferences.forEach { (prefName, prefData) ->
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Clear existing data
        editor.clear()

        // Restore each key-value pair
        prefData.forEach { (key, entry) ->
          when (entry.type) {
            "Boolean" -> editor.putBoolean(key, entry.value.toBoolean())
            "Float" -> editor.putFloat(key, entry.value.toFloat())
            "Int" -> editor.putInt(key, entry.value.toInt())
            "Long" -> editor.putLong(key, entry.value.toLong())
            "String" -> editor.putString(key, entry.value)
            "StringSet" -> editor.putStringSet(
              key,
              json.decodeFromString<Set<String>>(entry.value)
            )
          }
        }

        editor.apply()
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Gets a list of all SharedPreferences file names in the app's storage.
   * @return List of SharedPreferences names.
   */
  fun getAllSharedPrefsNames(): List<String> {
    val prefNames = mutableListOf<String>()
    val prefsDir = KUniFile.fromFile(context, File("${context.filesDir.parent}/shared_prefs"))
      ?: return emptyList()

    if (prefsDir.exists() && prefsDir.isDirectory) {
      prefsDir.listFiles()?.forEach { file ->
        file.name?.let { fileName ->
          if (fileName.endsWith(".xml")) {
            prefNames.add(fileName.removeSuffix(".xml"))
          }
        }
      }
    }
    return prefNames
  }

  /**
   * Returns the default Downloads folder in shared media storage as a KUniFile.
   * @return KUniFile representing the Downloads directory, or null if unavailable.
   */

  fun getDefaultBackupDir(): KUniFile? {
    return KUniFile.fromMedia(
      context = context,
      collection = MediaCollection.DOWNLOADS,
      relativePath = "", // Root of Downloads
      external = true
    )
  }

  fun storeBackupPath(uri: Uri?): KUniFile? {
    if (uri == null) return null

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val uriString = uri.toString()

    // Store the URI as the primary backup path
    prefs.edit()
      .putString(context.getString(R.string.pref_backup_path), uriString)
      .apply()


    addAllowedPath(uri)

    // Return display path for UI purposes
    return KUniFile.fromUri(context, uri)
  }

  /**
   * Retrieves the set of allowed backup paths from SharedPreferences.
   * @param context Application context.
   * @return MutableSet of allowed path URIs.
   */
  fun getAllowedPaths(default: KUniFile?): List<KUniFile> {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val uris = prefs.getStringSet(context.getString(R.string.pref_paths_allowed), null)
    val list = uris?.mapNotNull { uri ->
      KUniFile.fromUri(context, uri.toUri())
    }?.toMutableList() ?: mutableListOf()
    default?.let { list.add(0, it) }
    return list
  }

  @SuppressLint("MutatingSharedPrefs")
  fun addAllowedPath(uri: Uri) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val allowedPaths =
      prefs.getStringSet(context.getString(R.string.pref_paths_allowed), mutableSetOf())
        ?: mutableSetOf()

    allowedPaths.add(uri.toString())
    prefs.edit {
      putStringSet(context.getString(R.string.pref_paths_allowed), allowedPaths)
    }
  }

}
