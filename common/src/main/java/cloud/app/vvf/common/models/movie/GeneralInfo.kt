package cloud.app.vvf.common.models.movie

import cloud.app.vvf.common.models.Actor
import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.common.utils.getYear
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName
@Serializable
data class GeneralInfo(
  @SerialName("title") var title: String,
  @SerialName("original_title") var originalTitle: String,
  @SerialName("overview") var overview: String? = null,
  @SerialName("release_date_ms_utc") var releaseDateMsUTC: Long? = null,
  @SerialName("poster") var poster: String? = null,
  @SerialName("backdrop") var backdrop: String? = null,
  @SerialName("logo") var logo: String? = null,
  @SerialName("runtime") var runtime: Int? = null,
  @SerialName("genres") var genres: List<String>? = null,
  @SerialName("content_rating") var contentRating: String? = null,
  @SerialName("rating") var rating: Double? = null,
  @SerialName("actors") var actors: List<Actor>? = null,
  @SerialName("homepage") var homepage: String? = null,
  @SerialName("videos") var videos: List<StreamData>? = null
) {
  fun getReleaseYear(): Int? {
    return releaseDateMsUTC?.getYear()
  }
}



