package cloud.app.vvf.common.models.user


import cloud.app.vvf.common.models.ImageHolder
import kotlinx.serialization.Serializable

@Serializable
open class User(
  open val id: String,
  open val name: String,
  open val cover: ImageHolder? = null,
  open val extras: Map<String, String> = mapOf()
)
