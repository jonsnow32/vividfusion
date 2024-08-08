package cloud.app.avp.network.di

import cloud.app.avp.BuildConfig
import cloud.app.avp.network.api.openSubtitle.OpenSubtitleOAuthSettings
import cloud.app.avp.network.api.openSubtitle.OpenSubtitleV1Api
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class OpenSubtitleModule {
    companion object {
        const val BASE_URL = "https://api.opensubtitles.com/api/"
    }

    @Provides
    fun provideOpenSubtitleApi(
        okHttpClient: OkHttpClient,
        openSubtitleOAuthSettings: OpenSubtitleOAuthSettings
    ): OpenSubtitleV1Api {
        val okhttpBuilder = okHttpClient.newBuilder()
            .addInterceptor(Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("User-Agent", "AVP v${BuildConfig.VERSION_CODE}")
                    .addHeader("Api-Key", "UX4Bbf6bR9IjfBwKJYmPLpClPotd1g2w")

                val token = openSubtitleOAuthSettings.getAccessToken()
                if (token.isNotEmpty())
                    newRequest.addHeader(
                        "Authorization",
                        "Bearer $token"
                    )
                chain.proceed(newRequest.build());
            })

        val retrofit =
            Retrofit.Builder().baseUrl(BASE_URL).client(okhttpBuilder.build())
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .serializeNulls()
                            .create()
                    )
                ).build()
        return retrofit.create(OpenSubtitleV1Api::class.java)
    }
}
