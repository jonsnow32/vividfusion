package cloud.app.vvf.network.di

import android.content.SharedPreferences
import cloud.app.vvf.common.helpers.network.USER_AGENT
import cloud.app.vvf.network.api.premiumize.PremiumizeApi
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
class PremiumizeModule {
    @Provides
    fun providePremiumizeApi(
        okHttpClient: OkHttpClient,
        sharedPreferences: SharedPreferences
    ): PremiumizeApi {

        val builder = okHttpClient.newBuilder()
            .addInterceptor(Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("accept", "application/json")
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
        return retrofit.create(PremiumizeApi::class.java)
    }

    companion object {
        const val BASE_URL = "https://www.premiumize.me"
    }

}

