package cloud.app.vvf.network.api.realdebrid

import cloud.app.vvf.network.api.realdebrid.model.RealDebridCheckAuthResult
import cloud.app.vvf.network.api.realdebrid.model.RealDebridGetDeviceCodeResult
import cloud.app.vvf.network.api.realdebrid.model.RealDebridGetTokenResult
import retrofit2.Call
import retrofit2.http.*

interface RealDebridOauthApi {
    @GET("oauth/v2/device/code?new_credentials=yes")
    fun oauthDeviceCode(@Query("client_id") clientID: String = "X245A4XAIBGVM"): Call<RealDebridGetDeviceCodeResult>

    @GET("oauth/v2/device/credentials")
    fun oauthDeviceCredentials(
        @Query("client_id") clientID: String = "X245A4XAIBGVM",
        @Query("code") code: String,
    ): Call<RealDebridCheckAuthResult>

    @POST("oauth/v2/token")
    @FormUrlEncoded
    fun oauthtoken(@FieldMap params: Map<String, String?>): Call<RealDebridGetTokenResult?>
}
