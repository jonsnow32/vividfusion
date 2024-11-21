package cloud.app.avp.di

import android.app.Application
import android.content.SharedPreferences
import cloud.app.avp.datastore.DataStore
import cloud.app.avp.extension.ExtensionLoader
import cloud.app.common.clients.DatabaseExtension
import cloud.app.common.clients.Extension
import cloud.app.common.clients.StreamExtension
import cloud.app.common.clients.SubtitleExtension
import cloud.app.common.clients.mvdatabase.DatabaseClient
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.settings.PrefSettings
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

    extensionsFlow: MutableStateFlow<List<Extension<*>>>
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
    extensionsFlow
  )

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


}
