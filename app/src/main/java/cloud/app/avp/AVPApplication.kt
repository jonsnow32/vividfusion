package cloud.app.avp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


@HiltAndroidApp
class AVPApplication : Application(){
  override fun onCreate() {
    super.onCreate()
    Thread.setDefaultUncaughtExceptionHandler { _, exception ->
      exception.printStackTrace()
      ExceptionActivity.start(this, exception)
      Runtime.getRuntime().exit(0)
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
  }
}
