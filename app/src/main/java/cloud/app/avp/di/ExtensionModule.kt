package cloud.app.avp.di

import android.app.Application
import android.content.Context
import cloud.app.avp.network.api.tmdb.AppTmdb
import cloud.app.avp.plugin.LocalRepos
import cloud.app.avp.plugin.tmdb.TmdbExtension
import cloud.app.common.clients.BaseExtension
import cloud.app.plugger.RepoComposer
import cloud.app.plugger.repos.installedApk.InstalledApkConfig
import cloud.app.plugger.repos.installedApk.InstalledApkRepos
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ExtensionModule {

  @Provides
  @Singleton
  fun provideExtensionRepo(context: Application, tmdb: AppTmdb) = getComposer(context,tmdb)

  private fun getComposer(
    context: Context,
    tmdb : AppTmdb
  ): RepoComposer<BaseExtension> {
    val installedApkRepo = InstalledApkRepos<BaseExtension>(
      context,
      InstalledApkConfig("cloud.app.avp"),
    )
    val localRepo = LocalRepos(tmdb)
    return RepoComposer(installedApkRepo,localRepo)
  }

  @Provides
  @Singleton
  fun provideTmdbExtension(tmdb: AppTmdb) = TmdbExtension(tmdb)

  @Provides
  @Singleton
  fun provideExtensionFlow() = MutableStateFlow<BaseExtension?>(null)

  @Provides
  @Singleton
  fun provideExtensionFlowList() = MutableStateFlow<List<BaseExtension>>(emptyList())

}
