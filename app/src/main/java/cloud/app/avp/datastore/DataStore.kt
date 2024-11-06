package cloud.app.avp.datastore

import android.content.SharedPreferences
import cloud.app.avp.utils.toData
import cloud.app.avp.utils.toJson
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

const val PREFERENCES_NAME = "data_in_preference"
const val FOLDER_APP_SETTINGS = "folder_app_settings"
const val HISTORY_FOLDER = "history"

class DataStore @Inject constructor(
  val sharedPreferences: SharedPreferences,
  val mapper: Json
) {

  val accountId: String = "default"

  // Editor class for batch edits
  data class Editor(val editor: SharedPreferences.Editor, val accountId: String) {
    fun <T> setKeyRaw(path: String, value: T) {
      when (value) {
        is Set<*> -> if (value.all { it is String }) editor.putStringSet("$accountId/$path", value as Set<String>)
        is Boolean -> editor.putBoolean("$accountId/$path", value)
        is Int -> editor.putInt("$accountId/$path", value)
        is String -> editor.putString("$accountId/$path", value)
        is Float -> editor.putFloat("$accountId/$path", value)
        is Long -> editor.putLong("$accountId/$path", value)
      }
    }

    fun apply() {
      editor.apply()
    }
  }

  // Functions for DataStore
  fun editor(): Editor {
    return Editor(sharedPreferences.edit(), accountId)
  }

  fun getKeys(folder: String): List<String> {
    return sharedPreferences.all.keys.filter { it.startsWith("$accountId/$folder") }
  }

  fun removeKey(folder: String, path: String) {
    removeKey("$accountId/$folder/$path")
  }

  fun containsKey(path: String): Boolean {
    return sharedPreferences.contains("$accountId/$path")
  }

  fun removeKey(path: String) {
    try {
      sharedPreferences.edit().remove("$accountId/$path").apply()
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun removeKeys(folder: String): Int {
    val keys = getKeys("$accountId/$folder")
    keys.forEach { removeKey(it) }
    return keys.size
  }

  inline fun <reified T> setKey(path: String, value: T) {
    try {
      sharedPreferences.edit().putString("$accountId/$path", value.toJson()).apply()
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  inline fun <reified T> getKey(path: String, defVal: T? = null): T? {
    return try {
      sharedPreferences.getString("$accountId/$path", null)?.toData()
    } catch (e: Exception) {
      Timber.e(e)
      defVal
    }
  }
}
