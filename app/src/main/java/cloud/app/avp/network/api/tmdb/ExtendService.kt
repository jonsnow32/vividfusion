package cloud.app.avp.network.api.tmdb

import cloud.app.avp.network.api.tmdb.model.ImageResult
import com.uwetrottmann.tmdb2.entities.ReviewResultsPage
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ExtendService {
    @GET("movie/{id}/images")
    fun getImages(
        @Path("id") movieID: Int,
        @Query("language") language: String,
        @Query("include_image_language") imgLanguage: String,
    ): Call<ImageResult>


    @GET("tv/{id}/images")
    fun getTvImages(
        @Path("id") movieID: Int,
        @Query("language") language: String,
        @Query("include_image_language") imgLanguage: String,
    ): Call<ImageResult>


    @POST("tv/{tv_id}/reviews")
    fun getReviews(
        @Path("tv_id") tvShowId: Int,
        @Query("page") page: Int,
        @Query("language") language: String?,
    ): Call<ReviewResultsPage>
}
