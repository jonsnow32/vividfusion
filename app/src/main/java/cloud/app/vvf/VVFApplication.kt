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
import cloud.app.vvf.ads.AdManager
import cloud.app.vvf.ads.AdPreloadManager
import cloud.app.vvf.ads.providers.AdProvider
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.extension.Message
import cloud.app.vvf.extension.ExtensionLoader
import cloud.app.vvf.utils.setLocale
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class VVFApplication : Application(), Configuration.Provider, Application.ActivityLifecycleCallbacks {

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

  @Inject
  lateinit var adManager: AdManager

  @Inject
  lateinit var adPreloadManager: AdPreloadManager

  private val scope = MainScope() + CoroutineName("Application")
  private var currentActivity: Activity? = null
  private var isAppInForeground = false
  private var activeActivities = 0

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

    // Initialize AdManager and Enhanced Preload System
    scope.launch {
      try {
        adManager.initialize(this@VVFApplication)

        // Initialize preload manager after AdManager is ready
        adPreloadManager.initialize(this@VVFApplication)

        // Start intelligent preload
        adPreloadManager.startIntelligentPreload(isAppInForeground = true)

        Timber.i("Ad system with enhanced preload initialized successfully")
      } catch (e: Exception) {
        Timber.e(e, "Failed to initialize ad system")
      }
    }

    // Initialize Firebase
    FirebaseApp.initializeApp(this)
    val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
    val crashlytics = FirebaseCrashlytics.getInstance()

    // Optional: Set up Firebase Analytics and Crashlytics
    firebaseAnalytics.setAnalyticsCollectionEnabled(true)
    crashlytics.setCrashlyticsCollectionEnabled(true)
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    // Activity lifecycle tracking for preload optimization
  }

  override fun onActivityStarted(activity: Activity) {
    activeActivities++
    if (activeActivities == 1) {
      // App came to foreground
      isAppInForeground = true
      onAppForeground()
    }
  }

  override fun onActivityResumed(activity: Activity) {
    currentActivity = activity
  }

  override fun onActivityPaused(activity: Activity) {
    if (currentActivity == activity) currentActivity = null
  }

  override fun onActivityStopped(activity: Activity) {
    activeActivities--
    if (activeActivities == 0) {
      // App went to background
      isAppInForeground = false
      onAppBackground()
    }
  }

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

  override fun onActivityDestroyed(activity: Activity) {
    if (currentActivity == activity) currentActivity = null
  }

  /**
   * Called when app comes to foreground
   */
  private fun onAppForeground() {
    Timber.d("App came to foreground - optimizing ad preload")
    scope.launch {
      try {
        // Restart intelligent preload with foreground settings
        adPreloadManager.startIntelligentPreload(isAppInForeground = true)

        // Preload critical ads immediately
        currentActivity?.let { activity ->
          adPreloadManager.preloadOnDemand(activity, AdProvider.AdType.INTERSTITIAL)
        }
      } catch (e: Exception) {
        Timber.e(e, "Error optimizing preload on foreground")
      }
    }
  }

  /**
   * Called when app goes to background
   */
  private fun onAppBackground() {
    Timber.d("App went to background - switching to background preload mode")
    scope.launch {
      try {
        // Switch to background preload mode (less frequent)
        adPreloadManager.startIntelligentPreload(isAppInForeground = false)
      } catch (e: Exception) {
        Timber.e(e, "Error switching to background preload")
      }
    }
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
