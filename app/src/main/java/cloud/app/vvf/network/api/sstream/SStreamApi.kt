package cloud.app.vvf.network.api.sstream

import cloud.app.vvf.network.api.sstream.model.AdConfig
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface SStreamApi {
    @GET("app/ads")
    suspend fun getAds(): Response<List<AdConfig>>

    @GET("app/housead")
    suspend fun getHouseAd(): Response<ResponseBody>
}
