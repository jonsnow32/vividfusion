package cloud.app.vvf.ui.download

import android.os.StatFs
import android.os.Environment
import java.io.File

/**
 * Data class representing device storage information
 */
data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val appUsedBytes: Long,
    val freeBytes: Long
) {
    val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) * 100f else 0f
    val appUsedPercentage: Float get() = if (totalBytes > 0) (appUsedBytes.toFloat() / totalBytes) * 100f else 0f
    val freePercentage: Float get() = if (totalBytes > 0) (freeBytes.toFloat() / totalBytes) * 100f else 0f

    companion object {
        /**
         * Get storage information for the device
         */
        fun getDeviceStorageInfo(downloadDir: File? = null): StorageInfo {
            return try {
                // Get external storage stats (where downloads are typically stored)
                val externalStorageDir = Environment.getExternalStorageDirectory()
                val stat = StatFs(externalStorageDir.path)

                val totalBytes = stat.blockCountLong * stat.blockSizeLong
                val freeBytes = stat.availableBytes
                val usedBytes = totalBytes - freeBytes

                // Calculate app usage from downloads directory
                val appUsedBytes = downloadDir?.let { dir ->
                    if (dir.exists()) calculateDirectorySize(dir) else 0L
                } ?: 0L

                StorageInfo(
                    totalBytes = totalBytes,
                    usedBytes = usedBytes,
                    appUsedBytes = appUsedBytes,
                    freeBytes = freeBytes
                )
            } catch (e: Exception) {
                // Return empty storage info if we can't read storage
                StorageInfo(0L, 0L, 0L, 0L)
            }
        }

        /**
         * Calculate the total size of a directory recursively
         */
        private fun calculateDirectorySize(directory: File): Long {
            return try {
                var size = 0L
                if (directory.exists()) {
                    directory.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            size += file.length()
                        }
                    }
                }
                size
            } catch (e: Exception) {
                0L
            }
        }

        /**
         * Format bytes to human readable string
         */
        fun formatBytes(bytes: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var value = bytes.toDouble()
            var unitIndex = 0

            while (value >= 1024 && unitIndex < units.size - 1) {
                value /= 1024
                unitIndex++
            }

            return "%.2f %s".format(value, units[unitIndex])
        }
    }
}
