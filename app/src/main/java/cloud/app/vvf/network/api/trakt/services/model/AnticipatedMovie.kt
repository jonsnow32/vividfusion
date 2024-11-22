package cloud.app.vvf.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName
import com.uwetrottmann.trakt5.entities.Movie

data class AnticipatedMovie(
    @SerializedName("list_count")
    val listCount: Int,
    @SerializedName("movie")
    val movie: Movie,
)
