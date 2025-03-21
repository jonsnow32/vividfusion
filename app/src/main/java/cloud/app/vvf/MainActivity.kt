package cloud.app.vvf

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color.TRANSPARENT
import android.graphics.Rect
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import cloud.app.vvf.ExtensionOpenerActivity.Companion.openExtensionInstaller
import cloud.app.vvf.MainActivityViewModel.Companion.isNightMode
import cloud.app.vvf.databinding.ActivityMainBinding
import cloud.app.vvf.databinding.ConfirmExitDialogBinding
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.features.player.PlayerManager
import cloud.app.vvf.utils.TV
import cloud.app.vvf.utils.Utils.isAndroidTV
import cloud.app.vvf.utils.isLayout
import cloud.app.vvf.utils.openItemFragmentFromUri
import cloud.app.vvf.utils.setDefaultFocus
import cloud.app.vvf.utils.setLocale
import cloud.app.vvf.utils.tv.screenHeight
import cloud.app.vvf.utils.updateTv
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.configureSnackBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
  val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
  private val mainActivityViewModel by viewModels<MainActivityViewModel>()

  @Inject
  lateinit var sharedPreferences: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {

    PlayerManager.getInstance().setActivityResultRegistry(activityResultRegistry)
    lifecycle.addObserver(PlayerManager.getInstance())
    super.onCreate(savedInstanceState)
    PlayerManager.getInstance()
      .inject(
        sharedPreferences,
        this
      ) // call after activity injected

    setContentView(binding.root)

    enableEdgeToEdge(
      SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
      if (isNightMode()) SystemBarStyle.dark(TRANSPARENT)
      else SystemBarStyle.light(TRANSPARENT, TRANSPARENT)
    )

    configureSnackBar(binding.root)

    updateTv()
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      mainActivityViewModel.setSystemInsets(this, insets)
      insets
    }

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    val deviceIds = inputManager.inputDeviceIds
    val hasController = isAndroidTV(this) || isControllerConnected(inputManager, deviceIds.toList())
    //center view on focus change
    if (hasController) {
      supportFragmentManager.addOnBackStackChangedListener {
        val fragment = supportFragmentManager.fragments.lastOrNull()
        fragment?.let {
          Timber.i("Fragment ID: ${it.id}")
          // If you need the fragment tag or other properties:
          Timber.i("Fragment Tag: ${it.tag}")
        }
      }
    }
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

          var hasPoppedChild = false

          // Loop through all fragments and pop child fragments first
          for (fragment in supportFragmentManager.fragments) {
            if (fragment.isVisible && fragment.childFragmentManager.backStackEntryCount > 0) {
              fragment.childFragmentManager.popBackStack()
              hasPoppedChild = true
              break // Stop after popping one set of child fragments
            }
          }

          if (!hasPoppedChild) {
            if (!supportFragmentManager.popBackStackImmediate()) {
              // If no fragments left, show exit confirmation
              val canShowDialog =
                sharedPreferences.getBoolean(getString(R.string.pref_show_exit_confirm), true)
              if (canShowDialog) {
                showConfirmExitDialog(sharedPreferences)
              } else {
                moveTaskToBack(true) // Move app to background
              }
            }
          }
        }
      }
    )


    if (hasController) {
      val bottomButtons =
        listOf<Int>(R.id.homePreviewPlay, R.id.homePreviewBookmark, R.id.homePreviewInfo)
      binding.root.viewTreeObserver.addOnGlobalFocusChangeListener { _, view ->
        if (bottomButtons.contains(view?.id)) {
          bottomView(view)
          return@addOnGlobalFocusChangeListener
        }
        centerView(view)
      }
    }

    addOnNewIntentListener { onIntent(it) }
    onIntent(intent)

    val localeCode = sharedPreferences.getString(getString(R.string.pref_locale), "en")
    setLocale(localeCode)
    checkUpdate()
  }

  private fun checkUpdate() {
    val isUpdating = sharedPreferences.getBoolean("isUpdating", false)
    val updatingVersion = sharedPreferences.getString("updatingVersion", null)
    val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName

    if (isUpdating && updatingVersion != null && currentVersion == updatingVersion) {
      // Update succeeded
      Timber.i("Update completed: $currentVersion")
      showUpdateSuccessDialog(updatingVersion)
      sharedPreferences.edit().clear().apply() // Clear update state
    } else if (isUpdating) {
      // Update was in progress but didn’t complete (e.g., cancelled)
      Timber.w("Update was in progress but didn’t complete. Current: $currentVersion, Expected: $updatingVersion")
      sharedPreferences.edit().clear().apply()
    }
  }
  private fun showUpdateSuccessDialog(newVersion: String) {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.update_success_title)
      .setMessage(getString(R.string.update_success_message, newVersion))
      .setPositiveButton(android.R.string.ok) { _, _ ->
        // Optional: Add action, e.g., restart or proceed
      }
      .setCancelable(false)
      .show()
  }

  private fun onIntent(intent: Intent?) {
    this.intent = null
    intent ?: return
    val uri = intent.data
    when (uri?.scheme) {
      "vvf" -> openItemFragmentFromUri(uri)
      "file" -> openExtensionInstaller(uri)
    }
  }

  private fun centerView(view: View?) {
    if (view == null) return
    try {
      Timber.i("centerView: $view")
      val r = Rect(0, 0, 0, 0)
      view.getDrawingRect(r)
      val x = r.centerX()
      val y = r.centerY()
      val dx = r.width() / 2
      val dy = screenHeight / 2
      val newRect = Rect(x - dx, y - dy, x + dx, y + dy)
      view.requestRectangleOnScreen(newRect, false)
      // TvFocus.current =TvFocus.current.copy(y=y.toFloat())
    } catch (_: Throwable) {
    }
  }

  private fun bottomView(view: View?) {
    if (view == null) return
    try {
      Timber.i("bottomView: $view")
      val r = Rect(0, 0, 0, 0)
      view.getDrawingRect(r)
      val x = r.centerX()
      val y = r.bottom // Move focus towards the bottom of the view
      val dx = r.width() / 2
      val dy = screenHeight - r.height() // Adjust positioning towards the bottom
      val newRect = Rect(x - dx, y - dy, x + dx, y)
      view.requestRectangleOnScreen(newRect, false)
    } catch (_: Throwable) {
    }
  }

  private fun isControllerConnected(inputManager: InputManager, deviceIds: List<Int>): Boolean {
    for (deviceId in deviceIds) {
      val device = inputManager.getInputDevice(deviceId)
      device?.let {
        if (device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
          device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        ) {
          return true
        }
      }
    }
    return false
  }

  private fun showConfirmExitDialog(settingsManager: SharedPreferences) {
    val binding = ConfirmExitDialogBinding.inflate(LayoutInflater.from(this))
    MaterialAlertDialogBuilder(this)
      .setView(binding.root)
      .setTitle(R.string.exit_confirm_msg)
      .setNegativeButton(R.string.no) { _, _ -> /*NO-OP*/ }
      .setPositiveButton(R.string.yes) { _, _ ->
        if (binding.checkboxDontShowAgain.isChecked) {
          settingsManager.edit().putBoolean(getString(R.string.pref_show_exit_confirm), true)
            .apply()
        }
        if (isLayout(TV)) exitProcess(0) else finish()
      }.show().setDefaultFocus()
  }


}
