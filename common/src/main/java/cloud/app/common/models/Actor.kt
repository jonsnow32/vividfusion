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
)

@Serializable
data class ActorData(
  val actor: Actor,
  val role: ActorRole? = null,
  val roleString: String? = null,
  val voiceActor: Actor? = null,
)
