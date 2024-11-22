package cloud.app.vvf.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName
import com.uwetrottmann.trakt5.entities.Show

data class AnticipatedShow(
    @SerializedName("list_count")
    val listCount: Int,
    @SerializedName("show")
    val show: Show,
)
