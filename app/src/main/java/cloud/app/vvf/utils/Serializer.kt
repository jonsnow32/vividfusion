package cloud.app.vvf.utils

import android.os.Bundle
import cloud.app.vvf.common.utils.json
import cloud.app.vvf.common.utils.toData
import cloud.app.vvf.common.utils.toJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


inline fun <reified T> Bundle.putSerialized(key: String, value: T) {
  putString(key, value.toJson())
}

inline fun <reified T> Bundle.getSerialized(key: String): T? {
  return getString(key)?.toData()
}

inline fun <reified T> String.safeToData(): T? {
  return try {
    json.decodeFromString<T>(this)
  } catch (ex: Exception) {
    null
  }
}

//inline fun <reified T : Serializable> Bundle.getSerial(key: String?) =
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//        getSerializable(key, T::class.java)
//    else getSerializable(key) as T
