package cloud.app.vvf.common.models


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

enum class ActorRole {
  Main,
  Supporting,
  Background,
}

@Serializable
data class Actor(
  @SerialName("name") val name: String,
  @SerialName("image") val image: ImageHolder? = null,
  @SerialName("id") val id: Int? = null,
  @SerialName("role") val role: String? = null
)
