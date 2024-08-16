package cloud.app.avp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import cloud.app.avp.plugin.tmdb.TmdbExtension
import cloud.app.avp.utils.toSettings
import cloud.app.avp.utils.tryWith
import cloud.app.avp.viewmodels.SnackBarViewModel
import cloud.app.common.clients.BaseExtension
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.plugger.RepoComposer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient
import javax.inject.Inject


@HiltAndroidApp
class AVPApplication : Application() {
  @Inject
  lateinit var throwableFlow: MutableSharedFlow<Throwable>

  @Inject
  lateinit var extensionFlow: MutableStateFlow<BaseExtension?>

  @Inject
  lateinit var extensionFlowList: MutableStateFlow<List<BaseExtension>>

  @Inject
  lateinit var extensionRepo: RepoComposer<BaseExtension>

  @Inject
  lateinit var preferences: SharedPreferences

  @Inject
  lateinit var httpHelper: HttpHelper

  private val scope = MainScope() + CoroutineName("Application")

  override fun onCreate() {
    super.onCreate()

    Thread.setDefaultUncaughtExceptionHandler { _, exception ->
      exception.printStackTrace()
      ExceptionActivity.start(this, exception)
      Runtime.getRuntime().exit(0)
    }

    scope.launch {
      throwableFlow.collect {
        it.printStackTrace()
      }
    }

    scope.launch {
      extensionRepo.load().collect {
        it.forEach { client ->
          tryWith(throwableFlow) {
            client.init(toSettings(preferences), httpHelper)
            if(client is TmdbExtension) {
              extensionFlow.emit(client)
            }
            //client.onExtensionSelected()
          }
        }
        extensionFlowList.emit(it)
      }
    }
  }

  companion object {
    fun Context.restartApp() {
      val mainIntent = Intent.makeRestartActivityTask(
        packageManager.getLaunchIntentForPackage(packageName)!!.component
      )
      startActivity(mainIntent)
      Runtime.getRuntime().exit(0)
    }
    fun Context.noClient() = SnackBarViewModel.Message(
      getString(R.string.error_no_client)
    )
  }
}
