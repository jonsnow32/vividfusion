package cloud.app.avp.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import cloud.app.avp.viewmodels.SnackBarViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
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
  fun provideSettingsPreferences(application: Application): SharedPreferences =
    application.getSharedPreferences(application.packageName, Context.MODE_PRIVATE)

}
