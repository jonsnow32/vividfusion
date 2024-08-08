package cloud.app.avp.utils

import cloud.app.avp.utils.Utils.getDefaultDatePattern
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

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

  fun convertTimestampToLocalTime(timestamp: Long, zoneId: String = ZoneId.systemDefault().id): String {
    val instant = Instant.ofEpochSecond(timestamp)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of(zoneId))
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return localDateTime.format(formatter)
  }
}
