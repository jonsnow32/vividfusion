package cloud.app.vvf.extension.tmdb.services.tmdb.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ImageResult(
  @SerialName("backdrops")
    val backdrops: List<Backdrop>,
  @SerialName("id")
    val id: Int,
  @SerialName("logos")
    val logos: List<Logo>,
  @SerialName("posters")
    val posters: List<Poster>,
)
