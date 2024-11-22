package cloud.app.vvf.network.di

import android.content.SharedPreferences
import cloud.app.vvf.network.api.alldebrid.AllDebridApi
import cloud.app.vvf.network.api.alldebrid.AllDebridOAuthSettings
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@InstallIn(SingletonComponent::class)
@Module
class AllDebirdModule {
    @Provides
    fun provideAllDebridApi(
        okHttpClient: OkHttpClient,
        sharedPreferences: SharedPreferences
    ): AllDebridApi {

        val builder = okHttpClient.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val httpUrl =
                    chain.request().url.newBuilder()
                        .addQueryParameter("agent", "starOneTvAgent")
                        .addQueryParameter(
                            "apikey",
                            AllDebridOAuthSettings.getApiKey(sharedPreferences)
                        )
                        .build()
                val newRequest = chain.request().newBuilder().url(httpUrl).build()
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
        return retrofit.create(AllDebridApi::class.java)
    }

    companion object {
        const val BASE_URL = "https://api.alldebrid.com/"
    }
}

