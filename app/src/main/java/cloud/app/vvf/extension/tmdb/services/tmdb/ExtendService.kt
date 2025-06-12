package cloud.app.vvf.extension.tmdb.services.tmdb


import cloud.app.vvf.extension.tmdb.services.tmdb.model.TmdbCountry
import cloud.app.vvf.extension.tmdb.services.tmdb.model.ImageResult
import cloud.app.vvf.extension.tmdb.services.tmdb.model.WatchProviders
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


    @GET("configuration/countries")
    fun getContries(): Call<List<TmdbCountry>>


    @POST("tv/{tv_id}/reviews")
    fun getReviews(
        @Path("tv_id") tvShowId: Int,
        @Query("page") page: Int,
        @Query("language") language: String?,
    ): Call<ReviewResultsPage>

  @GET("tv/{tv_id}/watch/providers")
  fun watchTvProviders(
    @Path("tv_id") tvShowId: Int
  ): Call<WatchProviders>

  @GET("movie/{movie_id}/watch/providers")
  fun watchMovieProviders(
    @Path("movie_id") movieId: Int
  ): Call<WatchProviders>

}
