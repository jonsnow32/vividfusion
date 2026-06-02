package cloud.app.vvf.datastore.app.helper

import kotlinx.serialization.Serializable

@Serializable
data class UriHistoryItem(
  val uri: String,
  val title: String? = null,
  val lastUpdated: Long = System.currentTimeMillis()
) {
  val id = uri
    .trim()
    .lowercase()
    .replace("[^a-z0-9\\s]".toRegex(), "")
    .replace("\\s+".toRegex(), "-")
}
