package cloud.app.vvf.utils

import kotlin.math.pow
import kotlin.math.roundToInt

fun Double.roundTo(numFractionDigits: Int): Double {
  val factor = 10.0.pow(numFractionDigits.toDouble())
  return (this * factor).roundToInt() / factor
}

fun Double.roundTo(numFractionDigits: Double) = String.format("%.${numFractionDigits}f", this)
