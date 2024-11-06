package cloud.app.common.models.movie


import kotlinx.serialization.Serializable

@Serializable
data class Season(
  val title: String?,
  val number: Int,
  val overview: String?,
  val episodeCount: Int =  0,
  val posterPath: String?,
  val backdrop: String?,
  val episodes: List<Episode> ? = null,
  var showIds: Ids,
  var showOriginTitle: String?,
  var releaseDateMsUTC: Long?,
)
