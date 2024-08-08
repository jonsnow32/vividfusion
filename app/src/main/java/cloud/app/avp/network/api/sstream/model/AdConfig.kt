package cloud.app.avp.network.api.sstream.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdConfig(
    val name: String,
    val appID: String,
    val banner: String,
    val eCpm: Double,
    val interstitial: String,
    val native: String
) : Parcelable
