package cloud.app.vvf.extension.tmdb.services.trakt.services.model


import com.google.gson.annotations.SerializedName

data class Season(
    @SerializedName("ids")
    val ids: Ids,
    @SerializedName("number")
    val number: Int,
)
