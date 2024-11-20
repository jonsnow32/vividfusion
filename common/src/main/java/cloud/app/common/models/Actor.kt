package cloud.app.common.models


import kotlinx.serialization.Serializable

enum class ActorRole {
  Main,
  Supporting,
  Background,
}

@Serializable
data class Actor(
  val name: String,
  val image: ImageHolder? = null,
  val id: Int? = null,
  val role: String? = null
)
