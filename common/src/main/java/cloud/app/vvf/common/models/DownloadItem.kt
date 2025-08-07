package cloud.app.vvf.common.models

import kotlinx.serialization.Serializable

@Serializable
sealed class DownloadItem {
  abstract val id: String
  abstract val mediaItem: AVPMediaItem
  abstract val url: String
  abstract val fileName: String
  abstract val fileSize: Long
  abstract val downloadedBytes: Long
  abstract val status: DownloadStatus
  abstract val progress: Int
  abstract val localPath: String?
  abstract val downloadSpeed: Long
  abstract val createdAt: Long
  abstract var updatedAt: Long

  // copyWith function to create a copy of the download item with updated fields
  abstract fun copyWith(
    id: String = this.id,
    mediaItem: AVPMediaItem = this.mediaItem,
    url: String = this.url,
    fileName: String = this.fileName,
    fileSize: Long = this.fileSize,
    downloadedBytes: Long = this.downloadedBytes,
    status: DownloadStatus = this.status,
    progress: Int = this.progress,
    localPath: String? = this.localPath,
    downloadSpeed: Long = this.downloadSpeed,
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt
  ): DownloadItem

  // Common functions for all download types
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
    val digitGroups =
      (kotlin.math.log10(downloadSpeed.toDouble()) / kotlin.math.log10(1024.0)).toInt()

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

