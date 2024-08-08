package cloud.app.avp.network.api.sstream

import cloud.app.avp.network.api.sstream.model.AdConfig
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface SStreamApi {
    @GET("app/ads")
    suspend fun getAds(): Response<List<AdConfig>>

    @GET("app/housead")
    suspend fun getHouseAd(): Response<ResponseBody>
}
