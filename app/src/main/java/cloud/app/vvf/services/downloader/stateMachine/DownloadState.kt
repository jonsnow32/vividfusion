package cloud.app.vvf.services.downloader.stateMachine

import androidx.work.WorkInfo
import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.DownloadStatus

/**
 * Represents the internal state of a download operation with rich data
 * Generic to support different types of download data
 */
sealed class DownloadState {
  data object Idle : DownloadState()
  data object Queued : DownloadState()

  data class Running(
    val downloadData: DownloadData
  ) : DownloadState() {
    // Backward compatibility
    val progress: Int get() = downloadData.progress
    val downloadedBytes: Long get() = downloadData.downloadedBytes
    val totalBytes: Long get() = downloadData.totalBytes
  }

  data object Paused : DownloadState()

  data class Completed(
    val localPath: String,
    val fileSize: Long,
    val downloadData: DownloadData? = null
  ) : DownloadState()

  data class Failed(
    val error: String,
    val downloadData: DownloadData? = null
  ) : DownloadState()

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
    val downloadData: DownloadData
  ) : DownloadEvent() {
    // Backward compatibility
    val progress: Int get() = downloadData.progress
    val downloadedBytes: Long get() = downloadData.downloadedBytes
    val totalBytes: Long get() = downloadData.totalBytes
  }

  data class WorkCompleted(
    val downloadId: String,
    val localPath: String,
    val fileSize: Long,
    val downloadData: DownloadData? = null
  ) : DownloadEvent()

  data class WorkFailed(
    val downloadId: String,
    val error: String,
    val downloadData: DownloadData? = null
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
      val downloadData = extractDownloadDataFromWorkInfo(this)
      if (downloadData.progress > 0 || downloadData.downloadedBytes > 0) {
        DownloadEvent.ProgressUpdated(downloadId, downloadData)
      } else {
        DownloadEvent.WorkStarted(downloadId)
      }
    }

    WorkInfo.State.SUCCEEDED -> {
      val localPath = this.outputData.getString("localPath") ?: ""
      val fileSize = this.outputData.getLong("fileSize", 0L)
      val downloadData = extractDownloadDataFromWorkInfo(this, isOutput = true)
      DownloadEvent.WorkCompleted(downloadId, localPath, fileSize, downloadData)
    }

    WorkInfo.State.FAILED -> {
      val error = this.outputData.getString("error") ?: "Unknown error"
      val downloadData = extractDownloadDataFromWorkInfo(this, isOutput = true)
      DownloadEvent.WorkFailed(downloadId, error, downloadData)
    }

    WorkInfo.State.CANCELLED -> DownloadEvent.WorkCancelled(downloadId)
    WorkInfo.State.BLOCKED -> null // Ignore blocked state
  }
}

/**
 * Extract DownloadData from WorkInfo progress or output data
 */
private fun extractDownloadDataFromWorkInfo(workInfo: WorkInfo, isOutput: Boolean = false): DownloadData {
  val data = if (isOutput) workInfo.outputData else workInfo.progress

  val builder = DownloadData.Companion.DownloadDataBuilder()
    .progress(data.getInt("progress", 0))
    .downloadedBytes(data.getLong("downloadedBytes", 0L))
    .totalBytes(data.getLong("totalBytes", 0L))
    .downloadSpeed(data.getLong("downloadSpeed", 0L))
    .fileName(data.getString("fileName"))

  // Torrent-specific data
  val uploadSpeed = data.getLong("uploadSpeed", -1L)
  if (uploadSpeed >= 0) builder.uploadSpeed(uploadSpeed)

  val peers = data.getInt("peers", -1)
  if (peers >= 0) builder.peers(peers)

  val seeds = data.getInt("seeds", -1)
  if (seeds >= 0) builder.seeds(seeds)

  val totalPeers = data.getInt("totalPeers", -1)
  if (totalPeers >= 0) builder.totalPeers(totalPeers)

  val shareRatio = data.getFloat("shareRatio", -1f)
  if (shareRatio >= 0) builder.shareRatio(shareRatio)

  val torrentState = data.getString("torrentState")
  if (torrentState != null) builder.torrentState(torrentState)

  val bytesRead = data.getLong("bytesRead", -1L)
  if (bytesRead >= 0) builder.bytesRead(bytesRead)

  val bytesWritten = data.getLong("bytesWritten", -1L)
  if (bytesWritten >= 0) builder.bytesWritten(bytesWritten)

  val preloadedBytes = data.getLong("preloadedBytes", -1L)
  if (preloadedBytes >= 0) builder.preloadedBytes(preloadedBytes)

  val eta = data.getLong("eta", -1L)
  if (eta >= 0) builder.eta(eta)

  // HLS-specific data
  val segmentsDownloaded = data.getInt("segmentsDownloaded", -1)
  if (segmentsDownloaded >= 0) builder.segmentsDownloaded(segmentsDownloaded)

  val totalSegments = data.getInt("totalSegments", -1)
  if (totalSegments >= 0) builder.totalSegments(totalSegments)

  val quality = data.getString("quality")
  if (quality != null) builder.quality(quality)

  val encryption = data.getString("encryption")
  if (encryption != null) builder.encryption(encryption)

  // HTTP-specific data
  val connections = data.getInt("connections", -1)
  if (connections >= 0) builder.connections(connections)

  builder.resumeSupported(data.getBoolean("resumeSupported", false))

  val contentType = data.getString("contentType")
  if (contentType != null) builder.contentType(contentType)

  // Common data
  val streamUrl = data.getString("streamUrl")
  if (streamUrl != null) builder.streamUrl(streamUrl)


  return builder.build()
}
