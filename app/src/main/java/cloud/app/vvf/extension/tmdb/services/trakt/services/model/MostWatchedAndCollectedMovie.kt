package cloud.app.vvf.extension.tmdb.services.trakt.services.model


import com.google.gson.annotations.SerializedName
import com.uwetrottmann.trakt5.entities.Movie

data class MostWatchedAndCollectedMovie(
    @SerializedName("collected_count")
    val collectedCount: Int,
    @SerializedName("movie")
    val movie: Movie,
    @SerializedName("play_count")
    val playCount: Int,
    @SerializedName("watcher_count")
    val watcherCount: Int,
)
