package cloud.app.avp.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName

data class Episode(
    @SerializedName("ids")
    val ids: Ids,
    @SerializedName("number")
    val number: Int,
    @SerializedName("season")
    val season: Int,
    @SerializedName("title")
    val title: String,
)
