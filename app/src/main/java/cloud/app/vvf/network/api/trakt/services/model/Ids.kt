package cloud.app.vvf.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName

data class Ids(
    @SerializedName("imdb")
    val imdb: String?,
    @SerializedName("tmdb")
    val tmdb: Int,
    @SerializedName("trakt")
    val trakt: Int,
    @SerializedName("tvdb")
    val tvdb: Int,
)
