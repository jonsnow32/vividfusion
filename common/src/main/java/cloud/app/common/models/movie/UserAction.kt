package cloud.app.common.models.movie

import java.time.OffsetDateTime

interface UserAction {
  var collectedAt: Long?
  var watchedAt: Long?
  var watchlistAt: Long?
  var focusAt: Long?
  var plays: Int
  var position: Long
  var hidden: Boolean
}
