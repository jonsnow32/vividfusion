package cloud.app.vvf.network.api.sstream.model


import kotlinx.serialization.Serializable

@Serializable
data class AdConfig(
    val name: String,
    val appID: String,
    val banner: String,
    val eCpm: Double,
    val interstitial: String,
    val native: String
)
