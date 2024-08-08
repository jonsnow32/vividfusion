package cloud.app.avp.network.api.trakt.services.model


import com.google.gson.annotations.SerializedName
import com.uwetrottmann.trakt5.entities.User


data class FeatureList(
    @SerializedName("allow_comments")
    val allowComments: Boolean,
    @SerializedName("comment_count")
    val commentCount: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("display_numbers")
    val displayNumbers: Boolean,
    @SerializedName("ids")
    val ids: ListIds,
    @SerializedName("item_count")
    val itemCount: Int,
    @SerializedName("likes")
    val likes: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("privacy")
    val privacy: String,
    @SerializedName("sort_by")
    val sortBy: String,
    @SerializedName("sort_how")
    val sortHow: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("user")
    val user: User,
) {
    data class ListIds(
        @SerializedName("slug")
        val slug: String,
        @SerializedName("trakt")
        val trakt: Int,
    )
}
