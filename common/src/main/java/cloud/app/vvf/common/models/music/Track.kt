package cloud.app.vvf.common.models.music


import cloud.app.vvf.common.models.ImageHolder
import cloud.app.vvf.common.models.stream.Streamable
import kotlinx.serialization.Serializable

@Serializable
data class Track(
  override var uri: String,
  val id: String,
  val title: String,
  val artists: List<Artist> = listOf(),
  val album: String? = null,
  val cover: String? = null,
  val duration: Long? = null,
  val plays: Long? = null,
  val releaseDate: Long? = null,
  val description: String? = null,
) : Streamable
