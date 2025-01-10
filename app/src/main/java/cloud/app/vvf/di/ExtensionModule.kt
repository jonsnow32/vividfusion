package cloud.app.vvf.di

import android.app.Application
import android.content.SharedPreferences
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.clients.subtitles.SubtitleClient
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.datastore.DataStore
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
  fun provideCurrentDatabaseExtension() = MutableStateFlow<Extension<DatabaseClient>?>(null)

  @Provides
  @Singleton
  fun provideCurrentStreamExtension() = MutableStateFlow<Extension<StreamClient>?>(null)

  @Provides
  @Singleton
  fun provideCurrentSubtitleExtension() = MutableStateFlow<Extension<SubtitleClient>?>(null)

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

    currentDBExtFlow: MutableStateFlow<Extension<DatabaseClient>?>,
    currentStreamExtFlow: MutableStateFlow<Extension<StreamClient>?>,
    currentSubtitleExtFlow: MutableStateFlow<Extension<SubtitleClient>?>,

    extensionsFlow: MutableStateFlow<List<Extension<*>>>,
    refresher: MutableSharedFlow<Boolean>
  ) = ExtensionLoader(
    context,
    dataStore,
    httpHelper,
    throwableFlow,
    sharedPreferences,
    currentDBExtFlow,
    currentStreamExtFlow,
    currentSubtitleExtFlow,
    extensionsFlow,
    refresher
  )

}
