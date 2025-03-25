package cloud.app.vvf.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import cloud.app.vvf.BuildConfig
import cloud.app.vvf.R
import cloud.app.vvf.network.Interceptors.CacheInterceptor
import cloud.app.vvf.network.Interceptors.addAdGuardDns
import cloud.app.vvf.network.Interceptors.addCloudFlareDns
import cloud.app.vvf.network.Interceptors.addDNSWatchDns
import cloud.app.vvf.network.Interceptors.addGoogleDns
import cloud.app.vvf.network.Interceptors.addQuad9Dns
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.helpers.network.ignoreAllSSLErrors
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.utils.AppUpdater
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

  val cacheSize: Long = 10 * 1024 * 1024  // 10 MB

  @Provides
  @Singleton
  fun provideCache(app: Application): Cache {
    return Cache(app.cacheDir, cacheSize)
  }

  @Provides
  @Singleton
  fun provideOkHttpClient(
    loggingInterceptor: HttpLoggingInterceptor,
    cache: Cache,
    cookiePersistor: PersistentCookieJar,
    sharedPreferences: SharedPreferences,
    context: Context,
  ): OkHttpClient {
    val dns = sharedPreferences.getInt(context.getString(R.string.dns_pref), 0)
    return OkHttpClient.Builder()
      .followRedirects(true)
      .followSslRedirects(true)
      .cookieJar(cookiePersistor)
      .ignoreAllSSLErrors()
      .cache(cache).apply {
        when (dns) {
          1 -> addGoogleDns()
          2 -> addCloudFlareDns()
//                3 -> addOpenDns()
          4 -> addAdGuardDns()
          5 -> addDNSWatchDns()
          6 -> addQuad9Dns()
        }
      }
      .addInterceptor(loggingInterceptor)
      .addNetworkInterceptor(CacheInterceptor())
      //.addInterceptor(OfflineCacheInterceptor())

      .build()
  }

  @Provides
  @Singleton
  fun provideHttpHelper(okHttpClient: OkHttpClient) = HttpHelper(okHttpClient)

  @Provides
  fun provideLoggingInterceptor(): HttpLoggingInterceptor =
    HttpLoggingInterceptor().apply {
      level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
      } else {
        HttpLoggingInterceptor.Level.NONE
      }
    }

  @Provides
  fun provideGson(): Gson = Gson()

  @Provides
  fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory =
    GsonConverterFactory.create(gson)

  @Provides
  fun provideCookieJar(context: Context): PersistentCookieJar =
    PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context));


  @Provides
  @Singleton
  fun provideAppUpdate(context: Context, client: OkHttpClient, sharedPreferences: SharedPreferences) = AppUpdater(context, client, "jonsnow32", "vividfusion", context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown")

}
