package cloud.app.avp.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName
import com.uwetrottmann.trakt5.entities.Episode
import com.uwetrottmann.trakt5.entities.Movie
import com.uwetrottmann.trakt5.entities.Show

data class ListItemItem(
    @SerializedName("episode")
    val episode: Episode,
    @SerializedName("listed_at")
    val listedAt: String,
    @SerializedName("movie")
    val movie: Movie,
    @SerializedName("person")
    val person: Person,
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("season")
    val season: Season,
    @SerializedName("show")
    val show: Show,
    @SerializedName("type")
    val type: String,
)
