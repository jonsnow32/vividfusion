package cloud.app.avp.network.api.realdebrid

import cloud.app.avp.network.api.realdebrid.model.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface RealDebridApi {

    @GET("rest/1.0/downloads")
    fun downloads(
        @Query("offset") offset: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Call<List<RealDebridDownloadedObjectItem>>

    @GET("rest/1.0/torrents")
    fun torrents(
        @Query("offset") offset: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("filter") filter: String,
    ): Call<List<RealDebridTorrentInfoObject>>

    @GET("rest/1.0/torrents/info/{id}")
    fun torrentInfos(@Path("id") id: String): Call<RealDebridTorrentInfoObject>

    @PUT("rest/1.0/torrents/addTorrent")
    fun addTorrent(@Query("host") host: String): Call<AddMagnetResponse>

    @FormUrlEncoded
    @POST("rest/1.0/torrents/addMagnet")
    fun addMagnet(
        @Field("magnet") magnet: String,
        @Field("host") host: String,
    ): Call<AddMagnetResponse>

    @GET("rest/1.0/torrents/instantAvailability/{hash}")
    fun instantAvailability(@Path("hash") hash: String): Call<ResponseBody>

    @FormUrlEncoded
    @POST("rest/1.0/torrents/selectFiles/{id}")
    fun selectFiles(@Path("id") id: String, @Field("files") files: String): Call<ResponseBody>

    @DELETE("rest/1.0/torrents/delete/{id}")
    fun delete(@Path("id") id: String): Call<ResponseBody> //Selected files IDs (comma separated) or "all"

    @FormUrlEncoded
    @POST("rest/1.0/unrestrict/check")
    fun unrestrictCheck(
        @Field("link") link: String,
        @Field("password") password: String?,
    ): Call<UnRestrictCheckObject>

    @FormUrlEncoded
    @POST("rest/1.0/unrestrict/link")
    fun unrestrictLink(
        @Field("link") link: String,
        @Field("password") password: String?,
        @Field("remote") remote: String?,
    ): Call<UnRestrictObject>

    @FormUrlEncoded
    @POST("rest/1.0/unrestrict/containerLink")
    fun unrestrictContainerLink(@Field("link") link: String): Call<List<String>>

    @get:GET("rest/1.0/user")
    val userInfo: Call<RealDebridUserInfor>
}
