package cloud.app.common.models.movie


import kotlinx.serialization.Serializable

@Serializable
data class  Episode(
  val ids: Ids,
  val generalInfo: GeneralInfo,
  var seasonNumber: Int,
  var episodeNumber: Int,
  var showIds: Ids,
  var showOriginTitle: String,
  var position : Long = 0,
  var updateTime : Long = System.currentTimeMillis()
)
