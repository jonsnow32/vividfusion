package cloud.app.vvf.network.di

import cloud.app.vvf.network.api.torrentserver.TorrentServerApi
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
import javax.inject.Inject

@InstallIn(SingletonComponent::class)
@Module
class TorrentServerModule {
  @Provides
  fun provideTorrentServerApiFactory(okHttpClient: OkHttpClient): TorrentServerApiFactory {
    return TorrentServerApiFactory(okHttpClient)
  }
}

class TorrentServerApiFactory @Inject constructor(
  private val okHttpClient: OkHttpClient
) {
  companion object {
    const val USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
  }

  fun create(port: Long): TorrentServerApi {
    val builder = okHttpClient.newBuilder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .addInterceptor(Interceptor { chain ->
        val newRequest = chain.request().newBuilder()
          .addHeader("User-Agent", USER_AGENT)
          .build()
        chain.proceed(newRequest)
      })

    val retrofit = Retrofit.Builder()
      .client(builder.build())
      .addConverterFactory(
        GsonConverterFactory.create(
          GsonBuilder()
            .serializeNulls()
            .create()
        )
      )
      .baseUrl("http://127.0.0.1:$port/")
      .build()

    return retrofit.create(TorrentServerApi::class.java)
  }
}
