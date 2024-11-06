package cloud.app.common.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
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

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    // For API level 26 and above, use java.time classes
    val instant = Instant.ofEpochMilli(this)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }
    year = localDateTime.year
  } else {
    // For API level below 26, use java.util.Calendar
    val calendar = Calendar.getInstance().apply { timeInMillis = this@toLocalMonthYear }
    month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Unknown"
    year = calendar.get(Calendar.YEAR)
  }

  return "$month $year"
}
