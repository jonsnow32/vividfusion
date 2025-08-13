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
   * Cancel an active download
   */
  fun cancelDownload(downloadId: String) {
    val command = DownloadCommand.Cancel(downloadId)
    if (downloadController.executeCommand(command)) {
      workManager.cancelUniqueWork("download_$downloadId")
      Timber.Forest.d("Cancelled download: $downloadId")
    }
  }

  /**
   * Remove a download from the list
   */
  fun removeDownload(downloadId: String) {
    val command = DownloadCommand.Remove(downloadId)
    if (downloadController.executeCommand(command)) {
      workManager.cancelUniqueWork("download_$downloadId")
      appDataStore.value.removeDownload(downloadId)
      downloadController.removeDownload(downloadId)
      Timber.Forest.d("Removed download: $downloadId")
    }
  }

  /**
   * Retry a failed download
   */
  fun retryDownload(downloadId: String) {
    val downloadData = downloads.value[downloadId] ?: return

    if (downloadData.status == DownloadStatus.FAILED || downloadData.status == DownloadStatus.CANCELLED) {
      // Start a new download for retry
      downloadData.mediaItem?.let { mediaItem ->
        startDownload(mediaItem, downloadData.url)
      }
      Timber.Forest.d("Retrying download: $downloadId")
    } else {
      Timber.Forest.w("Cannot retry download $downloadId: current status is ${downloadData.status}")
    }
  }

  /**
   * Get download progress as a flow for a specific download
   */
  fun getDownloadProgress(downloadId: String): Flow<Int> {
    return downloads.map { downloadMap ->
      downloadMap[downloadId]?.progress ?: 0
    }
  }

  /**
   * Get all downloads for a specific media item
   */
  fun getDownloadsForMediaItem(mediaItem: AVPMediaItem): List<DownloadData> {
    return downloads.value.values.filter { it.mediaItem?.id == mediaItem.id }
  }

  /**
   * Check if a media item is currently being downloaded
   */
  fun isDownloading(mediaItem: AVPMediaItem): Boolean {
    return downloads.value.values.any {
      it.mediaItem?.id == mediaItem.id && it.isActive()
    }
  }

  /**
   * Check if a media item has been downloaded
   */
  fun isDownloaded(mediaItem: AVPMediaItem): Boolean {
    return downloads.value.values.any {
      it.mediaItem?.id == mediaItem.id && it.status == DownloadStatus.COMPLETED
    }
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
      workInfos?.forEach { workInfo ->
        val downloadId = extractDownloadId(workInfo)
        if (downloadId != null) {
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


  companion object {
    private const val DOWNLOAD_WORK_TAG = "media_download"
  }
}
