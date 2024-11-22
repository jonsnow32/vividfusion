package cloud.app.vvf.common.utils

import java.util.Calendar
import java.util.Locale

fun Long.getYear(): Int {
  val calendar = Calendar.getInstance()
  calendar.timeInMillis = this
  return calendar.get(Calendar.YEAR)
}


fun Long.toLocalMonthYear(): String {
  val month: String
  val year: Int

  val calendar = Calendar.getInstance().apply { timeInMillis = this@toLocalMonthYear }
  month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
  year = calendar.get(Calendar.YEAR)
  return "$month $year"
}
