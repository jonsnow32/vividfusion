package cloud.app.avp

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cloud.app.avp.plugin.TmdbExtension
import cloud.app.avp.utils.toSettings
import cloud.app.avp.utils.tryWith
import cloud.app.avp.viewmodels.SnackBarViewModel
import cloud.app.common.clients.BaseExtension
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.plugger.RepoComposer
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.ThemeUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
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
      ExceptionActivity.start(this, exception, false)
      Runtime.getRuntime().exit(0)
    }
    applyUiChanges(preferences.getString(getString(R.string.pref_theme_key), "system"))
    scope.launch {
      throwableFlow.collect {
        it.printStackTrace()
      }
    }

    scope.launch {
      extensionRepo.load().collect {
        val extensionList = mutableListOf<BaseExtension>()
        it.forEach {
          tryWith(throwableFlow) {
            val client = it.value.getOrNull();
            client?.apply {
              init(toSettings(preferences), httpHelper)
              if (client is TmdbExtension) {
                extensionFlow.emit(client)
              }
              extensionList.add(client)
            }
          }
        }
        extensionFlowList.value = extensionList
      }
    }
  }

companion object {

  @SuppressLint("RestrictedApi")
  fun applyUiChanges(mode: String?) {


//    val customColor = if (!preferences.getBoolean(app.getString(R.string.theme_custom_key), false)) null
//    else preferences.getInt(app.getString(R.string.them_color), -1).takeIf { it != -1 }
//
//    val builder = if (customColor != null) DynamicColorsOptions.Builder()
//      .setContentBasedSource(customColor)
//    else DynamicColorsOptions.Builder()

//    theme?.let {
//      builder.setOnAppliedCallback {
//        ThemeUtils.applyThemeOverlay(it, theme)
//      }
//    }

//    DynamicColors.applyToActivitiesIfAvailable(app, builder.build())

    when (mode) {
      "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
      "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
      else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
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
}
}
