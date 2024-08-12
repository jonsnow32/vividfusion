package cloud.app.avp.utils


import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import cloud.app.common.settings.Settings
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import timber.log.Timber
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


const val PREFERENCES_NAME = "rebuild_preference"

/** When inserting many keys use this function, this is because apply for every key is very expensive on memory */
data class Editor(
  val editor : SharedPreferences.Editor
) {
  /** Always remember to call apply after */
  fun<T> setKeyRaw(path: String, value: T) {
    @Suppress("UNCHECKED_CAST")
    if (isStringSet(value)) {
      editor.putStringSet(path, value as Set<String>)
    } else {
      when (value) {
        is Boolean -> editor.putBoolean(path, value)
        is Int -> editor.putInt(path, value)
        is String -> editor.putString(path, value)
        is Float -> editor.putFloat(path, value)
        is Long -> editor.putLong(path, value)
      }
    }
  }

  private fun isStringSet(value: Any?) : Boolean {
    if (value is Set<*>) {
      return value.filterIsInstance<String>().size == value.size
    }
    return false
  }

  fun apply() {
    editor.apply()
    System.gc()
  }
}


fun toSettings(prefs: SharedPreferences) = object : Settings {
  override fun getString(key: String) = prefs.getString(key, null)
  override fun putString(key: String, value: String?) {
    prefs.edit { putString(key, value) }
  }

  override fun getInt(key: String) = if (prefs.contains(key)) prefs.getInt(key, 0)
  else null

  override fun putInt(key: String, value: Int?) {
    prefs.edit { putInt(key, value) }
  }

  override fun getBoolean(key: String) = if (prefs.contains(key)) prefs.getBoolean(key, false)
  else null

  override fun putBoolean(key: String, value: Boolean?) {
    prefs.edit { putBoolean(key, value) }
  }
}

object DataStore {

  val mapper: JsonMapper = JsonMapper.builder().addModule(kotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()

  private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  }

  fun Context.getSharedPrefs(): SharedPreferences {
    return getPreferences(this)
  }

  fun getFolderName(folder: String, path: String): String {
    return "${folder}/${path}"
  }

  fun editor(context : Context, isEditingAppSettings: Boolean = false) : Editor {
    val editor: SharedPreferences.Editor =
      if (isEditingAppSettings) context.getDefaultSharedPrefs().edit() else context.getSharedPrefs().edit()
    return Editor(editor)
  }

  fun Context.getDefaultSharedPrefs(): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(this)
  }

  fun Context.getKeys(folder: String): List<String> {
    return this.getSharedPrefs().all.keys.filter { it.startsWith(folder) }
  }

  fun Context.removeKey(folder: String, path: String) {
    removeKey(getFolderName(folder, path))
  }

  fun Context.containsKey(folder: String, path: String): Boolean {
    return containsKey(getFolderName(folder, path))
  }

  fun Context.containsKey(path: String): Boolean {
    val prefs = getSharedPrefs()
    return prefs.contains(path)
  }

  fun Context.removeKey(path: String) {
    try {
      val prefs = getSharedPrefs()
      if (prefs.contains(path)) {
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.remove(path)
        editor.apply()
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun Context.removeKeys(folder: String): Int {
    val keys = getKeys("$folder/")
    keys.forEach { value ->
      removeKey(value)
    }
    return keys.size
  }

  fun <T> Context.setKey(path: String, value: T) {
    try {
      val editor: SharedPreferences.Editor = getSharedPrefs().edit()
      editor.putString(path, mapper.writeValueAsString(value))
      editor.apply()
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun <T> Context.getKey(path: String, valueType: Class<T>): T? {
    try {
      val json: String = getSharedPrefs().getString(path, null) ?: return null
      return json.toKotlinObject(valueType)
    } catch (e: Exception) {
      return null
    }
  }

  fun <T> Context.setKey(folder: String, path: String, value: T) {
    setKey(getFolderName(folder, path), value)
  }

  inline fun <reified T : Any> String.toKotlinObject(): T {
    return mapper.readValue(this, T::class.java)
  }

  fun <T> String.toKotlinObject(valueType: Class<T>): T {
    return mapper.readValue(this, valueType)
  }

  // GET KEY GIVEN PATH AND DEFAULT VALUE, NULL IF ERROR
  inline fun <reified T : Any> Context.getKey(path: String, defVal: T?): T? {
    try {
      val json: String = getSharedPrefs().getString(path, null) ?: return defVal
      return json.toKotlinObject()
    } catch (e: Exception) {
      return null
    }
  }

  inline fun <reified T : Any> Context.getKey(path: String): T? {
    return getKey(path, null)
  }

  inline fun <reified T : Any> Context.getKey(folder: String, path: String): T? {
    return getKey(getFolderName(folder, path), null)
  }

  inline fun <reified T : Any> Context.getKey(folder: String, path: String, defVal: T?): T? {
    return getKey(getFolderName(folder, path), defVal) ?: defVal
  }
}
