package cloud.app.vvf.network.di

import cloud.app.vvf.network.api.voting.VotingService
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class VotingModule {
  @Provides
  fun provideVotingApi(
    okHttpClient: OkHttpClient,
  ): VotingService {
    val retrofit =
      Retrofit.Builder().baseUrl(COUNTER_API_URL).client(okHttpClient)
        .addConverterFactory(
          GsonConverterFactory.create(
            GsonBuilder()
              .serializeNulls()
              .create()
          )
        ).build()
    return retrofit.create(VotingService::class.java)
  }

  companion object {
    private const val COUNTER_API_URL = "https://counterapi.com/api/"
  }
}
