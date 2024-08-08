package cloud.app.avp.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName
import com.uwetrottmann.trakt5.entities.Movie

data class BoxOffice(
    @SerializedName("movie")
    val movie: Movie,
    @SerializedName("revenue")
    val revenue: Int,
)
