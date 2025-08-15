package cloud.app.vvf.services.downloader

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.app.vvf.R
import cloud.app.vvf.services.downloader.helper.DownloadFileManager
import cloud.app.vvf.services.downloader.helper.DownloadFileManager.Companion.uriToSlug
import cloud.app.vvf.services.downloader.helper.DownloadNotificationManager
import cloud.app.vvf.services.downloader.helper.HttpDownloadClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.math.pow

@HiltWorker
class HlsDownloader @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

  companion object {
    const val KEY_DOWNLOAD_ID = "key_download_id"
    const val KEY_HLS_URL = "key_hls_url"
    const val KEY_FILE_NAME = "key_file_name"
    const val KEY_QUALITY = "key_quality"
  }

  private val notificationManager = DownloadNotificationManager(context)
  private val fileManager = DownloadFileManager(context)

  private val httpClient = HttpDownloadClient()

  @Inject
  lateinit var sharedPreferences: SharedPreferences

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val downloadParams = extractDownloadParams()
    if (!downloadParams.isValid()) {
      return@withContext Result.failure(workDataOf("error" to "Missing required parameters"))
    }

    Timber.d("Starting HLS download: ${downloadParams.downloadId} | ${downloadParams.hlsUrl}")

    val displayName = downloadParams.hlsUrl.uriToSlug()

    // Set foreground with proper downloadId
    if (notificationManager.hasPermission()) {
      setForeground(
        notificationManager.createForegroundInfo(
          downloadParams.downloadId,
          displayName,
          "Processing HLS stream...",
          0
        )
      )
    }

    try {
      Result.success(downloadHlsStream(downloadParams))
    } catch (e: Exception) {
      Timber.e(e, "HLS download failed for ${downloadParams.downloadId}")
      val keys = DownloadData.Companion.Keys
      Result.failure(
        workDataOf(
          keys.ERROR to (e.message ?: "Unknown HLS download error"),
          keys.DOWNLOAD_ID to downloadParams.downloadId
        )
      )
    }
  }

  private suspend fun downloadHlsStream(params: HlsDownloadParams): Data {
    // Parse M3U8 playlist
    val masterPlaylist = fetchPlaylist(params.hlsUrl)
    val selectedVariant = selectBestVariant(masterPlaylist, params.quality, params.hlsUrl)

    // If it's a master playlist, fetch the variant playlist
    val (segmentPlaylist, playlistBaseUrl) = if (selectedVariant != params.hlsUrl) {
      val variantPlaylist = fetchPlaylist(selectedVariant)
      Pair(variantPlaylist, selectedVariant)
    } else {
      Pair(masterPlaylist, params.hlsUrl)
    }

    val segments = parseSegments(segmentPlaylist, playlistBaseUrl)
    val totalSegments = segments.size
    val downloadedBytes = AtomicLong(0)

    // Parse display name from HLS playlist
    val displayName = parseDisplayNameFromHls(masterPlaylist, segmentPlaylist, params.hlsUrl)

    // Create output file using fileManager
    val (outputFile, _) = fileManager.createOrGetFile(
      params.downloadId,
      detectHlsMimeType(segmentPlaylist, segments),
      false,
      0L
    )

    // Pre-calculate total size from all segments (estimate)
    var estimatedTotalBytes = 0L
    val estimationClient = OkHttpClient.Builder()
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

    // Get size of more segments to get better estimation
    val sampleSize = minOf(10, totalSegments) // Sample up to 10 segments or all if less
    val sampleSegments = segments.take(sampleSize)
    var totalSampleSize = 0L
    var successfulSamples = 0

    for (segmentUrl in sampleSegments) {
      try {
        val headRequest = createRequestWithHeaders(segmentUrl).newBuilder().head().build()
        val headResponse = estimationClient.newCall(headRequest).execute()
        if (headResponse.isSuccessful) {
          val contentLength = headResponse.header("Content-Length")?.toLongOrNull() ?: 0L
          if (contentLength > 0) {
            totalSampleSize += contentLength
            successfulSamples++
          }
        }
        headResponse.close()
      } catch (e: Exception) {
        Timber.w(e, "Failed to get segment size for estimation")
      }
    }

    // Estimate total size based on average segment size with safety margin
    estimatedTotalBytes = if (totalSampleSize > 0 && successfulSamples > 0) {
      val averageSegmentSize = totalSampleSize / successfulSamples
      // Add 30% safety margin to account for size variations
      (averageSegmentSize * totalSegments * 1.3).toLong()
    } else {
      // Fallback: assume average segment is 3MB per segment (increased from 2MB)
      totalSegments * 3 * 1024 * 1024L
    }

    Timber.d(
      "Estimated total size: ${formatFileSize(estimatedTotalBytes)} for $totalSegments segments (sampled $successfulSamples segments, avg: ${
        if (successfulSamples > 0) formatFileSize(
          totalSampleSize / successfulSamples
        ) else "unknown"
      })"
    )

    // Speed calculation variables
    val downloadStartTime = System.currentTimeMillis()
    val lastSpeedUpdateTime = AtomicLong(downloadStartTime)
    val lastSpeedUpdateBytes = AtomicLong(0L)
    val speedSamples = mutableListOf<Long>() // Store recent speed samples for smoothing
    val maxSpeedSamples = 5 // Keep last 5 speed samples

    // Use simple client without progress callback to avoid double counting
    val downloadClient = OkHttpClient.Builder()
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

    // Download segments and merge
    outputFile.openOutputStream().use { outputStream ->
      // Download segments in parallel with batch processing
      val batchSize =
        sharedPreferences.getInt(context.getString(R.string.pref_download_batch_size), 3)
      val segmentDataMap =
        mutableMapOf<Int, ByteArray>() // Store segments by index for ordered writing
      val semaphore = Semaphore(batchSize) // Control concurrency

      // Process segments in batches to maintain order
      segments.chunked(batchSize).forEachIndexed { batchIndex, segmentBatch ->
        if (isStopped) {
          throw InterruptedException("Download was stopped")
        }

        // Download segments in current batch concurrently
        val batchJobs = segmentBatch.mapIndexed { indexInBatch: Int, segmentUrl: String ->
          val globalIndex = batchIndex * batchSize + indexInBatch

          withContext(Dispatchers.IO) {
            async {
              semaphore.acquire()
              try {
                val segmentStartTime = System.currentTimeMillis()

                // Download segment
                val request = createRequestWithHeaders(segmentUrl)
                val response = downloadClient.newCall(request).execute()

                if (!response.isSuccessful) {
                  throw IOException("Failed to download segment ${globalIndex + 1}: HTTP ${response.code}: segmentUrl ${segmentUrl}")
                }

                val segmentData = response.body.bytes()
                response.close()

                val segmentEndTime = System.currentTimeMillis()
                downloadedBytes.addAndGet(segmentData.size.toLong())

                // Calculate download speed (thread-safe)
                val currentSpeed = calculateDownloadSpeed(
                  downloadStartTime,
                  lastSpeedUpdateTime.get(),
                  lastSpeedUpdateBytes.get(),
                  downloadedBytes.get(),
                  segmentData.size.toLong(),
                  segmentEndTime - segmentStartTime,
                  speedSamples,
                  maxSpeedSamples
                )

                // Update atomic variables
                lastSpeedUpdateTime.set(segmentEndTime)
                lastSpeedUpdateBytes.set(downloadedBytes.get())

                // Update progress
                updateProgress(
                  params.downloadId,
                  displayName,
                  downloadedBytes.get(),
                  estimatedTotalBytes,
                  currentSpeed,
                  quality = params.quality,
                  segmentsDownloaded = globalIndex + 1,
                  totalSegment = totalSegments
                )

                Timber.d(
                  "Downloaded segment ${globalIndex + 1}/$totalSegments (${segmentData.size} bytes, ${
                    formatSpeed(
                      currentSpeed
                    )
                  }) - Total: ${formatFileSize(downloadedBytes.get())}/${
                    formatFileSize(
                      estimatedTotalBytes
                    )
                  }"
                )

                // Return segment data with its index
                Pair(globalIndex, segmentData)

              } finally {
                semaphore.release()
              }
            }
          }
        }

        // Wait for current batch to complete and write segments in order
        val batchResults: List<Pair<Int, ByteArray>> = batchJobs.map { it.await() }
        batchResults.forEach { (index: Int, data: ByteArray) ->
          segmentDataMap[index] = data
        }

        // Write completed segments to file in order
        val startIndex = batchIndex * batchSize
        val endIndex = minOf(startIndex + batchSize, totalSegments)

        for (i in startIndex until endIndex) {
          segmentDataMap[i]?.let { segmentData ->
            outputStream.write(segmentData)
            outputStream.flush()
            segmentDataMap.remove(i) // Free memory after writing
          }
        }
      }
    }

    val finalSize =
      downloadedBytes.get() // Use actual downloaded bytes instead of outputFile.length()
    val filePath = outputFile.uri.path ?: outputFile.uri.toString()
    notificationManager.showCompletionNotification(params.downloadId, displayName, filePath)

    val keys = DownloadData.Companion.Keys
    return workDataOf(
      keys.PROGRESS to 100,
      keys.DOWNLOADED_BYTES to finalSize,
      keys.TOTAL_BYTES to finalSize, // Use actual file size as total, not estimated
      keys.DOWNLOAD_ID to params.downloadId,
      keys.DISPLAY_NAME to displayName,
      keys.FILE_PATH to filePath,
      keys.FILE_SIZE to finalSize,
      keys.SEGMENTS_DOWNLOADED to totalSegments
    )
  }

  private suspend fun fetchPlaylist(url: String): String = withContext(Dispatchers.IO) {
    // Use simple client for playlist fetching (no progress needed)
    val simpleClient = OkHttpClient.Builder()
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

    val request = createRequestWithHeaders(url)
    val response = simpleClient.newCall(request).execute()

    if (!response.isSuccessful) {
      throw IOException("Failed to fetch playlist: HTTP ${response.code}")
    }

    response.body.string()
  }

  private suspend fun downloadSegmentWithProgress(client: OkHttpClient, url: String): ByteArray =
    withContext(Dispatchers.IO) {
      val request = httpClient.createRequest(url)
      val response = client.newCall(request).execute()

      if (!httpClient.validateResponse(response, false)) {
        throw IOException("Failed to download segment: HTTP ${response.code}")
      }

      response.body.bytes()
    }

  private fun selectBestVariant(masterPlaylist: String, quality: String, baseUrl: String): String {
    val lines = masterPlaylist.lines()
    val variants = mutableListOf<Pair<String, Int>>() // URL to bandwidth

    var i = 0
    while (i < lines.size) {
      val line = lines[i].trim()
      if (line.startsWith("#EXT-X-STREAM-INF:")) {
        val bandwidth = extractBandwidth(line)
        if (i + 1 < lines.size && !lines[i + 1].startsWith("#")) {
          val variantUrl = lines[i + 1].trim()
          // Convert relative URL to absolute URL
          val absoluteUrl = if (variantUrl.startsWith("http")) {
            variantUrl
          } else {
            try {
              URI.create(baseUrl).resolve(variantUrl).toString()
            } catch (e: Exception) {
              Timber.w(e, "Failed to resolve variant URL: $variantUrl")
              continue
            }
          }
          variants.add(Pair(absoluteUrl, bandwidth))
        }
      }
      i++
    }

    if (variants.isEmpty()) {
      // Not a master playlist, return original URL
      return baseUrl
    }

    // Select variant based on quality preference
    return when (quality.lowercase()) {
      "low" -> variants.minByOrNull { it.second }?.first
      "high" -> variants.maxByOrNull { it.second }?.first
      else -> variants.sortedBy { it.second }.getOrNull(variants.size / 2)?.first
    } ?: variants.first().first
  }

  private fun extractBandwidth(streamInfLine: String): Int {
    val bandwidthRegex = "BANDWIDTH=(\\d+)".toRegex()
    return bandwidthRegex.find(streamInfLine)?.groupValues?.get(1)?.toIntOrNull() ?: 0
  }

  private suspend fun parseSegments(playlist: String, baseUrl: String): List<String> {
    val baseUri = try {
      URI.create(baseUrl)
    } catch (e: Exception) {
      Timber.w(e, "Invalid base URL: $baseUrl")
      return emptyList()
    }

    val lines = playlist.lines()
    val segments = mutableListOf<String>()

    // Check if this is a media playlist (contains actual segments) or master playlist (contains sub-playlists)
    val hasMediaSegments = lines.any { line ->
      val trimmed = line.trim()
      !trimmed.startsWith("#") && trimmed.isNotBlank() &&
        (trimmed.endsWith(".ts") || trimmed.endsWith(".m4s") || trimmed.endsWith(".mp4"))
    }

    if (hasMediaSegments) {
      // This is a media playlist - parse actual media segments
      lines.forEach { line ->
        val trimmed = line.trim()
        if (!trimmed.startsWith("#") && trimmed.isNotBlank()) {
          val segmentUrl = resolveUrl(trimmed, baseUri)
          if (segmentUrl != null) {
            segments.add(segmentUrl)
          }
        }
      }
    } else {
      // This might be a master playlist or contains sub-playlists - recursively parse them
      val subPlaylistUrls = mutableListOf<String>()

      lines.forEach { line ->
        val trimmed = line.trim()
        if (!trimmed.startsWith("#") && trimmed.isNotBlank() && trimmed.endsWith(".m3u8")) {
          val subPlaylistUrl = resolveUrl(trimmed, baseUri)
          if (subPlaylistUrl != null) {
            subPlaylistUrls.add(subPlaylistUrl)
          }
        }
      }

      // If we found sub-playlists, fetch and parse them recursively
      if (subPlaylistUrls.isNotEmpty()) {
        for (subPlaylistUrl in subPlaylistUrls) {
          try {
            Timber.d("Fetching sub-playlist: $subPlaylistUrl")
            val subPlaylist = fetchPlaylist(subPlaylistUrl)
            // Use the sub-playlist URL as base for resolving its segments
            val subSegments = parseSegments(subPlaylist, subPlaylistUrl)
            segments.addAll(subSegments)
            Timber.d("Found ${subSegments.size} segments in sub-playlist: $subPlaylistUrl")
          } catch (e: Exception) {
            Timber.w(e, "Failed to fetch sub-playlist: $subPlaylistUrl")
            // Continue with other sub-playlists
          }
        }
      } else {
        // No sub-playlists found, treat as regular segments (fallback)
        lines.forEach { line ->
          val trimmed = line.trim()
          if (!trimmed.startsWith("#") && trimmed.isNotBlank()) {
            val segmentUrl = resolveUrl(trimmed, baseUri)
            if (segmentUrl != null) {
              segments.add(segmentUrl)
            }
          }
        }
      }
    }

    Timber.d("Total segments parsed: ${segments.size} from playlist: $baseUrl")
    return segments
  }

  /**
   * Helper function to properly resolve relative URLs to absolute URLs
   */
  private fun resolveUrl(url: String, baseUri: URI): String? {
    return try {
      if (url.startsWith("http://") || url.startsWith("https://")) {
        // Already absolute URL
        url
      } else {
        // Relative URL - resolve against base URI
        baseUri.resolve(url).toString()
      }
    } catch (e: Exception) {
      Timber.w(e, "Failed to resolve URL: $url with base: $baseUri")
      null
    }
  }

  private fun extractDownloadParams(): HlsDownloadParams {
    return HlsDownloadParams(
      downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: "",
      hlsUrl = inputData.getString(KEY_HLS_URL) ?: "",
      quality = inputData.getString(KEY_QUALITY) ?: "high"
    )
  }

  private var lastProgressUpdateTime = 0L

  private suspend fun updateProgress(
    downloadId: String,
    displayName: String,
    downloadedBytes: Long,
    totalBytes: Long,
    currentSpeed: Long,
    quality: String,
    segmentsDownloaded: Int,
    totalSegment: Int
  ) {
    val now = System.currentTimeMillis()
    if (now - lastProgressUpdateTime < 1000) return // Only update once per second
    lastProgressUpdateTime = now

    val progress = if (totalBytes > 0) {
      ((downloadedBytes * 100) / totalBytes).toInt()
    } else 0

    val keys = DownloadData.Companion.Keys
    setProgressAsync(
      workDataOf(
        keys.DOWNLOAD_ID to downloadId,
        keys.PROGRESS to progress,
        keys.DOWNLOADED_BYTES to downloadedBytes,
        keys.TOTAL_BYTES to totalBytes,
        keys.DISPLAY_NAME to displayName,
        keys.DOWNLOAD_SPEED to currentSpeed,
        keys.QUALITY to quality,
        keys.SEGMENTS_DOWNLOADED to segmentsDownloaded,
        keys.TOTAL_SEGMENTS to totalSegment
      )
    )

    // Update notification using optimized notification manager
    notificationManager.updateNotification(
      this,
      downloadId,
      displayName,
      progress,
      DownloadStatus.DOWNLOADING,
      "Downloading... $progress% • ${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}"
    )
  }

  private suspend fun updateFinalProgress(
    downloadId: String,
    displayName: String,
    filePath: String,
    fileSize: Long
  ) {
    val keys = DownloadData.Companion.Keys
    setProgressAsync(
      workDataOf(
        keys.PROGRESS to 100,
        keys.DOWNLOADED_BYTES to fileSize,
        keys.TOTAL_BYTES to fileSize, // Use actual file size as total, not estimated
        keys.DOWNLOAD_ID to downloadId,
        keys.DISPLAY_NAME to displayName,
        keys.FILE_PATH to filePath,
        keys.FILE_SIZE to fileSize
      )
    )

    // Show completion notification
    notificationManager.showCompletionNotification(downloadId, displayName, filePath)
  }

  private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return String.format(
      java.util.Locale.getDefault(),
      "%.1f %s",
      bytes / 1024.0.pow(digitGroups.toDouble()),
      units[digitGroups]
    )
  }

  /**
   * Parse display name from HLS playlist files
   * Priority: EXT-X-TITLE > filename from URL > segments filename > fallback
   */
  private fun parseDisplayNameFromHls(
    masterPlaylist: String,
    segmentPlaylist: String,
    hlsUrl: String
  ): String {
    // 1. Try to extract title from EXT-X-TITLE or EXT-X-MEDIA tags
    val titleFromPlaylist = extractTitleFromPlaylist(masterPlaylist)
      ?: extractTitleFromPlaylist(segmentPlaylist)

    if (!titleFromPlaylist.isNullOrBlank()) {
      return sanitizeFilename(titleFromPlaylist)
    }

    // 2. Try to extract filename from the main HLS URL
    val filenameFromUrl = extractFilenameFromUrl(hlsUrl)
    if (!filenameFromUrl.isNullOrBlank()) {
      return sanitizeFilename(filenameFromUrl)
    }

    // 3. Try to extract common filename pattern from segments
    val filenameFromSegments = extractFilenameFromSegments(segmentPlaylist)
    if (!filenameFromSegments.isNullOrBlank()) {
      return sanitizeFilename(filenameFromSegments)
    }

    // 4. Fallback to URL slug
    return hlsUrl.uriToSlug().takeIf { it.isNotBlank() }
      ?: "hls_video_${System.currentTimeMillis()}"
  }

  /**
   * Extract title from HLS playlist tags
   */
  private fun extractTitleFromPlaylist(playlist: String): String? {
    val lines = playlist.lines()

    // Look for EXT-X-TITLE tag
    for (line in lines) {
      val trimmedLine = line.trim()
      if (trimmedLine.startsWith("#EXT-X-TITLE:")) {
        val title = trimmedLine.substringAfter("#EXT-X-TITLE:").trim()
        if (title.isNotBlank()) return title
      }
    }

    // Look for EXT-X-MEDIA with NAME attribute
    for (line in lines) {
      val trimmedLine = line.trim()
      if (trimmedLine.startsWith("#EXT-X-MEDIA:")) {
        val nameRegex = "NAME=\"([^\"]+)\"".toRegex()
        val nameMatch = nameRegex.find(trimmedLine)
        if (nameMatch != null) {
          val name = nameMatch.groupValues[1].trim()
          if (name.isNotBlank() && !name.equals("default", ignoreCase = true)) {
            return name
          }
        }
      }
    }

    // Look for comments that might contain title
    for (line in lines) {
      val trimmedLine = line.trim()
      if (trimmedLine.startsWith("#") && !trimmedLine.startsWith("#EXT")) {
        val comment = trimmedLine.substring(1).trim()
        if (comment.isNotBlank() && comment.length > 3 && !comment.contains("Generated")) {
          return comment
        }
      }
    }

    return null
  }

  /**
   * Extract filename from URL path
   */
  private fun extractFilenameFromUrl(url: String): String? {
    return try {
      val uri = URI.create(url)
      val path = uri.path
      if (path.isNullOrBlank()) return null

      val filename = path.substringAfterLast('/')
      if (filename.isBlank()) return null

      // Remove .m3u8 extension and clean up
      val cleanName = filename.removeSuffix(".m3u8").removeSuffix(".M3U8")
      if (cleanName.isNotBlank()) cleanName else null
    } catch (e: Exception) {
      Timber.w(e, "Failed to extract filename from URL: $url")
      null
    }
  }

  /**
   * Extract common filename pattern from segments
   */
  private fun extractFilenameFromSegments(playlist: String): String? {
    val segmentUrls = playlist.lines()
      .filter { !it.startsWith("#") && it.isNotBlank() }
      .take(5) // Only check first 5 segments

    if (segmentUrls.isEmpty()) return null

    // Look for common prefix in segment filenames
    val segmentNames = segmentUrls.mapNotNull { url ->
      try {
        val filename = url.substringAfterLast('/')
        if (filename.contains('.')) {
          // Remove segment number and extension (e.g., video_001.ts -> video)
          filename.replace(Regex("_?\\d+\\.(ts|m4s|mp4)$"), "")
        } else null
      } catch (e: Exception) {
        null
      }
    }.filter { it.isNotBlank() }

    // Find the most common prefix
    val commonPrefix = segmentNames.groupingBy { it }.eachCount()
      .maxByOrNull { it.value }?.key

    return if (!commonPrefix.isNullOrBlank() && commonPrefix.length > 2) {
      commonPrefix
    } else null
  }

  /**
   * Detect MIME type based on HLS playlist and segment information
   */
  private fun detectHlsMimeType(playlist: String, segments: List<String>): String {
    // 1. Check playlist for CODECS information
    val lines = playlist.lines()
    for (line in lines) {
      val trimmedLine = line.trim()
      if (trimmedLine.startsWith("#EXT-X-STREAM-INF:") && trimmedLine.contains("CODECS=")) {
        val codecsRegex = "CODECS=\"([^\"]+)\"".toRegex()
        val codecsMatch = codecsRegex.find(trimmedLine)
        if (codecsMatch != null) {
          val codecs = codecsMatch.groupValues[1].lowercase()
          return when {
            codecs.contains("avc1") || codecs.contains("h264") -> "video/mp4"
            codecs.contains("hev1") || codecs.contains("hvc1") || codecs.contains("h265") -> "video/mp4"
            codecs.contains("vp9") -> "video/webm"
            codecs.contains("vp8") -> "video/webm"
            codecs.contains("av01") -> "video/mp4" // AV1 in MP4
            else -> "video/mp4" // Default for unknown video codecs
          }
        }
      }
    }

    // 2. Check segment file extensions
    if (segments.isNotEmpty()) {
      val segmentExtensions = segments.take(5).map { url ->
        url.substringAfterLast('.').lowercase()
      }.distinct()

      for (extension in segmentExtensions) {
        return when (extension) {
          "ts" -> "video/mp2t" // MPEG-2 Transport Stream
          "m4s", "mp4" -> "video/mp4"
          "webm" -> "video/webm"
          "mkv" -> "video/x-matroska"
          else -> continue
        }
      }
    }

    // 3. Check for audio-only streams
    for (line in lines) {
      val trimmedLine = line.trim()
      if (trimmedLine.startsWith("#EXT-X-STREAM-INF:")) {
        // If no video resolution is specified, might be audio-only
        if (!trimmedLine.contains("RESOLUTION=") && trimmedLine.contains("CODECS=")) {
          val codecsRegex = "CODECS=\"([^\"]+)\"".toRegex()
          val codecsMatch = codecsRegex.find(trimmedLine)
          if (codecsMatch != null) {
            val codecs = codecsMatch.groupValues[1].lowercase()
            if (codecs.contains("mp4a") && !codecs.contains("avc1") && !codecs.contains("hev1")) {
              return "audio/mp4"
            }
          }
        }
      }
    }

    // 4. Default fallback
    return "video/mp4"
  }

  /**
   * Sanitize filename for file system
   */
  private fun sanitizeFilename(filename: String): String {
    return filename
      .replace(Regex("[<>:\"/\\\\|?*]"), "_") // Replace invalid characters
      .replace(Regex("\\s+"), "_") // Replace spaces with underscores
      .replace(Regex("_{2,}"), "_") // Replace multiple underscores with single
      .trim('_') // Remove leading/trailing underscores
      .take(100) // Limit length
      .takeIf { it.isNotBlank() } ?: "hls_video"
  }

  data class HlsDownloadParams(
    val downloadId: String,
    val hlsUrl: String,
    val quality: String
  ) {
    fun isValid(): Boolean = downloadId.isNotEmpty() && hlsUrl.isNotEmpty()
  }

  /**
   * Calculate download speed in bytes per second, with smoothing over recent samples
   */
  private fun calculateDownloadSpeed(
    downloadStartTime: Long,
    lastSpeedUpdateTime: Long,
    lastSpeedUpdateBytes: Long,
    currentDownloadedBytes: Long,
    segmentSize: Long,
    segmentTime: Long,
    speedSamples: MutableList<Long>,
    maxSpeedSamples: Int
  ): Long {
    // Calculate raw speed in bytes per second
    val elapsedTime = System.currentTimeMillis() - downloadStartTime
    val rawSpeed = if (elapsedTime > 0) {
      (currentDownloadedBytes * 1000) / elapsedTime
    } else {
      0L
    }

    // Smooth speed using recent samples
    speedSamples.add(rawSpeed)
    if (speedSamples.size > maxSpeedSamples) {
      speedSamples.removeAt(0)
    }

    val smoothedSpeed = if (speedSamples.isNotEmpty()) {
      speedSamples.sum() / speedSamples.size
    } else {
      rawSpeed
    }

    return smoothedSpeed
  }

  /**
   * Format speed in bytes per second to human-readable string
   */
  private fun formatSpeed(speed: Long): String {
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
    val index = if (speed > 0) {
      (kotlin.math.log10(speed.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    } else {
      0
    }
    val value = speed / Math.pow(1024.0, index.toDouble())

    return String.format(
      java.util.Locale.getDefault(),
      "%.1f %s",
      value,
      units[index.coerceAtMost(units.size - 1)]
    )
  }

  /**
   * Create a request with proper headers to avoid 403 errors
   */
  private fun createRequestWithHeaders(url: String): Request {
    return Request.Builder()
      .url(url)
      .header(
        "User-Agent",
        "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
      )
      .header("Accept", "*/*")
      .header("Accept-Language", "en-US,en;q=0.9")
      .header("Accept-Encoding", "gzip, deflate, br")
      .header("Connection", "keep-alive")
      .header("Cache-Control", "no-cache")
      .header("Pragma", "no-cache")
      .header("Referer", extractRefererFromUrl(url))
      .header("Sec-Fetch-Dest", "empty")
      .header("Sec-Fetch-Mode", "cors")
      .header("Sec-Fetch-Site", "same-origin")
      .build()
  }

  /**
   * Extract a reasonable referer URL from the given URL
   */
  private fun extractRefererFromUrl(url: String): String {
    return try {
      val uri = URI.create(url)
      val scheme = uri.scheme ?: "https"
      val host = uri.host ?: "localhost"
      val port = if (uri.port != -1) ":${uri.port}" else ""
      "$scheme://$host$port/"
    } catch (e: Exception) {
      "https://www.google.com/"
    }
  }
}
