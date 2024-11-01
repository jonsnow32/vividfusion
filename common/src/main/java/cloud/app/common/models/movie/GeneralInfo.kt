package cloud.app.common.models.movie

import android.os.Build
import android.os.Parcelable
import androidx.annotation.RequiresApi
import cloud.app.common.models.Actor
import cloud.app.common.models.ActorData
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

@Parcelize
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
  private var slug: String? = null
) : Parcelable {

  fun getReleaseYear(): Int? {
    return releaseDateMsUTC?.let {
      val calendar = Calendar.getInstance()
      calendar.timeInMillis = it
      calendar.get(Calendar.YEAR)
    }
  }

  fun getSlug(): String? {
    if(slug == null) {
      val formattedName = originalTitle
        .trim()
        .lowercase()
        .replace("[^a-z0-9\\s]".toRegex(), "") // Remove special characters
        .replace("\\s+".toRegex(), "-")        // Replace spaces with hyphens
      slug = "$formattedName-${getReleaseYear()}"
    }
    return slug;
  }

}



