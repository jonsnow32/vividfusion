package cloud.app.vvf.common.models.video

import cloud.app.vvf.common.models.stream.MagnetObject
import cloud.app.vvf.common.models.stream.PremiumType
import cloud.app.vvf.common.models.stream.Resolution
import cloud.app.vvf.common.models.stream.Streamable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Video : Streamable{
  abstract val title: String?
  abstract val description: String?
  abstract val duration: Long?
  abstract val thumbnailUri: String?
  @Serializable
  data class LocalVideo(
    override var uri: String,
    override val title: String,
    override val description: String? = null,
    override val duration: Long,
    override val thumbnailUri: String,
    val id: String,
    val fileSize: Long? = null,
    val dateAdded: Long,
    val album: String,
    val width: Int? = null,
    val height: Int? = null
  ) : Video()

  @Serializable
  data class RemoteVideo(
    override var uri: String,
    override val title: String? = null,
    override val description: String? = null,
    override val duration: Long? = null,
    override val thumbnailUri: String? = null,
    val originalUrl: String? = null,

    val bitrate: Int? = null,
    val serverName: String? = null,
    val streamingProtocol: String? = null,

    @SerialName("resolver_name") var resolverName: String? = null,
    @SerialName("provider_name") var providerName: String? = null,
    @SerialName("provider_logo") var providerLogo: String? = null,
    @SerialName("host_logo") var hostLogo: String? = null,
    @SerialName("premium_type") var premiumType: Int = PremiumType.Free.ordinal,
    @SerialName("stream_quality") var streamQuality: Resolution = Resolution.Unknow,
    @SerialName("file_size") var fileSize: Long = 0,
    @SerialName("file_name") var fileName: String = "",
    @SerialName("headers") var headers: HashMap<String, String>? = null,
    @SerialName("magnets") var magnets: List<MagnetObject>? = null,
  ) : Video()


  val addedTime = when (this) {
    is LocalVideo -> dateAdded
    is RemoteVideo -> null
  }
}
