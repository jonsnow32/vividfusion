package cloud.app.avp.network.api.github.model

import com.google.gson.annotations.SerializedName

data class GetGithubUserResponseModel(
    @SerializedName("total_count") var totalCount: Long,
    @SerializedName("incomplete_results") var incompleteResults: Boolean,
    @SerializedName("items") var items: List<GithubUserModel>
)
