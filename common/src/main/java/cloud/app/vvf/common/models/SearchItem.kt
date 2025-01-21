package cloud.app.vvf.common.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchItem(
  @SerialName("query")
  val query: String,
  @SerialName("searched")
  val searched: Boolean = false,
  @SerialName("searchedAt")
  val searchedAt: Long,
) {
  fun sameAs(other: SearchItem) = query == other.query
  val id
    get() = query
    .trim()
    .lowercase()
    .replace("[^a-z0-9\\s]".toRegex(), "") // Remove special characters
    .replace("\\s+".toRegex(), "-")
}
