package cloud.app.vvf.di

import android.app.Application
import android.content.SharedPreferences
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.extension.Message
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
  fun provideSelectedDbExtension() = MutableStateFlow<Extension<DatabaseClient>?>(null)

  @Provides
  @Singleton
  fun provideExtensionList() = MutableStateFlow<List<Extension<*>>?>(null)

  @Provides
  @Singleton
  fun providesRefresher(): MutableSharedFlow<Boolean> = MutableStateFlow(false)

  @Provides
  @Singleton
  fun provideExtensionLoader(
    context: Application,
    httpHelper: HttpHelper,
    throwableFlow: MutableSharedFlow<Throwable>,
    extensionsFlow: MutableStateFlow<List<Extension<*>>>,
    messageFlow: MutableSharedFlow<Message>,
    refresher: MutableSharedFlow<Boolean>
  ) = ExtensionLoader(
    context.applicationContext,
    httpHelper,
    throwableFlow,
    extensionsFlow,
    messageFlow,
    refresher
  )

}
