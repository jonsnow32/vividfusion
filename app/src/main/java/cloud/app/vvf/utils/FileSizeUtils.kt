package cloud.app.vvf.utils

import android.os.StatFs

fun Long.toHumanReadableSize(): String {
    if (this < 1024) return "$this B"
    val z = (63 - java.lang.Long.numberOfLeadingZeros(this)) / 10
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
    return String.format("%.2f %s", this / Math.pow(1024.0, z.toDouble()), units[z])
}

fun getFreeSpace(path: String): Long {
  val stat = StatFs(path)
  val availableBytes = stat.availableBytes
  return availableBytes
}
