package cloud.app.vvf.services.downloader

import androidx.work.WorkInfo
import cloud.app.vvf.common.models.DownloadStatus

/**
 * Represents the internal state of a download operation
 * This is separate from UI DownloadStatus to avoid conflicts
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data object Queued : DownloadState()
    data class Running(
        val progress: Int = 0,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L
    ) : DownloadState()
    data object Paused : DownloadState()
    data class Completed(
        val localPath: String,
        val fileSize: Long
    ) : DownloadState()
    data class Failed(val error: String) : DownloadState()
    data object Cancelled : DownloadState()

    /**
     * Convert internal state to UI-friendly DownloadStatus
     */
    fun toDownloadStatus(): DownloadStatus {
        return when (this) {
            is Idle -> DownloadStatus.PENDING
            is Queued -> DownloadStatus.PENDING
            is Running -> DownloadStatus.DOWNLOADING
            is Paused -> DownloadStatus.PAUSED
            is Completed -> DownloadStatus.COMPLETED
            is Failed -> DownloadStatus.FAILED
            is Cancelled -> DownloadStatus.CANCELLED
        }
    }

    /**
     * Check if this state represents an active download
     */
    fun isActive(): Boolean {
        return this is Queued || this is Running
    }

    /**
     * Check if this state can be paused
     */
    fun canPause(): Boolean {
        return this is Queued || this is Running
    }

    /**
     * Check if this state can be resumed
     */
    fun canResume(): Boolean {
        return this is Paused
    }
}

/**
 * Represents a download command/action
 */
sealed class DownloadCommand {
    data class Start(
        val downloadId: String,
        val url: String,
        val fileName: String
    ) : DownloadCommand()

    data class Pause(val downloadId: String) : DownloadCommand()
    data class Resume(val downloadId: String) : DownloadCommand()
    data class Cancel(val downloadId: String) : DownloadCommand()
    data class Remove(val downloadId: String) : DownloadCommand()
}

/**
 * Represents a download event from WorkManager
 */
sealed class DownloadEvent {
    data class WorkEnqueued(val downloadId: String) : DownloadEvent()
    data class WorkStarted(val downloadId: String) : DownloadEvent()
    data class ProgressUpdated(
        val downloadId: String,
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadEvent()
    data class WorkCompleted(
        val downloadId: String,
        val localPath: String,
        val fileSize: Long
    ) : DownloadEvent()
    data class WorkFailed(
        val downloadId: String,
        val error: String
    ) : DownloadEvent()
    data class WorkCancelled(val downloadId: String) : DownloadEvent()
}

/**
 * Maps WorkInfo.State to DownloadEvent
 */
fun WorkInfo.toDownloadEvent(downloadId: String): DownloadEvent? {
    return when (this.state) {
        WorkInfo.State.ENQUEUED -> DownloadEvent.WorkEnqueued(downloadId)
        WorkInfo.State.RUNNING -> {
            val progress = this.progress.getInt("progress", 0)
            val downloadedBytes = this.progress.getLong("downloadedBytes", 0L)
            val totalBytes = this.progress.getLong("totalBytes", 0L)

            if (progress > 0 || downloadedBytes > 0) {
                DownloadEvent.ProgressUpdated(downloadId, progress, downloadedBytes, totalBytes)
            } else {
                DownloadEvent.WorkStarted(downloadId)
            }
        }
        WorkInfo.State.SUCCEEDED -> {
            val localPath = this.outputData.getString("localPath") ?: ""
            val fileSize = this.outputData.getLong("fileSize", 0L)
            DownloadEvent.WorkCompleted(downloadId, localPath, fileSize)
        }
        WorkInfo.State.FAILED -> {
            val error = this.outputData.getString("error") ?: "Unknown error"
            DownloadEvent.WorkFailed(downloadId, error)
        }
        WorkInfo.State.CANCELLED -> DownloadEvent.WorkCancelled(downloadId)
        WorkInfo.State.BLOCKED -> null // Ignore blocked state
    }
}
