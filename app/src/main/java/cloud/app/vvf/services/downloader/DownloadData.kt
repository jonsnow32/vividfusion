package cloud.app.vvf.services.downloader

import cloud.app.vvf.common.models.AVPMediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.util.Locale


/**
 * Unified download data container that replaces DownloadItem
 * This serves as both state machine data and UI data model
 */
@Serializable
data class DownloadData(
  // Identification
  val id: String = "",
  val mediaItem: AVPMediaItem? = null,
  val url: String = "",
  val fileName: String? = null,
  val localPath: String? = null,

  // Common fields for all downloaders
  val progress: Int = 0,
  val downloadedBytes: Long = 0L,
  val totalBytes: Long = 0L,
  val downloadSpeed: Long = 0L,
  val status: DownloadStatus = DownloadStatus.PENDING,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),

  // Download type
  val type: DownloadType = DownloadType.HTTP,

  // Type-specific data stored as JsonElement for serialization compatibility
  val typeSpecificData: Map<String, JsonElement> = emptyMap()
) {
  companion object {
    // Keys for type-specific data
    object Keys {
      // Torrent-specific
      const val UPLOAD_SPEED = "uploadSpeed"
      const val PEERS = "peers"
      const val SEEDS = "seeds"
      const val TOTAL_PEERS = "totalPeers"
      const val SHARE_RATIO = "shareRatio"
      const val TORRENT_STATE = "torrentState"
      const val BYTES_READ = "bytesRead"
      const val BYTES_WRITTEN = "bytesWritten"
      const val PRELOADED_BYTES = "preloadedBytes"
      const val ETA = "eta"
      const val MAGNET_LINK = "magnetLink"
      const val TORRENT_FILE_PATH = "torrentFilePath"

      // HLS-specific
      const val SEGMENTS_DOWNLOADED = "segmentsDownloaded"
      const val TOTAL_SEGMENTS = "totalSegments"
      const val QUALITY = "quality"
      const val ENCRYPTION = "encryption"
      const val PLAYLIST_URL = "playlistUrl"

      // HTTP-specific
      const val CONNECTIONS = "connections"
      const val RESUME_SUPPORTED = "resumeSupported"
      const val CONTENT_TYPE = "contentType"
      const val HEADERS = "headers"

      // Common
      const val STREAM_URL = "streamUrl"
      const val NOTE = "note"
    }

    /**
     * Builder for creating DownloadData with type-specific data
     */
    class DownloadDataBuilder {
      private val typeSpecificData = mutableMapOf<String, JsonElement>()
      private var progress: Int = 0
      private var downloadedBytes: Long = 0L
      private var totalBytes: Long = 0L
      private var downloadSpeed: Long = 0L
      private var fileName: String? = null
      private var id: String = ""
      private var mediaItem: AVPMediaItem? = null
      private var url: String = ""
      private var localPath: String? = null
      private var status: DownloadStatus = DownloadStatus.PENDING
      private var createdAt: Long = System.currentTimeMillis()
      private var updatedAt: Long = System.currentTimeMillis()
      private var type: DownloadType = DownloadType.HTTP

      fun progress(progress: Int) = apply { this.progress = progress }
      fun downloadedBytes(bytes: Long) = apply { this.downloadedBytes = bytes }
      fun totalBytes(bytes: Long) = apply { this.totalBytes = bytes }
      fun downloadSpeed(speed: Long) = apply { this.downloadSpeed = speed }
      fun fileName(name: String?) = apply { this.fileName = name }
      fun id(id: String) = apply { this.id = id }
      fun mediaItem(mediaItem: AVPMediaItem?) = apply { this.mediaItem = mediaItem }
      fun url(url: String) = apply { this.url = url }
      fun localPath(path: String?) = apply { this.localPath = path }
      fun status(status: DownloadStatus) = apply { this.status = status }
      fun createdAt(timestamp: Long) = apply { this.createdAt = timestamp }
      fun updatedAt(timestamp: Long) = apply { this.updatedAt = timestamp }
      fun type(type: DownloadType) = apply { this.type = type }

      // Torrent-specific builders
      fun uploadSpeed(speed: Long) =
        apply { typeSpecificData[Keys.UPLOAD_SPEED] = JsonPrimitive(speed) }

      fun peers(count: Int) = apply { typeSpecificData[Keys.PEERS] = JsonPrimitive(count) }
      fun seeds(count: Int) = apply { typeSpecificData[Keys.SEEDS] = JsonPrimitive(count) }
      fun totalPeers(count: Int) =
        apply { typeSpecificData[Keys.TOTAL_PEERS] = JsonPrimitive(count) }

      fun shareRatio(ratio: Float) =
        apply { typeSpecificData[Keys.SHARE_RATIO] = JsonPrimitive(ratio) }

      fun torrentState(state: String) =
        apply { typeSpecificData[Keys.TORRENT_STATE] = JsonPrimitive(state) }

      fun bytesRead(bytes: Long) =
        apply { typeSpecificData[Keys.BYTES_READ] = JsonPrimitive(bytes) }

      fun bytesWritten(bytes: Long) =
        apply { typeSpecificData[Keys.BYTES_WRITTEN] = JsonPrimitive(bytes) }

      fun preloadedBytes(bytes: Long) =
        apply { typeSpecificData[Keys.PRELOADED_BYTES] = JsonPrimitive(bytes) }

      fun eta(seconds: Long) = apply { typeSpecificData[Keys.ETA] = JsonPrimitive(seconds) }
      fun magnetLink(link: String) =
        apply { typeSpecificData[Keys.MAGNET_LINK] = JsonPrimitive(link) }

      fun torrentFilePath(path: String) =
        apply { typeSpecificData[Keys.TORRENT_FILE_PATH] = JsonPrimitive(path) }

      // HLS-specific builders
      fun segmentsDownloaded(count: Int) =
        apply { typeSpecificData[Keys.SEGMENTS_DOWNLOADED] = JsonPrimitive(count) }

      fun totalSegments(count: Int) =
        apply { typeSpecificData[Keys.TOTAL_SEGMENTS] = JsonPrimitive(count) }

      fun quality(quality: String) =
        apply { typeSpecificData[Keys.QUALITY] = JsonPrimitive(quality) }

      fun encryption(encryption: String?) =
        apply { encryption?.let { typeSpecificData[Keys.ENCRYPTION] = JsonPrimitive(it) } }

      fun playlistUrl(url: String) =
        apply { typeSpecificData[Keys.PLAYLIST_URL] = JsonPrimitive(url) }

      // HTTP-specific builders
      fun connections(count: Int) =
        apply { typeSpecificData[Keys.CONNECTIONS] = JsonPrimitive(count) }

      fun resumeSupported(supported: Boolean) =
        apply { typeSpecificData[Keys.RESUME_SUPPORTED] = JsonPrimitive(supported) }

      fun contentType(type: String?) =
        apply { type?.let { typeSpecificData[Keys.CONTENT_TYPE] = JsonPrimitive(it) } }

      // Common builders
      fun streamUrl(url: String?) =
        apply { url?.let { typeSpecificData[Keys.STREAM_URL] = JsonPrimitive(it) } }

      fun note(note: String?) =
        apply { note?.let { typeSpecificData[Keys.NOTE] = JsonPrimitive(it) } }

      fun build(): DownloadData = DownloadData(
        id = id,
        mediaItem = mediaItem,
        url = url,
        fileName = fileName,
        localPath = localPath,
        progress = progress,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        downloadSpeed = downloadSpeed,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        type = type,
        typeSpecificData = typeSpecificData.toMap()
      )
    }
  }

  // Helper methods for type detection
  val isTorrentDownload: Boolean get() = type == DownloadType.TORRENT
  val isHlsDownload: Boolean get() = type == DownloadType.HLS
  val isHttpDownload: Boolean get() = type == DownloadType.HTTP

  // Type-safe getters for torrent data
  val uploadSpeed: Long
    get() = (typeSpecificData[Keys.UPLOAD_SPEED] as? JsonPrimitive)?.longOrNull ?: 0L
  val peers: Int get() = (typeSpecificData[Keys.PEERS] as? JsonPrimitive)?.intOrNull ?: 0
  val seeds: Int get() = (typeSpecificData[Keys.SEEDS] as? JsonPrimitive)?.intOrNull ?: 0
  val totalPeers: Int get() = (typeSpecificData[Keys.TOTAL_PEERS] as? JsonPrimitive)?.intOrNull ?: 0
  val shareRatio: Float
    get() = (typeSpecificData[Keys.SHARE_RATIO] as? JsonPrimitive)?.floatOrNull ?: 0f
  val torrentState: String
    get() = (typeSpecificData[Keys.TORRENT_STATE] as? JsonPrimitive)?.contentOrNull ?: ""
  val bytesRead: Long
    get() = (typeSpecificData[Keys.BYTES_READ] as? JsonPrimitive)?.longOrNull ?: 0L
  val bytesWritten: Long
    get() = (typeSpecificData[Keys.BYTES_WRITTEN] as? JsonPrimitive)?.longOrNull ?: 0L
  val preloadedBytes: Long
    get() = (typeSpecificData[Keys.PRELOADED_BYTES] as? JsonPrimitive)?.longOrNull ?: 0L
  val eta: Long get() = (typeSpecificData[Keys.ETA] as? JsonPrimitive)?.longOrNull ?: 0L
  val magnetLink: String? get() = (typeSpecificData[Keys.MAGNET_LINK] as? JsonPrimitive)?.contentOrNull
  val torrentFilePath: String? get() = (typeSpecificData[Keys.TORRENT_FILE_PATH] as? JsonPrimitive)?.contentOrNull

  // Type-safe getters for HLS data
  val segmentsDownloaded: Int
    get() = (typeSpecificData[Keys.SEGMENTS_DOWNLOADED] as? JsonPrimitive)?.intOrNull ?: 0
  val totalSegments: Int
    get() = (typeSpecificData[Keys.TOTAL_SEGMENTS] as? JsonPrimitive)?.intOrNull ?: 0
  val quality: String
    get() = (typeSpecificData[Keys.QUALITY] as? JsonPrimitive)?.contentOrNull ?: ""
  val encryption: String? get() = (typeSpecificData[Keys.ENCRYPTION] as? JsonPrimitive)?.contentOrNull
  val playlistUrl: String? get() = (typeSpecificData[Keys.PLAYLIST_URL] as? JsonPrimitive)?.contentOrNull

  // Type-safe getters for HTTP data
  val connections: Int
    get() = (typeSpecificData[Keys.CONNECTIONS] as? JsonPrimitive)?.intOrNull ?: 0
  val resumeSupported: Boolean
    get() = (typeSpecificData[Keys.RESUME_SUPPORTED] as? JsonPrimitive)?.booleanOrNull ?: false
  val contentType: String? get() = (typeSpecificData[Keys.CONTENT_TYPE] as? JsonPrimitive)?.contentOrNull
  val headers: Map<String, String> get() = emptyMap() // TODO: Implement if needed

  // Common getters
  val streamUrl: String? get() = (typeSpecificData[Keys.STREAM_URL] as? JsonPrimitive)?.contentOrNull
  val note: String? get() = (typeSpecificData[Keys.NOTE] as? JsonPrimitive)?.contentOrNull

  // Formatting helpers for UI
  val progressPercent: Int
    get() = (if (totalBytes > 0) {
      (downloadedBytes * 100f / totalBytes)
    } else {
      progress
    }).toInt()

  val downloadSpeedFormatted: String get() = formatBytes(downloadSpeed) + "/s"
  val uploadSpeedFormatted: String get() = formatBytes(uploadSpeed) + "/s"
  val downloadedSizeFormatted: String get() = formatBytes(downloadedBytes)
  val totalSizeFormatted: String get() = formatBytes(totalBytes)

  // Status helpers
  fun isCompleted(): Boolean = status == DownloadStatus.COMPLETED
  fun canRetry(): Boolean = status in listOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED)
  fun isActive(): Boolean = status in listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PENDING)

  // Type-specific helper functions
  fun getSegmentProgress(): String {
    return if (totalSegments > 0) {
      "$segmentsDownloaded/$totalSegments segments"
    } else "Unknown segments"
  }

  fun getPeerInfo(): String {
    return "$peers peers, $seeds seeds"
  }

  fun getShareRatioFormatted(): String {
    return String.format(Locale.getDefault(), "%.2f", shareRatio)
  }

  fun getEtaFormatted(): String {
    if (eta <= 0) return "Unknown"

    return when {
      eta < 60 -> "${eta}s"
      eta < 3600 -> "${eta / 60}m ${eta % 60}s"
      else -> "${eta / 3600}h ${(eta % 3600) / 60}m"
    }
  }

  fun getEstimatedTimeRemaining(): String {
    if (downloadSpeed <= 0 || totalBytes <= downloadedBytes) return "Unknown"

    val remainingBytes = totalBytes - downloadedBytes
    val remainingSeconds = remainingBytes / downloadSpeed

    return when {
      remainingSeconds < 60 -> "${remainingSeconds}s"
      remainingSeconds < 3600 -> "${remainingSeconds / 60}m ${remainingSeconds % 60}s"
      else -> "${remainingSeconds / 3600}h ${(remainingSeconds % 3600) / 60}m"
    }
  }

  private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
      size /= 1024
      unitIndex++
    }

    return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex])
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


@Serializable
enum class DownloadType {
  HTTP, HLS, TORRENT
}
