package cloud.app.vvf.common.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadItem(
    val id: String,
    val mediaItem: AVPMediaItem,
    val url: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val localPath: String? = null,
    val downloadSpeed: Long = 0L, // bytes per second
    val connections: Int = 1, // number of download connections
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getProgressPercentage(): Int {
        return if (fileSize > 0) {
            ((downloadedBytes.toFloat() / fileSize.toFloat()) * 100).toInt()
        } else progress
    }

    fun isCompleted(): Boolean = status == DownloadStatus.COMPLETED

    fun canRetry(): Boolean = status in listOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED)

    fun isActive(): Boolean = status in listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PENDING)

    fun getFormattedSpeed(): String {
        if (downloadSpeed <= 0) return "0 B/s"

        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        val digitGroups = (kotlin.math.log10(downloadSpeed.toDouble()) / kotlin.math.log10(1024.0)).toInt()

        return String.format(
            java.util.Locale.getDefault(),
            "%.1f %s",
            downloadSpeed / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    fun getEstimatedTimeRemaining(): String {
        if (downloadSpeed <= 0 || fileSize <= downloadedBytes) return "Unknown"

        val remainingBytes = fileSize - downloadedBytes
        val remainingSeconds = remainingBytes / downloadSpeed

        return when {
            remainingSeconds < 60 -> "${remainingSeconds}s"
            remainingSeconds < 3600 -> "${remainingSeconds / 60}m ${remainingSeconds % 60}s"
            else -> "${remainingSeconds / 3600}h ${(remainingSeconds % 3600) / 60}m"
        }
    }
}

@Serializable
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
