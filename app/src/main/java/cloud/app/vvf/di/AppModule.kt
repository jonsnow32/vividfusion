package cloud.app.vvf.di

import android.app.Application
import android.content.Context
import cloud.app.vvf.viewmodels.SnackBarViewModel
import cloud.app.vvf.common.models.AVPMediaItem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

  @Provides
  fun provideAppContext(app: Application): Context = app.applicationContext

  @Provides
  @Singleton
  fun provideThrowableFlow() = MutableSharedFlow<Throwable>()

  @Provides
  @Singleton
  fun provideMessageFlow() = MutableSharedFlow<SnackBarViewModel.Message>()

  @Provides
  @Singleton
  fun provideUpdateUiFlow() = MutableStateFlow<AVPMediaItem?>(null)

}
