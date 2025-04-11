package cloud.app.vvf.common.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

fun Long.getYear(): Int {
  val calendar = Calendar.getInstance()
  calendar.timeInMillis = this
  return calendar.get(Calendar.YEAR)
}


fun Long.toLocalMonthYear(): String {
//  val month: String
//  val year: Int
//
//  val calendar = Calendar.getInstance().apply { timeInMillis = this@toLocalMonthYear }
//  month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
//  year = calendar.get(Calendar.YEAR)
//  return "$month $year"
//

  val formattedAirDate = SimpleDateFormat.getDateInstance(
    DateFormat.LONG,
    Locale.getDefault()
  ).apply {
  }.format(Date(this))

  return formattedAirDate
}


fun Int.secondsToReadable(): String? {
  var secondsLong = toLong()
  val days = TimeUnit.SECONDS
    .toDays(secondsLong)
  secondsLong -= TimeUnit.DAYS.toSeconds(days)

  val hours = TimeUnit.SECONDS
    .toHours(secondsLong)
  secondsLong -= TimeUnit.HOURS.toSeconds(hours)

  val minutes = TimeUnit.SECONDS
    .toMinutes(secondsLong)
  secondsLong -= TimeUnit.MINUTES.toSeconds(minutes)

  if (minutes < 0) {
    return null
  }
  //println("$days $hours $minutes")
  return "${if (days != 0L) "$days" + "d " else ""}${if (hours != 0L) "$hours" + "h " else ""}${minutes}m"
}

fun Long.millisecondsToReadable(): String? {
  val hours = TimeUnit.MILLISECONDS.toHours(this)
  val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
  val seconds = TimeUnit.MILLISECONDS.toSeconds(this) -
    TimeUnit.MINUTES.toSeconds(minutes) -
    TimeUnit.HOURS.toSeconds(hours)
  return if (hours > 0) {
    String.format("%02d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format("%02d:%02d", minutes, seconds)
  }
}


/**
 * Formats the given duration in milliseconds to a string in the format of
 * `+mm:ss` or `+hh:mm:ss` or `-mm:ss` or `-hh:mm:ss`.
 */

fun Long.millisecondsToReadableWithSign(): String {
  return if (this >= 0) {
    "+${millisecondsToReadable()}"
  } else {
    "-${abs(this).millisecondsToReadable()}"
  }
}
