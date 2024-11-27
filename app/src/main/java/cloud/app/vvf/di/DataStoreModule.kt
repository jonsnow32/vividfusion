package cloud.app.vvf.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.PREFERENCES_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataStoreModule {

  @Singleton
  @Provides
  fun provideSharedPreferences(context: Context): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(context)
  }

  @Singleton
  @Provides
  fun provideJson() = Json {
    ignoreUnknownKeys = true
  }

  @Singleton
  @Provides
  fun provideDataStore(
    context: Context,
  ): DataStore {
    return DataStore(
      context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
    )
  }
}
