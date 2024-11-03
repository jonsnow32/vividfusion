package cloud.app.common.models.movie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Season(
  val title: String?,
  val number: Int,
  val overview: String?,
  val episodeCount: Int =  0,
  val posterPath: String?,
  val episodes: List<Episode> ? = null,
  var show: Show,
): Parcelable
