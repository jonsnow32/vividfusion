package cloud.app.vvf.common.models.stream

import cloud.app.vvf.common.models.subtitle.SubtitleData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamData(
  @SerialName("original_url") var originalUrl: String,
  @SerialName("resolved_url") var resolvedUrl: String? = null,
  @SerialName("resolver_name") var resolverName: String? = null,
  @SerialName("provider_name") var providerName: String? = null,
  @SerialName("provider_logo") var providerLogo: String? = null,
  @SerialName("host_logo") var hostLogo: String? = null,
  @SerialName("premium_type") var premiumType: Int = PremiumType.Free.ordinal,
  @SerialName("stream_quality") var streamQuality: StreamQuality = StreamQuality.Unknow,
  @SerialName("file_size") var fileSize: Long = 0,
  @SerialName("file_name") var fileName: String = "",
  @SerialName("headers") var headers: HashMap<String, String>? = null,
  @SerialName("magnets") var magnets: List<MagnetObject>? = null,
  @SerialName("subtitles") var subtitles: List<SubtitleData>? = null
)
