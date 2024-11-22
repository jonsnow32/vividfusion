package cloud.app.vvf.network.api.trakt.services

import cloud.app.vvf.network.api.trakt.services.model.*
import cloud.app.vvf.network.api.trakt.services.model.stats.UserStats
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ExtendService {
    @GET("lists/trending")
    fun trendingFeatureList(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<FeatureListResult>

    @GET("lists/popular")
    fun popularFeatureList(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<FeatureListResult>

    @GET("lists/{id}/items/movies,shows,episodes")
    fun FeatureListItems(
        @Path("id") listID: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<ListItemResult>

    @GET("movies/anticipated")
    fun anticipatedMovies(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<List<AnticipatedMovie>>

    @GET("shows/anticipated")
    fun anticipatedShows(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<List<AnticipatedShow>>

    @GET("movies/watched/period")
    fun mostWatchedMovie(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<List<MostWatchedAndCollectedMovie>>

    @GET("movies/collected/period")
    fun mostCollectedMovie(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<List<MostWatchedAndCollectedMovie>>

    @GET("shows/watched/period")
    fun mostWatchedShow(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<List<MostWatchedAndCollectedShow>>

    @GET("shows/collected/period")
    fun mostCollectedShow(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<List<MostWatchedAndCollectedShow>>


    @GET("/movies/boxoffice")
    fun boxOffice(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<List<BoxOffice>>

    @GET("/users/{id}/stats")
    fun getUserStats(@Path("id") id: String): Call<UserStats>
}
