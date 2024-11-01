package cloud.app.common.models.movie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class  Episode(
  val ids: Ids,
  val generalInfo: GeneralInfo,
  var seasonNumber: Int,
  var episodeNumber: Int,
  var showIds: Ids,
  var showOriginTitle: String,
  var position : Long = 0,
  var updateTime : Long = System.currentTimeMillis()
) : Parcelable
