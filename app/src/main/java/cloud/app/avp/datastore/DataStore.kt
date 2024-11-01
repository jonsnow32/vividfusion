package cloud.app.avp.datastore

import android.content.SharedPreferences
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.movie.Ids
import com.fasterxml.jackson.databind.json.JsonMapper
import timber.log.Timber
import javax.inject.Inject

const val PREFERENCES_NAME = "data_in_preference"
const val FOLDER_APP_SETTINGS = "folder_app_settings"

const val HISTORY_FOLDER = "history"


class DataStore @Inject constructor(
  val sharedPreferences: SharedPreferences,
  val mapper: JsonMapper
) {

  // Editor class for batch edits
  data class Editor(val editor: SharedPreferences.Editor) {
    fun <T> setKeyRaw(path: String, value: T) {
      when (value) {
        is Set<*> -> if (value.all { it is String }) editor.putStringSet(path, value as Set<String>)
        is Boolean -> editor.putBoolean(path, value)
        is Int -> editor.putInt(path, value)
        is String -> editor.putString(path, value)
        is Float -> editor.putFloat(path, value)
        is Long -> editor.putLong(path, value)
      }
    }

    fun apply() {
      editor.apply()
    }
  }

  // Functions for DataStore
  fun editor(): Editor {
    return Editor(sharedPreferences.edit())
  }

  fun getKeys(folder: String): List<String> {
    return sharedPreferences.all.keys.filter { it.startsWith(folder) }
  }

  fun removeKey(folder: String, path: String) {
    removeKey("$folder/$path")
  }

  fun containsKey(path: String): Boolean {
    return sharedPreferences.contains(path)
  }

  fun removeKey(path: String) {
    try {
      sharedPreferences.edit().remove(path).apply()
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun removeKeys(folder: String): Int {
    val keys = getKeys(folder)
    keys.forEach { removeKey(it) }
    return keys.size
  }

  fun <T> setKey(path: String, value: T) {
    try {
      sharedPreferences.edit().putString(path, mapper.writeValueAsString(value)).apply()
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun <T> getKey(path: String, valueType: Class<T>): T? {
    return try {
      sharedPreferences.getString(path, null)?.let { mapper.readValue(it, valueType) }
    } catch (e: Exception) {
      Timber.e(e)
      null
    }
  }

  inline fun <reified T : Any> getKey(path: String, defVal: T? = null): T? {
    return try {
      sharedPreferences.getString(path, null)?.let { mapper.readValue(it, T::class.java) }
    } catch (e: Exception) {
      Timber.e(e)
      defVal
    }
  }
}
