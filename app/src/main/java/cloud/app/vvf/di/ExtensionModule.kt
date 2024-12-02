package cloud.app.vvf.di

import android.app.Application
import android.content.SharedPreferences
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.extension.ExtensionLoader
import cloud.app.vvf.common.clients.DatabaseExtension
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.StreamExtension
import cloud.app.vvf.common.clients.SubtitleExtension
import cloud.app.vvf.common.helpers.network.HttpHelper
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
  fun provideCurrentDatabaseExtension() = MutableStateFlow<DatabaseExtension?>(null)


  @Provides
  @Singleton
  fun provideExtensionList() = MutableStateFlow<List<Extension<*>>>(emptyList())

  @Provides
  @Singleton
  fun provideDatabaseExtensionList() = MutableStateFlow<List<DatabaseExtension>>(emptyList())


  @Provides
  @Singleton
  fun provideCurrentStreamExtension() = MutableStateFlow<StreamExtension?>(null)

  @Provides
  @Singleton
  fun provideStreamExtensionList() = MutableStateFlow<List<StreamExtension>>(emptyList())


  @Provides
  @Singleton
  fun provideCurrentSubtitleExtension() = MutableStateFlow<SubtitleExtension?>(null)

  @Provides
  @Singleton
  fun provideSubtitleExtensionList() = MutableStateFlow<List<SubtitleExtension>>(emptyList())

  @Provides
  @Singleton
  fun providesRefresher(): MutableSharedFlow<Boolean> = MutableStateFlow(false)

  @Provides
  @Singleton
  fun provideExtensionLoader(
    context: Application,
    dataStore: DataStore,
    httpHelper: HttpHelper,
    sharedPreferences: SharedPreferences,
    throwableFlow: MutableSharedFlow<Throwable>,
    databaseExtensionListFlow: MutableStateFlow<List<DatabaseExtension>>,
    databaseExtensionFlow: MutableStateFlow<DatabaseExtension?>,

    streamExtensionListFlow: MutableStateFlow<List<StreamExtension>>,
    streamExtensionFlow: MutableStateFlow<StreamExtension?>,

    subtitleExtensionListFlow: MutableStateFlow<List<SubtitleExtension>>,
    subtitleExtensionFlow: MutableStateFlow<SubtitleExtension?>,

    extensionsFlow: MutableStateFlow<List<Extension<*>>>,
    refresher: MutableSharedFlow<Boolean>
  ) = ExtensionLoader(
    context,
    dataStore,
    httpHelper,
    throwableFlow,
    sharedPreferences,
    databaseExtensionListFlow,
    databaseExtensionFlow,

    streamExtensionListFlow,
    streamExtensionFlow,

    subtitleExtensionListFlow,
    subtitleExtensionFlow,
    extensionsFlow,
    refresher
  )

}
