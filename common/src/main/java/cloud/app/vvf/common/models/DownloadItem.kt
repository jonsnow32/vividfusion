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
