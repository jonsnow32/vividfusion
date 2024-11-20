package cloud.app.avp.datastore

import android.content.SharedPreferences
import cloud.app.avp.utils.toData
import cloud.app.avp.utils.toJson
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

const val PREFERENCES_NAME = "data_in_preference"
const val FOLDER_PLUGIN_SETTINGS = "folder_app_settings"
const val HISTORY_FOLDER = "history"

class DataStore @Inject constructor(
  val sharedPreferences: SharedPreferences,
  val mapper: Json
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
    Timber.i(folder)
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

  inline fun <reified T> setKey(path: String, value: T) {
    try {
      sharedPreferences.edit().putString(path, value.toJson()).apply()
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  inline fun <reified T> getKey(path: String, defVal: T? = null): T? {
    return try {
      Timber.i("getKey $path ${T::class.java}" )
      sharedPreferences.getString(path, null)?.toData<T>()
    } catch (e: Exception) {
      Timber.e(e)
      defVal
    }
  }
}
