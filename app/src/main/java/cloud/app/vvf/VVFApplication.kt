package cloud.app.vvf

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.Message
import cloud.app.vvf.extension.ExtensionLoader
import cloud.app.vvf.utils.setLocale
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class VVFApplication : Application(), Application.ActivityLifecycleCallbacks,
  Configuration.Provider {
  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  @Inject
  lateinit var throwableFlow: MutableSharedFlow<Throwable>

  @Inject
  lateinit var extensionLoader: ExtensionLoader

  @Inject
  lateinit var sharedPreferences: SharedPreferences

  @Inject
  lateinit var httpHelper: HttpHelper

  private val scope = MainScope() + CoroutineName("Application")
  private var currentActivity: Activity? = null
  override fun onCreate() {
    super.onCreate()
    registerActivityLifecycleCallbacks(this)
    Thread.setDefaultUncaughtExceptionHandler { _, exception ->
      exception.printStackTrace()
      ExceptionActivity.start(this, exception, true)
      Runtime.getRuntime().exit(0)
    }

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    scope.launch {
      throwableFlow.collect {
        it.printStackTrace()
      }
    }
    applyUiChanges(sharedPreferences, currentActivity = currentActivity)
    extensionLoader.initialize()
  }


  override fun onActivityResumed(activity: Activity) {
    currentActivity = activity
  }

  override fun onActivityPaused(activity: Activity) {
    if (currentActivity == activity) currentActivity = null
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
  override fun onActivityStarted(activity: Activity) {}
  override fun onActivityStopped(activity: Activity) {}
  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
  override fun onActivityDestroyed(activity: Activity) {
    if (currentActivity == activity) currentActivity = null
  }

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .build()

  companion object {

    fun isDynamicColorSupported(): Boolean {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31)
        DynamicColors.isDynamicColorAvailable()
      } else {
        false // Below Android 12, Dynamic Colors are not supported
      }
    }

    fun Application.applyUiChanges(
      preferences: SharedPreferences,
      newTheme: String? = null,
      newColor: Int? = null,
      currentActivity: Activity? = null
    ) {


      var theme: String? = null

      theme = newTheme ?: preferences.getString(getString(R.string.pref_theme), "system")

      val customColor =
        if (!preferences.getBoolean(getString(R.string.pref_enable_dynamic_color), false)) null
        else newColor ?: preferences.getInt(getString(R.string.dynamic_color), -1)
          .takeIf { it != -1 }

      val builder = if (customColor != null) DynamicColorsOptions.Builder()
        .setContentBasedSource(customColor)
      else DynamicColorsOptions.Builder()

      DynamicColors.applyToActivitiesIfAvailable(this, builder.build())

      when (theme) {
        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
      }

      val code = preferences.getString(getString(R.string.pref_locale), "en")
      code?.let { setLocale(it) }

      (this as VVFApplication).currentActivity?.recreate()
// 🔹 Force Activity Restart Properly
//      currentActivity?.recreate()
    }


    fun applyLocale(sharedPref: SharedPreferences) {
      val value = sharedPref.getString("language", "system") ?: "system"
      val locale = if (value == "system") LocaleListCompat.getEmptyLocaleList()
      else LocaleListCompat.forLanguageTags(value)
      AppCompatDelegate.setApplicationLocales(locale)
    }

    fun Context.appVersion(): String = packageManager
      .getPackageInfo(packageName, 0)
      .versionName!!

    fun Context.restartApp() {
      val mainIntent = Intent.makeRestartActivityTask(
        packageManager.getLaunchIntentForPackage(packageName)!!.component
      )
      startActivity(mainIntent)
      Runtime.getRuntime().exit(0)
    }

    fun Context.noClient() = Message(
      getString(R.string.extension_empty)
    )

    fun Context.loginNotSupported(client: String) = Message(
      getString(R.string.not_supported, getString(R.string.login), client)
    )

    fun Context.createNotificationChannel(
      channelId: String,
      channelName: String,
      description: String
    ) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel =
          NotificationChannel(channelId, channelName, importance).apply {
            this.description = description
          }

        // Register the channel with the system.
        val notificationManager: NotificationManager =
          this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
      }
    }
  }

}
