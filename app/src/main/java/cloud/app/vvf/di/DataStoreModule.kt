package cloud.app.vvf.di

import android.content.Context
import android.content.SharedPreferences
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.datastore.account.AccountDataStore
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.utils.BackupHelper
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
  fun provideDefaultSharePreferences(context: Context): SharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)


  @Singleton
  @Provides
  fun provideAccountDataStore(context: Context) =
    MutableStateFlow(AccountDataStore(context))

  @Singleton
  @Provides
  fun provideAccountFlow(dataStore: MutableStateFlow<AccountDataStore>) =
    MutableStateFlow(dataStore.value.getActiveAccount())

  @Singleton
  @Provides
  fun provideAppDataStore(context: Context, accountFlow: MutableStateFlow<Account>) =
    MutableStateFlow(AppDataStore(context, accountFlow.value))

  @Singleton
  @Provides
  fun provideBackupHelper(context: Context) = BackupHelper(context)
}
