package cloud.app.vvf.utils

import cloud.app.vvf.utils.Utils.getDefaultDatePattern
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date

object TimeUtils {
  private val TIME_PATTERN: String = getDefaultDatePattern()
  fun format(instant: Instant): String {
    return DateTimeFormatter.ofPattern(TIME_PATTERN).withZone(ZoneId.systemDefault())
      .format(instant)
  }

  fun format(rltime: String): String {
    val sdf = SimpleDateFormat(getDefaultDatePattern())
    return sdf.format(Date(rltime))
  }

  fun format(rltime: Long): String {
    val sdf = SimpleDateFormat(getDefaultDatePattern())
    return sdf.format(Date(rltime))
  }

  fun getYear(epoch: Long): Int {
    return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDateTime().year
  }

  fun format(date: Date): String {
    return "";
  }

  fun convertTimestampToLocalTime(
    timestamp: Long,
    zoneId: String = ZoneId.systemDefault().id
  ): String {
    val instant = Instant.ofEpochSecond(timestamp)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of(zoneId))
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return localDateTime.format(formatter)
  }

  fun Long.toLocalYear(): Int {
    val instant = Instant.ofEpochMilli(this)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return localDateTime.year
  }

  fun Long.toLocalMonthYear(): String {
    val instant = Instant.ofEpochMilli(this)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val month = localDateTime.month.name.lowercase().capitalize()
    val year = localDateTime.year
    return "$month $year"
  }

  fun Long.toLocalDayMonthYear(): String {
    val instant = Instant.ofEpochMilli(this)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

    // Create a DateTimeFormatter for the desired format
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    // Format the LocalDateTime using the formatter
    return localDateTime.format(formatter)
  }

}
