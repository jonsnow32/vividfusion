package cloud.app.vvf.di

import android.app.Application
import android.content.SharedPreferences
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.extension.ExtensionLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ExtensionModule {

  @Provides
  @Singleton
  fun provideExtensionList() = MutableStateFlow<List<Extension<*>>>(emptyList())

  @Provides
  @Singleton
  fun providesRefresher(): MutableSharedFlow<Boolean> = MutableStateFlow(false)

  @Provides
  @Singleton
  fun provideExtensionLoader(
    context: Application,
    dataStore: MutableStateFlow<AppDataStore>,
    httpHelper: HttpHelper,
    throwableFlow: MutableSharedFlow<Throwable>,
    extensionsFlow: MutableStateFlow<List<Extension<*>>>,
    refresher: MutableSharedFlow<Boolean>
  ) = ExtensionLoader(
    context,
    dataStore,
    httpHelper,
    throwableFlow,
    extensionsFlow,
    refresher
  )

}
