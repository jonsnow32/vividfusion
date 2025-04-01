package cloud.app.vvf.common.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
  val seconds = this / 1000
  val minutes = seconds / 60
  val hours = minutes / 60
  val days = hours / 24

  val parts = mutableListOf<String>()

  if (days > 0) {
    parts.add("${days}d")
  }
  if (hours % 24 > 0) {
    parts.add("${hours % 24}h")
  }
  if (minutes % 60 > 0) {
    parts.add("${minutes % 60}m")
  }
  if (seconds % 60 > 0) {
    parts.add("${seconds % 60}s")
  }

  return when {
    parts.isEmpty() -> "0s"
    else -> parts.joinToString(" ")
  }
}
