package cloud.app.avp.network.di

import android.content.SharedPreferences
import cloud.app.common.helpers.network.USER_AGENT
import cloud.app.avp.network.api.realdebrid.RealDebirdOAuthSettings
import cloud.app.avp.network.api.realdebrid.RealDebridApi
import cloud.app.avp.network.api.realdebrid.RealDebridOauthApi
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@InstallIn(SingletonComponent::class)
@Module
class RealdebirdModule {
    companion object {
        val BASE_URL = "https://api.real-debrid.com/"
    }

    @Provides
    fun provideRealDebridOauthApi(
        okHttpClient: OkHttpClient,
        sharedPreferences: SharedPreferences
    ): RealDebridOauthApi {
        val retrofit =
            Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient.newBuilder().build())
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .serializeNulls()
                            .create()
                    )
                ).build()
        return retrofit.create(RealDebridOauthApi::class.java)
    }

    @Provides
    fun provideRealDebirdApi(
        pref: SharedPreferences,
        okHttpClient: OkHttpClient,
        rdAuthenticator: RDAuthenticator
    ): RealDebridApi {
        val builder = okHttpClient.newBuilder()
            .authenticator(rdAuthenticator)
            .addInterceptor(Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        "Bearer " + RealDebirdOAuthSettings.getAccessToken(pref)
                    )
                    .addHeader("User-Agent", USER_AGENT)
                    .build();
                chain.proceed(newRequest);
            })
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(builder.build())
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .serializeNulls()
                        .create()
                )
            )
            .build()
        return retrofit.create(RealDebridApi::class.java)
    }

    class RDAuthenticator @Inject constructor(
        val pref: SharedPreferences,
        val realDebridOauthApi: RealDebridOauthApi
    ) : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            val clientId = RealDebirdOAuthSettings.getClientID(pref);
            val clientSecret = RealDebirdOAuthSettings.getClientSecret(pref);
            val refresh_token = RealDebirdOAuthSettings.getRefreshToken(pref);
            if (clientId.isNullOrEmpty())
                throw Exception("Please check your RealDebrid subscription first!\nGoto: Setting/Premium accounts/Real debrid")
            val body = mapOf(
                "user_agent" to USER_AGENT,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "code" to refresh_token,
                "grant_type" to "http://oauth.net/grant_type/device/1.0"
            )
            val tokenresponse = realDebridOauthApi.oauthtoken(body).execute()
            if (tokenresponse != null && tokenresponse.isSuccessful) {
                tokenresponse.body()?.let {
                    if (!it.access_token.isNullOrEmpty() && !it.refresh_token.isNullOrEmpty()) {
                        RealDebirdOAuthSettings.storeRefreshData(
                            pref,
                            it.access_token!!,
                            it.refresh_token!!,
                            it.expires_in.toLong()
                        )
                        return response.request.newBuilder()
                            .header(
                                "Authorization",
                                "Bearer " + it.access_token
                            )
                            .header(
                                "User-Agent",
                                "Bearer " + USER_AGENT
                            )
                            .build()
                    }
                }
            }
            return null
        }
    }
}

