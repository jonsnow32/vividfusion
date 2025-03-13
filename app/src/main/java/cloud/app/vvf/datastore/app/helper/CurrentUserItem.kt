package cloud.app.vvf.datastore.app.helper

import kotlinx.serialization.Serializable

@Serializable
data class CurrentUserItem(
  val extensionId: String,
  val id: String?
)

