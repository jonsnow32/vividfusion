package cloud.app.vvf.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName

data class Person(
    @SerializedName("ids")
    val ids: Ids,
    @SerializedName("name")
    val name: String,
)
