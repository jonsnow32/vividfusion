package cloud.app.avp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import cloud.app.avp.datastore.FOLDER_APP_SETTINGS
import cloud.app.avp.plugin.TmdbExtension
import cloud.app.avp.utils.tryWith
import cloud.app.avp.viewmodels.SnackBarViewModel
import cloud.app.common.clients.BaseExtension
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.settings.PrefSettings
import cloud.app.plugger.RepoComposer
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
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
  lateinit var sharedPreferences: SharedPreferences

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
    applyUiChanges(
      sharedPreferences
    )
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
              init(toSettings(client::javaClass.toString()), httpHelper)
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
    fun Application.applyUiChanges(
      preferences: SharedPreferences,
      newTheme: String? = null,
      newColor: Int? = null,
      currentActivity: Activity? = null
    ) {
      var theme: String? = null

      theme = newTheme ?: preferences.getString(getString(R.string.pref_theme_key), "system")

      val customColor =
        if (!preferences.getBoolean(getString(R.string.enable_dynamic_color_key), false)) null
        else newColor ?: preferences.getInt(getString(R.string.dynamic_color_key), -1)
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
  }

  fun toSettings(clientID: String) = object : PrefSettings {
    override fun getString(key: String) =
      sharedPreferences.getString("$FOLDER_APP_SETTINGS/$clientID/$key", null)

    override fun putString(key: String, value: String?) {
      sharedPreferences.edit { putString("$FOLDER_APP_SETTINGS/$clientID/$key", value) }
    }

    override fun getInt(key: String) =
      if (sharedPreferences.contains(key)) sharedPreferences.getInt(
        "$FOLDER_APP_SETTINGS/$clientID/$key",
        0
      )
      else null

    override fun putInt(key: String, value: Int?) {
      sharedPreferences.edit { putInt(key, value) }
    }

    override fun getBoolean(key: String) =
      if (sharedPreferences.contains(key)) sharedPreferences.getBoolean(
        "$FOLDER_APP_SETTINGS/$clientID/$key",
        false
      )
      else null

    override fun putBoolean(key: String, value: Boolean?) {
      sharedPreferences.edit { putBoolean("$FOLDER_APP_SETTINGS/$clientID/$key", value) }
    }
  }
}
