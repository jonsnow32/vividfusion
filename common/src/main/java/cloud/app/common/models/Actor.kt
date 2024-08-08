package cloud.app.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ActorRole {
  Main,
  Supporting,
  Background,
}
@Parcelize
data class Actor(
  val name: String,
  val image: ImageHolder? = null,
) : Parcelable

@Parcelize
data class ActorData(
  val actor: Actor,
  val role: ActorRole? = null,
  val roleString: String? = null,
  val voiceActor: Actor? = null,
) : Parcelable
