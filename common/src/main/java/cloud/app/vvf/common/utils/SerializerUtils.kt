package cloud.app.vvf.common.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json {
  ignoreUnknownKeys = true
}

inline fun <reified T> String.toData() = json.decodeFromString<T>(this)
inline fun <reified T> T.toJson() = json.encodeToString(this)
