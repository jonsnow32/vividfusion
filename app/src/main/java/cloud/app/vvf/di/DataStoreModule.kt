package cloud.app.vvf.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.datastore.account.AccountDataStore
import cloud.app.vvf.datastore.app.AppDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataStoreModule {

  @Singleton
  @Provides
  fun provideJson() = Json {
    ignoreUnknownKeys = true
  }

  @Singleton
  @Provides
  fun provideAccountDataStore(context: Context) =
    MutableStateFlow<AccountDataStore>(AccountDataStore(context))

  @Singleton
  @Provides
  fun provideAccountFlow(dataStore: MutableStateFlow<AccountDataStore>) =
    MutableStateFlow<Account>(dataStore.value.getActiveAccount())

  @Singleton
  @Provides
  fun provideAppDataStore(context: Context, accountFlow: MutableStateFlow<Account>) =
    MutableStateFlow<AppDataStore>(AppDataStore(context, accountFlow.value))
}
