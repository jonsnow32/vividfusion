package cloud.app.vvf.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.preference.PreferenceManager
import cloud.app.vvf.R

const val PHONE: Int = 0b001
const val TV: Int = 0b010
const val EMULATOR: Int = 0b100
private const val INVALID = -1
private var layoutId = INVALID


private fun Context.getLayoutInt(): Int {
  val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
  return settingsManager.getInt(this.getString(R.string.pref_layout), -1)
}

private fun Context.isAutoTv(): Boolean {
  val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager?
  // AFT = Fire TV
  val model = Build.MODEL.lowercase()
  return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION || Build.MODEL.contains(
    "AFT"
  ) || model.contains("firestick") || model.contains("fire tv") || model.contains("chromecast")
}

private fun Context.layoutIntCorrected(): Int {
  return when (getLayoutInt()) {
    -1 -> if (isAutoTv()) TV else PHONE
    0 -> PHONE
    1 -> TV
    2 -> EMULATOR
    else -> PHONE
  }
}

fun Context.updateTv() {
  layoutId = layoutIntCorrected()
}

/** Returns true if the layout is any of the flags,
 * so isLayout(TV or EMULATOR) is a valid statement for checking if the layout is in the emulator
 * or tv. Auto will become the "TV" or the "PHONE" layout.
 *
 * Valid flags are: PHONE, TV, EMULATOR
 * */
fun Context.isLayout(flags: Int): Boolean {
  return (layoutId and flags) != 0
}


