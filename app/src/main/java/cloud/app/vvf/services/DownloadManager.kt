package cloud.app.vvf.services.downloader

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.DownloadItem
import cloud.app.vvf.common.models.DownloadStatus
import cloud.app.vvf.datastore.app.AppDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
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
    val downloads: StateFlow<Map<String, DownloadItem>> = downloadController.downloads

    // Active downloads computed from controller
    val activeDownloads: StateFlow<Set<String>> = downloadController.downloads
        .map { downloads ->
            downloads.values.filter { it.isActive() }.map { it.id }.toSet()
        }
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
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
        Timber.d("Cleaned up all previous download work")
    }

    /**
     * Load downloads from AppDataStore and initialize controller
     */
    private fun loadDownloadsFromDataStore() {
        val existingDownloads = appDataStore.value.getAllDownloads() ?: emptyList()
        downloadController.initializeFromPersistedData(existingDownloads)
        Timber.d("Loaded ${existingDownloads.size} downloads from AppDataStore")
    }

    /**
     * Start downloading media content with automatic type detection
     */
    fun startDownload(
        mediaItem: AVPMediaItem,
        downloadUrl: String,
        quality: String = "default"
    ): String {
        val downloadType = DownloadType.fromUrl(downloadUrl)
        return startDownloadWithType(mediaItem, downloadUrl, downloadType, quality)
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
                    Timber.d("Download already in progress: $downloadId")
                    return downloadId
                }
                DownloadStatus.COMPLETED -> {
                    Timber.d("Download already completed: $downloadId")
                    return downloadId
                }
                DownloadStatus.PAUSED -> {
                    Timber.d("Resuming existing paused download: $downloadId")
                    resumeDownload(downloadId)
                    return downloadId
                }
                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    Timber.d("Retrying existing failed/cancelled download: $downloadId")
                    // Continue with creating new download to retry
                }
            }
        }

        // Create new download item
        val downloadItem = DownloadItem(
            id = downloadId,
            mediaItem = mediaItem,
            url = downloadUrl,
            fileName = fileName,
            status = DownloadStatus.PENDING
        )

        // Add to controller and persist
        downloadController.addDownloadItem(downloadItem)
        appDataStore.value.saveDownload(downloadItem)

        // Execute start command
        val command = DownloadCommand.Start(downloadId, downloadUrl, fileName)
        if (downloadController.executeCommand(command)) {
            // Create and enqueue WorkManager request
            enqueueWorkRequest(downloadId, downloadType, downloadUrl, fileName, quality)
            Timber.d("Started ${downloadType.name} download: $downloadId")
        } else {
            Timber.e("Failed to start download: $downloadId")
        }

        return downloadId
    }

    /**
     * Pause a download
     */
    fun pauseDownload(downloadId: String) {
        Timber.d("DownloadManager.pauseDownload called for: $downloadId")

        if (!downloadController.canPause(downloadId)) {
            Timber.w("DownloadManager: Cannot pause download: $downloadId")
            return
        }

        Timber.d("DownloadManager: About to execute Pause command for: $downloadId")
        val command = DownloadCommand.Pause(downloadId)
        if (downloadController.executeCommand(command)) {
            Timber.d("DownloadManager: Pause command executed successfully for: $downloadId")
            // Cancel WorkManager task
            workManager.cancelUniqueWork("download_$downloadId")
            Timber.d("DownloadManager: Cancelled WorkManager task for: $downloadId")
        } else {
            Timber.e("DownloadManager: Failed to execute Pause command for: $downloadId")
        }
    }

    /**
     * Resume a paused download
     */
    fun resumeDownload(downloadId: String) {
        if (!downloadController.canResume(downloadId)) {
            Timber.w("Cannot resume download: $downloadId")
            return
        }

        val downloadItem = downloads.value[downloadId] ?: return

        // Calculate actual downloaded bytes from file if it exists
        val actualDownloadedBytes = getActualDownloadedBytes(downloadItem.fileName)

        // Update download item with actual file size if different
        if (actualDownloadedBytes != downloadItem.downloadedBytes && actualDownloadedBytes > 0) {
            val updatedItem = downloadItem.copy(
                downloadedBytes = actualDownloadedBytes,
                progress = if (downloadItem.fileSize > 0) {
                    ((actualDownloadedBytes * 100) / downloadItem.fileSize).toInt()
                } else downloadItem.progress
            )
            // Save updated item to datastore - the controller will sync on resume
            appDataStore.value.saveDownload(updatedItem)
            Timber.d("Updated download $downloadId with actual downloaded bytes: $actualDownloadedBytes")
        }

        val command = DownloadCommand.Resume(downloadId)

        if (downloadController.executeCommand(command)) {
            // Re-enqueue work request with resume parameters
            val downloadType = DownloadType.fromUrl(downloadItem.url)
            val finalDownloadedBytes = if (actualDownloadedBytes > 0) actualDownloadedBytes else downloadItem.downloadedBytes

            enqueueWorkRequest(
                downloadId = downloadId,
                downloadType = downloadType,
                downloadUrl = downloadItem.url,
                fileName = downloadItem.fileName,
                quality = "default",
                resumeBytes = finalDownloadedBytes,
                resumeProgress = downloadItem.progress,
                isResuming = true
            )
            Timber.d("Resumed download: $downloadId from ${finalDownloadedBytes} bytes (${downloadItem.progress}%)")
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
            Timber.w(e, "Failed to get actual downloaded bytes for: $fileName")
            0L
        }
    }

    /**
     * Get downloads directory
     */
    private fun getDownloadsDirectory(): cloud.app.vvf.utils.KUniFile {
        return try {
            val downloadsFolder = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "VividFusion")
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs()
            }
            cloud.app.vvf.utils.KUniFile.fromFile(context, downloadsFolder)
                ?: throw Exception("Failed to create KUniFile")
        } catch (e: Exception) {
            Timber.e(e, "Failed to get downloads directory")
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
            Timber.d("Cancelled download: $downloadId")
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
            Timber.d("Removed download: $downloadId")
        }
    }

    /**
     * Retry a failed download
     */
    fun retryDownload(downloadId: String) {
        val downloadItem = downloads.value[downloadId] ?: return

        if (downloadItem.status == DownloadStatus.FAILED || downloadItem.status == DownloadStatus.CANCELLED) {
            // Start a new download for retry
            startDownload(downloadItem.mediaItem, downloadItem.url)
            Timber.d("Retrying download: $downloadId")
        } else {
            Timber.w("Cannot retry download $downloadId: current status is ${downloadItem.status}")
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
    fun getDownloadsForMediaItem(mediaItem: AVPMediaItem): List<DownloadItem> {
        return downloads.value.values.filter {
            it.mediaItem.id == mediaItem.id
        }
    }

    /**
     * Check if a media item is currently being downloaded
     */
    fun isDownloading(mediaItem: AVPMediaItem): Boolean {
        return downloads.value.values.any {
            it.mediaItem.id == mediaItem.id && it.isActive()
        }
    }

    /**
     * Check if a media item has been downloaded
     */
    fun isDownloaded(mediaItem: AVPMediaItem): Boolean {
        return downloads.value.values.any {
            it.mediaItem.id == mediaItem.id && it.status == DownloadStatus.COMPLETED
        }
    }

    /**
     * Enqueue WorkManager request
     */
    private fun enqueueWorkRequest(
        downloadId: String,
        downloadType: DownloadType,
        downloadUrl: String,
        fileName: String,
        quality: String,
        resumeBytes: Long = 0L,
        resumeProgress: Int = 0,
        isResuming: Boolean = false
    ) {
        val workRequest = when (downloadType) {
            DownloadType.HTTP -> {
                OneTimeWorkRequestBuilder<MediaDownloader>()
                    .setInputData(workDataOf(
                        MediaDownloader.DownloadParams.KEY_DOWNLOAD_ID to downloadId,
                        MediaDownloader.DownloadParams.KEY_DOWNLOAD_URL to downloadUrl,
                        MediaDownloader.DownloadParams.KEY_FILE_NAME to fileName,
                        MediaDownloader.DownloadParams.KEY_QUALITY to quality,
                        MediaDownloader.DownloadParams.KEY_DOWNLOAD_TYPE to "HTTP",
                        MediaDownloader.DownloadParams.KEY_RESUME_PROGRESS to resumeProgress,
                        MediaDownloader.DownloadParams.KEY_RESUME_BYTES to resumeBytes,
                        MediaDownloader.DownloadParams.KEY_RESUME_FROM_PAUSE to isResuming
                    ))
                    .addTag(DOWNLOAD_WORK_TAG)
                    .addTag(downloadId)
                    .addTag("HTTP")
                    .build()
            }
            DownloadType.HLS -> {
                OneTimeWorkRequestBuilder<HlsDownloader>()
                    .setInputData(workDataOf(
                        HlsDownloader.HlsDownloadParams.KEY_DOWNLOAD_ID to downloadId,
                        HlsDownloader.HlsDownloadParams.KEY_HLS_URL to downloadUrl,
                        HlsDownloader.HlsDownloadParams.KEY_FILE_NAME to fileName,
                        HlsDownloader.HlsDownloadParams.KEY_QUALITY to quality
                    ))
                    .addTag(DOWNLOAD_WORK_TAG)
                    .addTag(downloadId)
                    .addTag("HLS")
                    .build()
            }
            DownloadType.TORRENT -> {
                OneTimeWorkRequestBuilder<TorrentDownloader>()
                    .setInputData(workDataOf(
                        TorrentDownloader.DownloadParams.KEY_DOWNLOAD_ID to downloadId,
                        TorrentDownloader.DownloadParams.KEY_TORRENT_URL to downloadUrl,
                        TorrentDownloader.DownloadParams.KEY_FILE_NAME to fileName
                    ))
                    .addTag(DOWNLOAD_WORK_TAG)
                    .addTag(downloadId)
                    .addTag("TORRENT")
                    .build()
            }
            DownloadType.MAGNET -> {
                OneTimeWorkRequestBuilder<TorrentDownloader>()
                    .setInputData(workDataOf(
                        TorrentDownloader.DownloadParams.KEY_DOWNLOAD_ID to downloadId,
                        TorrentDownloader.DownloadParams.KEY_MAGNET_LINK to downloadUrl,
                        TorrentDownloader.DownloadParams.KEY_FILE_NAME to fileName
                    ))
                    .addTag(DOWNLOAD_WORK_TAG)
                    .addTag(downloadId)
                    .addTag("MAGNET")
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
        Timber.d("Setting up WorkManager observer")
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

        Timber.d("Generated deterministic downloadId: $finalId for URL: ${downloadUrl.take(50)}...")
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

        return if (quality != "default") "${cleanName}_${quality}" else cleanName ?: "download_${System.currentTimeMillis()}"
    }


    companion object {
        private const val DOWNLOAD_WORK_TAG = "media_download"
    }
}
