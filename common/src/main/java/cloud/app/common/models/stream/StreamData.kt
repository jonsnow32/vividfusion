package cloud.app.common.models.stream

import android.os.Parcelable
import cloud.app.common.models.subtitle.SubtitleData
import kotlinx.parcelize.Parcelize

@Parcelize
data class StreamData(
  var originalUrl: String,
  var resolvedUrl: String? = null,
  var resolverName: String? = null,
  var providerName: String? = null,
  var premiumType: Int = PremiumType.Free.ordinal,
  var streamQuality: StreamQuality = StreamQuality.Unknow,
  var fileSize: Long = 0,
  var fileName: String = "",
  var headers: HashMap<String, String>? = null,
  var magnets: List<MagnetObject>? = null,
  var subtitles: List<SubtitleData>? = null
) : Parcelable
