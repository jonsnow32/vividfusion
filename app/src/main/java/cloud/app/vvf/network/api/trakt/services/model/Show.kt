package cloud.app.vvf.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName

data class Show(
    @SerializedName("ids")
    val ids: Ids,
    @SerializedName("title")
    val title: String,
    @SerializedName("year")
    val year: Int,
)
