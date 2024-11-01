package cloud.app.avp.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.datastore.PREFERENCES_NAME
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
  fun provideJsonMapper(): JsonMapper {
    return JsonMapper.builder()
      .addModule(kotlinModule())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .build()
  }

  @Singleton
  @Provides
  fun provideDataStore(
    context: Context,
    jsonMapper: JsonMapper
  ): DataStore {
    return DataStore(context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE), jsonMapper)
  }
}
