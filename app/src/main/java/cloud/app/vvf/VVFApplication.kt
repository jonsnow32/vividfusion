package cloud.app.vvf

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cloud.app.vvf.extension.ExtensionLoader
import cloud.app.vvf.viewmodels.SnackBarViewModel
import cloud.app.vvf.common.helpers.network.HttpHelper
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
class VVFApplication : Application() {
  @Inject
  lateinit var throwableFlow: MutableSharedFlow<Throwable>

  @Inject
  lateinit var extensionLoader: ExtensionLoader

  @Inject
  lateinit var sharedPreferences: SharedPreferences

  @Inject
  lateinit var httpHelper: HttpHelper

  private val scope = MainScope() + CoroutineName("Application")

  override fun onCreate() {
    super.onCreate()

    Thread.setDefaultUncaughtExceptionHandler { _, exception ->
      exception.printStackTrace()
      ExceptionActivity.start(this, exception, true)
      Runtime.getRuntime().exit(0)
    }

    if(BuildConfig.DEBUG){
      Timber.plant(Timber.DebugTree())
    }

    applyUiChanges(
      sharedPreferences
    )

    scope.launch {
      throwableFlow.collect {
        it.printStackTrace()
      }
    }

    extensionLoader.initialize()
  }

  companion object {

    @SuppressLint("RestrictedApi")
    fun Application.applyUiChanges(
      preferences: SharedPreferences,
      newTheme: String? = null,
      newColor: Int? = null,
      currentActivity: Activity? = null
    ) {
      var theme: String? = null

      theme = newTheme ?: preferences.getString(getString(R.string.pref_theme), "system")

      val customColor =
        if (!preferences.getBoolean(getString(R.string.enable_dynamic_color), false)) null
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
      currentActivity?.recreate()
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

    fun Context.noClient() = SnackBarViewModel.Message(
      getString(R.string.error_no_client)
    )
    fun Context.loginNotSupported(client: String) = SnackBarViewModel.Message(
      getString(R.string.not_supported, getString(R.string.login), client)
    )
  }
}