  // HTTP/HTTPS Downloads
  @Serializable
  data class HttpDownload(
    override val id: String,
    override val mediaItem: AVPMediaItem,
    override val url: String,
    override val fileName: String,
    override val fileSize: Long = 0L,
    override val downloadedBytes: Long = 0L,
    override val status: DownloadStatus = DownloadStatus.PENDING,
    override val progress: Int = 0,
    override val localPath: String? = null,
    override val downloadSpeed: Long = 0L,
    override val createdAt: Long = System.currentTimeMillis(),
    override var updatedAt: Long = System.currentTimeMillis(),
    // HTTP-specific properties
    val connections: Int = 1, // number of download connections
    val resumeSupported: Boolean = true,
    val headers: Map<String, String> = emptyMap()
  ) : DownloadItem() {
    override fun copyWith(
      id: String,
      mediaItem: AVPMediaItem,
      url: String,
      fileName: String,
      fileSize: Long,
      downloadedBytes: Long,
      status: DownloadStatus,
      progress: Int,
      localPath: String?,
      downloadSpeed: Long,
      createdAt: Long,
      updatedAt: Long
    ): DownloadItem {
      return copy(
        id = id,
        mediaItem = mediaItem,
        url = url,
        fileName = fileName,
        fileSize = fileSize,
        downloadedBytes = downloadedBytes,
        status = status,
        progress = progress,
        localPath = localPath,
        downloadSpeed = downloadSpeed,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
    }
  }

  // HLS (HTTP Live Streaming) Downloads
  @Serializable
  data class HlsDownload(
    override val id: String,
    override val mediaItem: AVPMediaItem,
    override val url: String, // m3u8 playlist URL
    override val fileName: String,
    override val fileSize: Long = 0L,
    override val downloadedBytes: Long = 0L,
    override val status: DownloadStatus = DownloadStatus.PENDING,
    override val progress: Int = 0,
    override val localPath: String? = null,
    override val downloadSpeed: Long = 0L,
    override val createdAt: Long = System.currentTimeMillis(),
    override var updatedAt: Long = System.currentTimeMillis(),
    // HLS-specific properties
    val quality: String = "default",
    val segmentsTotal: Int = 0,
    val segmentsDownloaded: Int = 0,
    val playlistUrl: String = url,
    val encryption: String? = null
  ) : DownloadItem() {

    override fun copyWith(
      id: String,
      mediaItem: AVPMediaItem,
      url: String,
      fileName: String,
      fileSize: Long,
      downloadedBytes: Long,
      status: DownloadStatus,
      progress: Int,
      localPath: String?,
      downloadSpeed: Long,
      createdAt: Long,
      updatedAt: Long
    ): DownloadItem {
      return copy(
        id = id,
        mediaItem = mediaItem,
        url = url,
        fileName = fileName,
        fileSize = fileSize,
        downloadedBytes = downloadedBytes,
        status = status,
        progress = progress,
        localPath = localPath,
        downloadSpeed = downloadSpeed,
        createdAt = createdAt,
        updatedAt = updatedAt,
        quality = quality,
        segmentsTotal = segmentsTotal,
        segmentsDownloaded = segmentsDownloaded,
        playlistUrl = playlistUrl,
        encryption = encryption
      )
    }

    fun getSegmentProgress(): String {
      return if (segmentsTotal > 0) {
        "$segmentsDownloaded/$segmentsTotal segments"
      } else "Unknown segments"
    }
  }

  // Torrent Downloads
  @Serializable
  data class TorrentDownload(
    override val id: String,
    override val mediaItem: AVPMediaItem,
    override val url: String, // torrent file URL or magnet link
    override val fileName: String,
    override val fileSize: Long = 0L,
    override val downloadedBytes: Long = 0L,
    override val status: DownloadStatus = DownloadStatus.PENDING,
    override val progress: Int = 0,
    override val localPath: String? = null,
    override val downloadSpeed: Long = 0L,
    override val createdAt: Long = System.currentTimeMillis(),
    override var updatedAt: Long = System.currentTimeMillis(),
    // Torrent-specific properties
    val magnetLink: String? = null,
    val torrentFilePath: String? = null,
    val peersConnected: Int = 0,
    val seedsConnected: Int = 0,
    val uploadSpeed: Long = 0L, // bytes per second
    val uploadedBytes: Long = 0L,
    val shareRatio: Float = 0f,
    val pieces: Int = 0,
    val piecesHave: Int = 0,
    val eta: Long = 0L // estimated time to completion in seconds
  ) : DownloadItem() {

    override fun copyWith(
      id: String,
      mediaItem: AVPMediaItem,
      url: String,
      fileName: String,
      fileSize: Long,
      downloadedBytes: Long,
      status: DownloadStatus,
      progress: Int,
      localPath: String?,
      downloadSpeed: Long,
      createdAt: Long,
      updatedAt: Long
    ): DownloadItem {
      return copy(
        id = id,
        mediaItem = mediaItem,
        url = url,
        fileName = fileName,
        fileSize = fileSize,
        downloadedBytes = downloadedBytes,
        status = status,
        progress = progress,
        localPath = localPath,
        downloadSpeed = downloadSpeed,
        createdAt = createdAt,
        updatedAt = updatedAt,
        magnetLink = magnetLink,
        torrentFilePath = torrentFilePath,
        peersConnected = peersConnected,
        seedsConnected = seedsConnected,
        uploadSpeed = uploadSpeed,
        uploadedBytes = uploadedBytes,
        shareRatio = shareRatio,
        pieces = pieces,
        piecesHave = piecesHave,
        eta = eta
      )
    }
    fun isMagnetLink(): Boolean = url.startsWith("magnet:")

    fun getPeerInfo(): String = "$peersConnected peers, $seedsConnected seeds"

    fun getShareRatioFormatted(): String = String.format("%.2f", shareRatio)

    fun getUploadSpeedFormatted(): String {
      if (uploadSpeed <= 0) return "0 B/s"

      val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
      val digitGroups =
        (kotlin.math.log10(uploadSpeed.toDouble()) / kotlin.math.log10(1024.0)).toInt()

      return String.format(
        java.util.Locale.getDefault(),
        "%.1f %s",
        uploadSpeed / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
      )
    }

    fun getPieceProgress(): String {
      return if (pieces > 0) {
        "$piecesHave/$pieces pieces"
      } else "Unknown pieces"
    }
  }

  companion object {
    // Factory methods for creating specific download types
    fun createHttpDownload(
      id: String,
      mediaItem: AVPMediaItem,
      url: String,
      fileName: String,
      connections: Int = 1,
      headers: Map<String, String> = emptyMap()
    ): HttpDownload = HttpDownload(
      id = id,
      mediaItem = mediaItem,
      url = url,
      fileName = fileName,
      connections = connections,
      headers = headers
    )

    fun createHlsDownload(
      id: String,
      mediaItem: AVPMediaItem,
      playlistUrl: String,
      fileName: String,
      quality: String = "default"
    ): HlsDownload = HlsDownload(
      id = id,
      mediaItem = mediaItem,
      url = playlistUrl,
      fileName = fileName,
      quality = quality,
      playlistUrl = playlistUrl
    )

    fun createTorrentDownload(
      id: String, mediaItem: AVPMediaItem, url: String, fileName: String, magnetLink: String? = null
    ): TorrentDownload = TorrentDownload(
      id = id, mediaItem = mediaItem, url = url, fileName = fileName, magnetLink = magnetLink
    )
  }
}

@Serializable
enum class DownloadStatus {
  PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}
