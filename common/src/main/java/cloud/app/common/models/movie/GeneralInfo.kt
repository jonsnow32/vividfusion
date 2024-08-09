package cloud.app.common.models.movie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
  var rating: Double? = null
) : Parcelable
