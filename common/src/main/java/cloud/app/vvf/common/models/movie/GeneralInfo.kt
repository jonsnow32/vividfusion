package cloud.app.vvf.common.models.movie

import cloud.app.vvf.common.models.Actor
import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.common.utils.getYear
import kotlinx.serialization.Serializable

@Serializable
data class GeneralInfo(
  var title: String,
  var originalTitle: String,
  var overview: String? = null,
  var releaseDateMsUTC: Long? = null,
  var poster: String? = null,
  var backdrop: String? = null,
  var logo: String? = null,
  var runtime: Int? = null,
  var genres: List<String>? = null,
  var contentRating: String? = null,
  var rating: Double? = null,
  var actors: List<Actor>? = null,
  var homepage: String? = null,
  var videos: List<StreamData>? = null
) {
  fun getReleaseYear(): Int? {
    return releaseDateMsUTC?.getYear()
  }
}



