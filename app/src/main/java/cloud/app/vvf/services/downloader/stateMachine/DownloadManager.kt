package cloud.app.vvf.services.downloader.stateMachine

import android.content.Context
import android.os.Environment
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.DownloadStatus
import cloud.app.vvf.services.downloader.DownloadType
import cloud.app.vvf.services.downloader.HlsDownloader
import cloud.app.vvf.services.downloader.HttpDownloader
import cloud.app.vvf.services.downloader.TorrentDownloader
import cloud.app.vvf.utils.KUniFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val appDataStore: MutableStateFlow<AppDataStore>
) {

  // Central controller for download state management
  private val downloadController = DownloadController()

  // Expose downloads from controller
  val downloads: StateFlow<Map<String, DownloadData>> = downloadController.downloads

  // Active downloads computed from controller
  val activeDownloads: StateFlow<Set<String>> = downloadController.downloads
    .map { downloads ->
      downloads.values.filter { it.isActive() }.map { it.id }.toSet()
    }
    .stateIn(
      scope = CoroutineScope(Dispatchers.Default),
      started = SharingStarted.Eagerly,
      initialValue = emptySet()
    )

  init {
    // Clean up old work first
    cleanupOldWork()
    // Load existing downloads and initialize controller
    loadDownloadsFromDataStore()
    // Monitor work manager for download progress updates
    observeWorkManagerUpdates()
    // Setup automatic cleanup of orphaned workers
    setupAutoCleanup()
  }

  /**
   * Clean up old/cancelled work to prevent accumulation
   */
  private fun cleanupOldWork() {
    workManager.cancelAllWorkByTag(DOWNLOAD_WORK_TAG)
    Timber.Forest.d("Cleaned up all previous download work")
  }

  /**
   * Load downloads from AppDataStore and initialize controller
   */
  private fun loadDownloadsFromDataStore() {
    val existingDownloads = appDataStore.value.getAllDownloads() ?: emptyList()
    downloadController.initializeFromPersistedData(existingDownloads)
    Timber.Forest.d("Loaded ${existingDownloads.size} downloads from AppDataStore")
  }

  /**
   * Start downloading media content with automatic type detection
   */
  fun startDownload(
      mediaItem: AVPMediaItem,
      downloadUrl: String,
      quality: String = "default"
  ): String {
    val downloadType = detectDownloadType(downloadUrl)
    return startDownloadWithType(mediaItem, downloadUrl, downloadType, quality)
  }

  /**
   * Detect download type from URL
   */
  private fun detectDownloadType(url: String): DownloadType {
    return when {
      url.contains(".m3u8") || url.contains("m3u8") -> DownloadType.HLS
      url.startsWith("magnet:") -> DownloadType.TORRENT
      url.endsWith(".torrent") -> DownloadType.TORRENT
      else -> DownloadType.HTTP
    }
  }

  /**
   * Internal method to start download with specific type
   */
  private fun startDownloadWithType(
    mediaItem: AVPMediaItem,
    downloadUrl: String,
    downloadType: DownloadType,
    quality: String = "default"
  ): String {
    val downloadId = generateDownloadId(mediaItem, downloadUrl)
    val fileName = generateFileName(mediaItem, quality)

    // Check if download already exists
    val existingDownload = downloads.value[downloadId]
    if (existingDownload != null) {
      when (existingDownload.status) {
        DownloadStatus.DOWNLOADING, DownloadStatus.PENDING -> {
          Timber.Forest.d("Download already in progress: $downloadId")
          return downloadId
        }

        DownloadStatus.COMPLETED -> {
          Timber.Forest.d("Download already completed: $downloadId")
          return downloadId
        }

        DownloadStatus.PAUSED -> {
          Timber.Forest.d("Resuming existing paused download: $downloadId")
          resumeDownload(downloadId)
          return downloadId
        }

        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
          Timber.Forest.d("Retrying existing failed/cancelled download: $downloadId")
          // Continue with creating new download to retry
        }
      }
    }

    // Create new download data based on type
    val downloadData = when (downloadType) {
      DownloadType.HTTP -> DownloadData.Companion.DownloadDataBuilder()
        .id(downloadId)
        .mediaItem(mediaItem)
        .url(downloadUrl)
        .displayName(fileName)
        .type(DownloadType.HTTP)
        .status(DownloadStatus.PENDING)
        .build()

      DownloadType.HLS -> DownloadData.Companion.DownloadDataBuilder()
        .id(downloadId)
        .mediaItem(mediaItem)
        .url(downloadUrl)
        .displayName(fileName)
        .type(DownloadType.HLS)
        .quality(quality)
        .status(DownloadStatus.PENDING)
        .build()

      DownloadType.TORRENT -> DownloadData.Companion.DownloadDataBuilder()
        .id(downloadId)
        .mediaItem(mediaItem)
        .url(downloadUrl)
        .displayName(fileName)
        .type(DownloadType.TORRENT)
        .status(DownloadStatus.PENDING)
        .apply {
          if (downloadUrl.startsWith("magnet:")) {
            magnetLink(downloadUrl)
          }
        }
        .build()
    }

    // Add to controller and persist
    downloadController.addDownloadData(downloadData)
    appDataStore.value.saveDownload(downloadData)

    // Execute start command
    val command = DownloadCommand.Start(downloadId, downloadUrl)
    if (downloadController.executeCommand(command)) {
      // Create and enqueue WorkManager request
      enqueueWorkRequest(downloadId, downloadType, downloadUrl, quality)
      Timber.Forest.d("Started ${downloadType.name} download: $downloadId")
    } else {
      Timber.Forest.e("Failed to start download: $downloadId")
    }

    return downloadId
  }

  /**
   * Pause a download
   */
  fun pauseDownload(downloadId: String) {
    Timber.Forest.d("DownloadManager.pauseDownload called for: $downloadId")

    if (!downloadController.canPause(downloadId)) {
      Timber.Forest.w("DownloadManager: Cannot pause download: $downloadId")
      return
    }

    val command = DownloadCommand.Pause(downloadId)
    if (downloadController.executeCommand(command)) {
      // Cancel WorkManager task
      workManager.cancelUniqueWork("download_$downloadId")
      Timber.Forest.d("DownloadManager: Cancelled WorkManager task for: $downloadId")
    } else {
      Timber.Forest.e("DownloadManager: Failed to execute Pause command for: $downloadId")
    }
  }

  /**
   * Resume a paused download
   */
  fun resumeDownload(downloadId: String) {
    if (!downloadController.canResume(downloadId)) {
      Timber.Forest.w("Cannot resume download: $downloadId")
      return
    }

    val downloadData = downloads.value[downloadId] ?: return

    // Calculate actual downloaded bytes from file if it exists
    val actualDownloadedBytes = getActualDownloadedBytes(downloadData.title ?: "")

    // Update download data with actual file size if different
    if (actualDownloadedBytes != downloadData.downloadedBytes && actualDownloadedBytes > 0) {
      val updatedData = downloadData.copy(
        downloadedBytes = actualDownloadedBytes,
        progress = if (downloadData.totalBytes > 0) {
          ((actualDownloadedBytes * 100) / downloadData.totalBytes).toInt()
        } else downloadData.progress,
        updatedAt = System.currentTimeMillis()
      )

      // Save updated data to datastore
      appDataStore.value.saveDownload(updatedData)
      Timber.Forest.d("Updated download $downloadId with actual downloaded bytes: $actualDownloadedBytes")
    }

    val command = DownloadCommand.Resume(downloadId)

    if (downloadController.executeCommand(command)) {
      // Re-enqueue work request with resume parameters
      val downloadType = detectDownloadType(downloadData.url)
      val finalDownloadedBytes =
        if (actualDownloadedBytes > 0) actualDownloadedBytes else downloadData.downloadedBytes

      enqueueWorkRequest(
        downloadId = downloadId,
        downloadType = downloadType,
        downloadUrl = downloadData.url,
        quality = downloadData.quality,
        resumeBytes = finalDownloadedBytes,
        resumeProgress = downloadData.progress,
        isResuming = true
      )
      Timber.Forest.d("Resumed download: $downloadId from ${finalDownloadedBytes} bytes (${downloadData.progress}%)")
    }
  }

  /**
   * Get actual downloaded bytes from file system
   */
  private fun getActualDownloadedBytes(fileName: String): Long {
    return try {
      val downloadsDir = getDownloadsDirectory()
      val file = downloadsDir.findFile(fileName)
      file?.length() ?: 0L
    } catch (e: Exception) {
      Timber.Forest.w(e, "Failed to get actual downloaded bytes for: $fileName")
      0L
    }
  }

  /**
   * Get downloads directory
   */
  private fun getDownloadsDirectory(): KUniFile {
    return try {
      val downloadsFolder = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        "VividFusion"
      )
      if (!downloadsFolder.exists()) {
        downloadsFolder.mkdirs()
      }
      KUniFile.Companion.fromFile(context, downloadsFolder)
        ?: throw Exception("Failed to create KUniFile")
    } catch (e: Exception) {
      Timber.Forest.e(e, "Failed to get downloads directory")
      throw e
    }
  }

  /**
   * Cancel and remove a download completely
   */
  fun cancelDownload(downloadId: String) {
    Timber.d("DownloadManager.cancelDownload called for: $downloadId")

    // Cancel WorkManager task first
    workManager.cancelUniqueWork("download_$downloadId")
    Timber.d("Cancelled WorkManager task for: $downloadId")

    // Execute cancel command through controller
    val command = DownloadCommand.Cancel(downloadId)
    downloadController.executeCommand(command)

    // Remove from datastore
    appDataStore.value.removeDownload(downloadId)
    Timber.d("Removed download from datastore: $downloadId")
  }

  /**
   * Remove a download completely (for completed/failed downloads)
   */
  fun removeDownload(downloadId: String) {
    Timber.d("DownloadManager.removeDownload called for: $downloadId")

    // Cancel any running work
    workManager.cancelUniqueWork("download_$downloadId")

    // Remove from controller
    downloadController.removeDownload(downloadId)

    // Remove from datastore
    appDataStore.value.removeDownload(downloadId)
    Timber.d("Removed download: $downloadId")
  }

  /**
   * Clean up orphaned workers that don't have corresponding downloads in UI
   */
  fun cleanupOrphanedWorkers() {
    Timber.d("Starting cleanup of orphaned workers")

    // Get all active download work
    workManager.getWorkInfosByTag(DOWNLOAD_WORK_TAG).get()?.let { workInfos ->
      val activeWorkDownloadIds = workInfos.mapNotNull { workInfo ->
        extractDownloadId(workInfo)
      }.toSet()

      val currentDownloadIds = downloads.value.keys.toSet()

      // Find orphaned workers (workers that don't have corresponding downloads)
      val orphanedWorkers = activeWorkDownloadIds - currentDownloadIds

      if (orphanedWorkers.isNotEmpty()) {
        Timber.d("Found ${orphanedWorkers.size} orphaned workers: $orphanedWorkers")

        orphanedWorkers.forEach { orphanedDownloadId ->
          Timber.d("Cleaning up orphaned worker: $orphanedDownloadId")
          workManager.cancelUniqueWork("download_$orphanedDownloadId")
        }
      } else {
        Timber.d("No orphaned workers found")
      }
    }
  }

  /**
   * Periodic cleanup of orphaned workers - call this periodically or when UI state changes
   */
  fun scheduleOrphanedWorkerCleanup() {
    // Clean up immediately
    cleanupOrphanedWorkers()

    // You can also schedule periodic cleanup if needed
    // For now, we'll rely on manual calls when UI state changes
  }

  /**
   * Setup automatic cleanup of orphaned workers when download state changes
   */
  private fun setupAutoCleanup() {
    // Use a debounced approach to avoid too frequent cleanup calls
    var lastCleanupTime = 0L
    val cleanupDelay = 5000L // 5 seconds minimum between cleanups

    // Monitor downloads state flow for changes
    CoroutineScope(Dispatchers.IO).launch {
      downloads.collect { currentDownloads ->
        val currentTime = System.currentTimeMillis()

        // Only cleanup if enough time has passed since last cleanup
        if (currentTime - lastCleanupTime > cleanupDelay) {
          lastCleanupTime = currentTime
          cleanupOrphanedWorkers()
        }
      }
    }

    Timber.d("Setup automatic orphaned worker cleanup with ${cleanupDelay}ms debounce")
  }

  /**
   * Enqueue WorkManager request
   */
  private fun enqueueWorkRequest(
    downloadId: String,
    downloadType: DownloadType,
    downloadUrl: String,
    quality: String,
    resumeBytes: Long = 0L,
    resumeProgress: Int = 0,
    isResuming: Boolean = false
  ) {
    val workRequest = when (downloadType) {
      DownloadType.HTTP -> {
        OneTimeWorkRequestBuilder<HttpDownloader>()
          .setInputData(
              workDataOf(
                  HttpDownloader.Companion.KEY_DOWNLOAD_ID to downloadId,
                  HttpDownloader.Companion.KEY_DOWNLOAD_URL to downloadUrl,
                  HttpDownloader.Companion.KEY_DOWNLOAD_TYPE to "HTTP",
                  HttpDownloader.Companion.KEY_RESUME_PROGRESS to resumeProgress,
                  HttpDownloader.Companion.KEY_RESUME_BYTES to resumeBytes,
                  HttpDownloader.Companion.KEY_RESUME_FROM_PAUSE to isResuming
              )
          )
          .addTag(DOWNLOAD_WORK_TAG)
          .addTag(downloadId)
          .addTag("HTTP")
          .build()
      }

      DownloadType.HLS -> {
        OneTimeWorkRequestBuilder<HlsDownloader>()
          .setInputData(
              workDataOf(
                  HlsDownloader.Companion.KEY_DOWNLOAD_ID to downloadId,
                  HlsDownloader.Companion.KEY_HLS_URL to downloadUrl,
                  HlsDownloader.Companion.KEY_QUALITY to quality
              )
          )
          .addTag(DOWNLOAD_WORK_TAG)
          .addTag(downloadId)
          .addTag("HLS")
          .build()
      }

      DownloadType.TORRENT -> {
        OneTimeWorkRequestBuilder<TorrentDownloader>()
          .setInputData(
              workDataOf(
                  TorrentDownloader.Companion.KEY_DOWNLOAD_ID to downloadId,
                  TorrentDownloader.Companion.KEY_TORRENT_URL to downloadUrl,
              )
          )
          .addTag(DOWNLOAD_WORK_TAG)
          .addTag(downloadId)
          .addTag("TORRENT")
          .build()
      }
    }

    workManager.enqueueUniqueWork(
      "download_$downloadId",
      ExistingWorkPolicy.REPLACE,
      workRequest
    )
  }

  /**
   * Observe WorkManager updates and convert to events
   */
  private fun observeWorkManagerUpdates() {
    Timber.Forest.d("Setting up WorkManager observer")
    workManager.getWorkInfosByTagLiveData(DOWNLOAD_WORK_TAG).observeForever { workInfos ->
      Timber.Forest.d("WorkManager update received: ${workInfos.size} items")
      workInfos?.forEach { workInfo ->
        val downloadId = extractDownloadId(workInfo)
        if (downloadId != null) {
          Timber.Forest.d("WorkManager update for download: $downloadId")
          Timber.Forest.d("WorkManager update for workInfo: ${workInfo.id}")
          val event = workInfo.toDownloadEvent(downloadId)
          if (event != null) {
            handleWorkEvent(event)
          }
        }
      }
    }
  }

  /**
   * Handle WorkManager events through controller
   */
  private fun handleWorkEvent(event: DownloadEvent) {
    if (downloadController.handleWorkEvent(event)) {
      // Sync to persistent storage if needed
      val downloadId = when (event) {
        is DownloadEvent.WorkEnqueued -> event.downloadId
        is DownloadEvent.WorkStarted -> event.downloadId
        is DownloadEvent.ProgressUpdated -> event.downloadId
        is DownloadEvent.WorkCompleted -> event.downloadId
        is DownloadEvent.WorkFailed -> event.downloadId
        is DownloadEvent.WorkCancelled -> event.downloadId
      }

      downloads.value[downloadId]?.let { downloadItem ->
        appDataStore.value.saveDownload(downloadItem)
      }
    }
  }

  /**
   * Extract download ID from WorkInfo
   */
  private fun extractDownloadId(workInfo: WorkInfo): String? {
    // Try progress data first
    workInfo.progress.getString("downloadId")?.let { return it }

    // Try tags
    return workInfo.tags.find { tag ->
      tag != DOWNLOAD_WORK_TAG &&
        !tag.matches(Regex("(HLS|TORRENT|MAGNET|HTTP)")) &&
        !tag.contains("cloud.app.vvf") &&
        !tag.contains(".") &&
        tag.isNotEmpty() &&
        tag.length > 10
    }
  }

  // Utility methods remain the same
  private fun generateDownloadId(mediaItem: AVPMediaItem, downloadUrl: String): String {
    // Create deterministic ID based on media item and URL to prevent duplicates
    val mediaId = when (mediaItem) {
      is AVPMediaItem.MovieItem -> "movie-${mediaItem.id}"
      is AVPMediaItem.EpisodeItem -> "episode-${mediaItem.id}"
      is AVPMediaItem.VideoItem -> "video-${mediaItem.id}"
      is AVPMediaItem.TrackItem -> "track-${mediaItem.id}"
      else -> "media-${mediaItem.title}-${mediaItem.hashCode()}"
    }

    // Clean the media ID and make it more deterministic
    val cleanMediaId = mediaId
      .replace("[^a-zA-Z0-9\\s-]".toRegex(), "") // Allow hyphens
      .replace("\\s+".toRegex(), "-")
      .lowercase()
      .take(50) // Limit length

    // Use URL hash to ensure uniqueness while being deterministic
    // Normalize URL to prevent different representations of same URL creating different IDs
    val normalizedUrl = downloadUrl.trim()
      .lowercase()
      .replace(Regex("&timestamp=\\d+"), "") // Remove timestamps from URL
      .replace(Regex("[?&]t=\\d+"), "") // Remove time parameters
      .replace(Regex("\\s+"), "") // Remove whitespace

    // Use a stable hash function that creates consistent IDs
    val urlHash = normalizedUrl.hashCode().let { hash ->
      // Ensure positive hash and consistent format
      (if (hash < 0) -hash else hash).toString().takeLast(8)
    }

    val finalId = "${cleanMediaId}-${urlHash}"

    Timber.Forest.d("Generated deterministic downloadId: $finalId for URL: ${downloadUrl.take(50)}...")
    return finalId
  }

  private fun generateFileName(mediaItem: AVPMediaItem, quality: String): String {
    val baseName = when (mediaItem) {
      is AVPMediaItem.MovieItem -> {
        "${mediaItem.movie.generalInfo.title} (${mediaItem.releaseYear ?: "Unknown"})"
      }

      is AVPMediaItem.EpisodeItem -> {
        val show = mediaItem.seasonItem.showItem.show.generalInfo.title
        val season = mediaItem.seasonItem.season.number
        val episode = mediaItem.episode.episodeNumber
        "${show} - S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
      }

      is AVPMediaItem.VideoItem -> {
        mediaItem.video.title
      }

      is AVPMediaItem.TrackItem -> {
        mediaItem.track.title
      }

      else -> "Media_${System.currentTimeMillis()}"
    }

    // Clean filename for filesystem compatibility
    val cleanName = baseName
      ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")
      ?.take(100) // Limit length

    return if (quality != "default") "${cleanName}_${quality}" else cleanName
      ?: "download_${System.currentTimeMillis()}"
  }


  /**
   * Retry a failed download
   */
  fun retryDownload(downloadId: String) {
    Timber.d("DownloadManager.retryDownload called for: $downloadId")

    val downloadData = downloads.value[downloadId] ?: return

    // Cancel any existing work first
    workManager.cancelUniqueWork("download_$downloadId")

    // Execute start command to retry
    val command = DownloadCommand.Start(downloadId, downloadData.url)
    if (downloadController.executeCommand(command)) {
      // Create and enqueue WorkManager request
      val downloadType = detectDownloadType(downloadData.url)
      enqueueWorkRequest(downloadId, downloadType, downloadData.url, downloadData.quality)
      Timber.d("Retried ${downloadType.name} download: $downloadId")
    } else {
      Timber.e("Failed to retry download: $downloadId")
    }
  }

  /**
   * Get download progress for a specific download
   */
  fun getDownloadProgress(downloadId: String): StateFlow<Int> {
    return downloads.map { downloadsMap ->
      downloadsMap[downloadId]?.progress ?: 0
    }.stateIn(
      scope = CoroutineScope(Dispatchers.Default),
      started = SharingStarted.Eagerly,
      initialValue = 0
    )
  }

  /**
   * Get download data for a specific download
   */
  fun getDownloadData(downloadId: String): StateFlow<DownloadData?> {
    return downloads.map { downloadsMap ->
      downloadsMap[downloadId]
    }.stateIn(
      scope = CoroutineScope(Dispatchers.Default),
      started = SharingStarted.Eagerly,
      initialValue = null
    )
  }

  companion object {
    private const val DOWNLOAD_WORK_TAG = "media_download"
  }
}
