package cloud.app.vvf.network.api.openSubtitle

import cloud.app.vvf.network.api.openSubtitle.models.*
import retrofit2.Call
import retrofit2.http.*

interface OpenSubtitleV1Api {

    @POST("v1/login")
    fun login(
        @Body loginRequest: LoginRequest
    ): Call<LoginResponse>

    @DELETE("v1/logout")
    fun logout(): Call<BaseResponse>

    /**
     * @param languages : Language code(s), coma separated (en,fr)
     * @param query file name or text search
     * @param type is movie, episode or all
     * @param userID : To be used alone - for user uploads listing
     * @param year : filter by movie/episode year
     */

    @GET("v1/subtitles")
    fun searchSubtitle(
        @Query("moviehash") moviehash: String?,
        @Query("imdb_id") imdbID: Int?,
        @Query("tmdb_id") tmdbID: Int?,

        @Query("parent_imdb_id") imdbShowID: Int?,
        @Query("parent_tmdb_id") tmdbShowID: Int?,
        @Query("season_number") seasonNumber: Int?,
        @Query("episode_number") episodeNumber: Int?,

        @Query("query") query: String?,

        @Query("type") type: String,
        @Query("languages") languages: String,
        @Query("year") year: Int?,
        @Query("page") page: Int,
    ): Call<Subtitles>


    @POST("v1/download")
    @Headers("Content-Type: application/json", "Accept: */*")
    fun download(@Body downloadRequest: DownloadRequest): Call<DownloadResponse>

    @GET("v1/infos/languages")
    fun getLanguages(): Call<LanguagesResponse>

    @GET("v1/infos/user")
    fun getUsers(): Call<UserInfoResponse>
}
