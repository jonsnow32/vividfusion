package cloud.app.vvf.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import cloud.app.vvf.R
import cloud.app.vvf.databinding.ToastBinding
import java.util.Locale

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


/**
 * Not all languages can be fetched from locale with a code.
 * This map allows sidestepping the default Locale(languageCode)
 * when setting the app language.
 **/
val appLanguageExceptions = hashMapOf(
  "zh-rTW" to Locale.TRADITIONAL_CHINESE
)

fun Context.colorFromAttribute(attribute: Int): Int {
  val attributes = obtainStyledAttributes(intArrayOf(attribute))
  val color = attributes.getColor(0, 0)
  attributes.recycle()
  return color
}
fun Context.setLocale(languageCode: String?) {
  if (languageCode == null) return
  val locale = appLanguageExceptions[languageCode] ?: Locale(languageCode)
  val resources: Resources = resources
  val config = resources.configuration
  Locale.setDefault(locale)
  config.setLocale(locale)
  createConfigurationContext(config)
  @Suppress("DEPRECATION")
  resources.updateConfiguration(config, resources.displayMetrics)
}

fun Context.showToast(message: String, idRes: Int? = null, duration: Int = Toast.LENGTH_SHORT) {
  val layoutInflater = LayoutInflater.from(this)
  val binding = ToastBinding.inflate(layoutInflater)
  binding.toastText.text = message
  if (idRes != null) binding.icon.setImageDrawable(ContextCompat.getDrawable(this, idRes))
  else binding.icon.visibility = View.GONE
  Toast(this).apply {
    this.duration = duration
    setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
    view = binding.root
    show()
  }
}
fun Context.showToast(msgID: Int, idRes: Int? = null, duration: Int = Toast.LENGTH_SHORT) {
  showToast(getString(msgID, idRes, duration))
}
