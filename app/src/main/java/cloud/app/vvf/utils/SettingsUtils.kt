package cloud.app.vvf.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.datastore.account.Account

fun getSettings(context: Context, metadata: ExtensionMetadata): PrefSettings {
  val name = "${context.packageName}${metadata.className}"
  val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
  return toSettings(prefs)
}

fun toSettings(prefs: SharedPreferences) = object : PrefSettings {
  override fun getString(key: String) = prefs.getString(key, null)
  override fun putString(key: String, value: String?) {
    prefs.edit { putString(key, value) }
  }

  override fun getInt(key: String) =
    if (prefs.contains(key)) prefs.getInt(key, 0) else null

  override fun putInt(key: String, value: Int?) {
    prefs.edit { putInt(key, value) }
  }

  override fun getBoolean(key: String) =
    if (prefs.contains(key)) prefs.getBoolean(key, false) else null

  override fun putBoolean(key: String, value: Boolean?) {
    prefs.edit { putBoolean(key, value) }
  }

  override fun getLong(key: String) =
    if (prefs.contains(key)) prefs.getLong(key, 0) else null

  override fun putLong(key: String, value: Long?) {
    prefs.edit { putLong(key, value) }
  }

  override fun getStringSet(key: String) = prefs.getStringSet(key, null)
  override fun putStringSet(key: String, value: Set<String>?) {
    prefs.edit { putStringSet(key, value) }
  }
}
