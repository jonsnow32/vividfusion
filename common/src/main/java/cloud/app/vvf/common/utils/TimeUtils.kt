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


fun secondsToReadable(seconds: Int?): String? {
  var secondsLong = seconds?.toLong() ?: return null
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
