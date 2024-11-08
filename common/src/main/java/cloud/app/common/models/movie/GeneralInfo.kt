package cloud.app.common.models.movie

import android.os.Build

import androidx.annotation.RequiresApi
import cloud.app.common.models.Actor
import cloud.app.common.models.ActorData
import cloud.app.common.utils.getYear
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

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
  var actors: List<ActorData>? = null,

) {
  fun getReleaseYear(): Int? {
    return releaseDateMsUTC?.getYear()
  }
}



