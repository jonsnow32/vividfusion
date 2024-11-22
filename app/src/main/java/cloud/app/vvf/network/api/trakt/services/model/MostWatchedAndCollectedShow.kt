package cloud.app.vvf.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName
import com.uwetrottmann.trakt5.entities.Show

data class MostWatchedAndCollectedShow(
    @SerializedName("collected_count")
    val collectedCount: Int,
    @SerializedName("show")
    val show: Show,
    @SerializedName("play_count")
    val playCount: Int,
    @SerializedName("watcher_count")
    val watcherCount: Int,
)
