package cloud.app.common.models.movie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Show(
  val ids: Ids,
  val generalInfo: GeneralInfo,

  var tagLine: String? = null,
  var status: String = "continue" //continue, end
) : Parcelable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Movie
    return ids == other
  }

  override fun hashCode(): Int {
    var result = ids.hashCode()
    result = 31 * result + generalInfo.hashCode()
    return result
  }
}

