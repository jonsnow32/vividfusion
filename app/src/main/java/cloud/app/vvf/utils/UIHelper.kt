package cloud.app.vvf.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import androidx.core.view.WindowInsetsCompat

object UIHelper {
  /**
   * Retrieves the status bar height in pixels, returning 0 for TV or emulator layouts.
   *
   * @return The height of the status bar in pixels, or 0 if layout is TV or emulator.
   */
  fun Context.getStatusBarHeight(): Int {
    // If you still need to check for TV or emulator, keep this logic
    // Assuming these are defined elsewhere
    if (isLayout(TV) || isLayout(EMULATOR)) return 0

    // For Activities, you can get the WindowInsets from the current window
    (this as? Activity)?.let { activity ->
      val insets = WindowInsetsCompat.toWindowInsetsCompat(
        activity.window.decorView.rootWindowInsets
      )
      return insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
    }

    // Fallback for non-Activity contexts or if WindowInsets isn't available
    return 0
  }
  /**
   * Shows or hides the status bar and returns its height when shown.
   *
   * @param hide True to hide the status bar, false to show it.
   * @return The status bar height if shown, 0 if hidden.
   */
  fun Activity.changeStatusBarState(hide: Boolean): Int {
    val window = window ?: return 0
    return if (hide) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.hide(WindowInsets.Type.statusBars())
      } else {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
      }
      0
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.show(WindowInsets.Type.statusBars())
      } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
      }
      getStatusBarHeight()
    }
  }

  /**
   * Hides the system UI (status and navigation bars) using immersive sticky mode.
   * Uses stable flags for compatibility across API levels.
   */
  fun Activity.hideSystemUI(overlapNotch: Boolean = false) {
    val window = window ?: return
    val decorView = window.decorView
    @Suppress("DEPRECATION")
    decorView.systemUiVisibility = (
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_FULLSCREEN
      )
    // Note: Consider migrating to WindowInsetsController for modern APIs in future stable releases
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && overlapNotch) {
      val params = window.attributes
      params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      window.attributes = params
    }
  }

  /**
   * Shows the system UI (status and navigation bars), adjusting based on layout type.
   */
  fun Activity.showSystemUI() {
    val window = window ?: return
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

    // simply resets brightness and notch settings that might have been overridden
    val lp = window.attributes
    lp?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      lp?.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
    }
    window.attributes = lp
    val decorView = window.decorView
    @Suppress("DEPRECATION")
    decorView.systemUiVisibility = (
      View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      )
    changeStatusBarState(isLayout(EMULATOR))
    // Note: Consider migrating to WindowInsetsController for modern APIs in future stable releases
  }

  fun View.hasNotch(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val insets: WindowInsets? = rootWindowInsets
      insets?.displayCutout != null
    } else {
      false // No notch or API < 28
    }
  }

}
