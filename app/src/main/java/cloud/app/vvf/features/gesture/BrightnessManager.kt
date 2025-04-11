package cloud.app.vvf.features.gesture

import android.app.Activity
import android.provider.Settings
import android.view.WindowManager

class BrightnessManager(private val activity: Activity) {

  var currentBrightness = activity.currentBrightness
  val maxBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL

  val brightnessPercentage get() = (currentBrightness / maxBrightness).times(100).toInt()

  fun setBrightness(brightness: Float) {
    currentBrightness = brightness.coerceIn(0f, maxBrightness)
    val layoutParams = activity.window.attributes
    layoutParams.screenBrightness = currentBrightness
    activity.window.attributes = layoutParams
  }

  val Activity.currentBrightness: Float
    get() = when (val brightness = window.attributes.screenBrightness) {
      in WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF..WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL -> brightness
      else -> Settings.System.getFloat(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255
    }


}
