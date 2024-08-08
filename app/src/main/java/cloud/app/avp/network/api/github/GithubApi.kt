package cloud.app.avp.network.api.github

import cloud.app.avp.network.api.github.model.GetGithubUserResponseModel
import cloud.app.avp.network.api.github.model.GithubUserModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubApi {
    @GET("search/users?q=repos:>1")
    suspend fun getUsersList(
        @Query("page") page: Int,
        @Query("per_page") pageSize: Int
    ): Response<GetGithubUserResponseModel>

    @GET("users/{username}")
    suspend fun getUserInfo(
        @Path("username") username: String
    ): Response<GithubUserModel>
}
