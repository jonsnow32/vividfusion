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
import cloud.app.vvf.common.models.isDownloadable
import cloud.app.vvf.datastore.app.AppDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val appDataStore: MutableStateFlow<AppDataStore> // Inject AppDataStore để sync
) {
    private val _downloads = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadItem>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Set<String>>(emptySet())
    val activeDownloads: StateFlow<Set<String>> = _activeDownloads.asStateFlow()

    // Track active work IDs to prevent duplicates
    private val activeWorkIds = mutableSetOf<String>()

    init {
        // Clean up old work first
        cleanupOldWork()
        // Monitor work manager for download progress updates
        observeWorkManagerUpdates()
        // Load existing downloads from AppDataStore on startup
        loadDownloadsFromDataStore()
    }

    /**
     * Clean up old/cancelled work to prevent accumulation
     */
    private fun cleanupOldWork() {
        // Cancel all previous download work to start fresh
        workManager.cancelAllWorkByTag(DOWNLOAD_WORK_TAG)
        Timber.d("Cleaned up all previous download work")
    }

    /**
     * Load downloads from AppDataStore to sync with persistent storage
     */
    private fun loadDownloadsFromDataStore() {
        val existingDownloads = appDataStore.value.getAllDownloads() ?: emptyList()
        val downloadsMap = existingDownloads.associateBy { it.id }
        _downloads.value = downloadsMap

        // Update active downloads based on current status
        val activeIds = existingDownloads
            .filter { it.isActive() }
            .map { it.id }
            .toSet()
        _activeDownloads.value = activeIds

        Timber.d("Loaded ${existingDownloads.size} downloads from AppDataStore")
    }

    /**
     * Start downloading media content with automatic type detection
     */    fun startDownload(
      mediaItem: AVPMediaItem,
      downloadUrl: String,

        quality: String = "default"
    ): String {
        val downloadType = DownloadType.fromUrl(downloadUrl)
        return startDownloadWithType(mediaItem, downloadUrl, downloadType, quality)
    }

    /**
     * Start downloading HLS stream
     */
    fun startHlsDownload(
        mediaItem: AVPMediaItem,
        hlsUrl: String,
        quality: String = "default"
    ): String {
        return startDownloadWithType(mediaItem, hlsUrl, DownloadType.HLS, quality)
    }

    /**
     * Start downloading from torrent file URL
     */
    fun startTorrentDownload(
        mediaItem: AVPMediaItem,
        torrentUrl: String,
        quality: String = "default"
    ): String {
        return startDownloadWithType(mediaItem, torrentUrl, DownloadType.TORRENT, quality)
    }

    /**
     * Start downloading from magnet link
     */
    fun startMagnetDownload(
        mediaItem: AVPMediaItem,
        magnetLink: String,
        quality: String = "default"
    ): String {
        return startDownloadWithType(mediaItem, magnetLink, DownloadType.MAGNET, quality)
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

        // Check if download already exists
        val existingDownload = _downloads.value[downloadId]
        if (existingDownload != null) {
            when (existingDownload.status) {
                DownloadStatus.DOWNLOADING -> {
                    Timber.d("Download already in progress: $downloadId")
                    return downloadId
                }
                DownloadStatus.PENDING -> {
                    Timber.d("Download already queued: $downloadId")
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

        val fileName = generateFileName(mediaItem, quality)

        val downloadItem = DownloadItem(
            id = downloadId,
            mediaItem = mediaItem,
            url = downloadUrl,
            fileName = fileName,
            status = DownloadStatus.PENDING
        )

        // Add to downloads map
        updateDownloadItem(downloadItem)

        // Create appropriate work request based on download type
        val workRequest = when (downloadType) {
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
                        TorrentDownloader.TorrentDownloadParams.KEY_DOWNLOAD_ID to downloadId,
                        TorrentDownloader.TorrentDownloadParams.KEY_TORRENT_URL to downloadUrl,
                        TorrentDownloader.TorrentDownloadParams.KEY_FILE_NAME to fileName
                    ))
                    .addTag(DOWNLOAD_WORK_TAG)
                    .addTag(downloadId)
                    .addTag("TORRENT")
                    .build()
            }
            DownloadType.MAGNET -> {
                OneTimeWorkRequestBuilder<TorrentDownloader>()
                    .setInputData(workDataOf(
                        TorrentDownloader.TorrentDownloadParams.KEY_DOWNLOAD_ID to downloadId,
                        TorrentDownloader.TorrentDownloadParams.KEY_MAGNET_LINK to downloadUrl,
                        TorrentDownloader.TorrentDownloadParams.KEY_FILE_NAME to fileName
                    ))
                    .addTag(DOWNLOAD_WORK_TAG)
                    .addTag(downloadId)
                    .addTag("MAGNET")
                    .build()
            }
            DownloadType.HTTP -> {
                OneTimeWorkRequestBuilder<MediaDownloader>()
                    .setInputData(workDataOf(
                        MediaDownloader.DownloadParams.KEY_DOWNLOAD_ID to downloadId,
                        MediaDownloader.DownloadParams.KEY_DOWNLOAD_URL to downloadUrl,
                        MediaDownloader.DownloadParams.KEY_FILE_NAME to fileName,
                        MediaDownloader.DownloadParams.KEY_QUALITY to quality,
                        MediaDownloader.DownloadParams.KEY_DOWNLOAD_TYPE to "HTTP"
                    ))
                    .addTag(DOWNLOAD_WORK_TAG)
                    .addTag(downloadId)
                    .addTag("HTTP")
                    .build()
            }
        }

        // Enqueue work with unique name to prevent duplicates
        val uniqueWorkName = "download_$downloadId"

        // Cancel any existing work for this download first
        workManager.cancelUniqueWork(uniqueWorkName)

        // Enqueue new work with REPLACE policy to ensure only one instance
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE, // Replace any existing work
            workRequest
        )

        _activeDownloads.value = _activeDownloads.value + downloadId
        activeWorkIds.add(downloadId)

        Timber.d("Started ${downloadType.name} download for ${mediaItem.title} with ID: $downloadId, Unique work name: $uniqueWorkName")
        return downloadId
    }

    /**
     * Cancel an active download
     */
    fun cancelDownload(downloadId: String) {
        Timber.d("Cancelling download: $downloadId - Reason: User requested")
        workManager.cancelUniqueWork("download_$downloadId")

        val currentItem = _downloads.value[downloadId]
        if (currentItem != null) {
            Timber.d("Download $downloadId status before cancel: ${currentItem.status}, progress: ${currentItem.progress}%")
            updateDownloadItem(currentItem.copy(
                status = DownloadStatus.CANCELLED,
                updatedAt = System.currentTimeMillis()
            ))
        }

        _activeDownloads.value = _activeDownloads.value - downloadId
        activeWorkIds.remove(downloadId)
        Timber.d("Cancelled download: $downloadId")
    }

    /**
     * Pause a download (implementation depends on underlying downloader capabilities)
     */
    fun pauseDownload(downloadId: String) {
        // For now, we'll cancel and mark as paused
        // In a more sophisticated implementation, you'd implement resumable downloads
        workManager.cancelUniqueWork("download_$downloadId")

        val currentItem = _downloads.value[downloadId]
        if (currentItem != null) {
            updateDownloadItem(currentItem.copy(
                status = DownloadStatus.PAUSED,
                updatedAt = System.currentTimeMillis()
            ))
        }

        _activeDownloads.value = _activeDownloads.value - downloadId
        activeWorkIds.remove(downloadId)
        Timber.d("Paused download: $downloadId")
    }

    /**
     * Resume a paused download
     */
    fun resumeDownload(downloadId: String) {
        val downloadItem = _downloads.value[downloadId] ?: return

        if (downloadItem.status == DownloadStatus.PAUSED) {
            // Update status to pending first
            updateDownloadItem(downloadItem.copy(
                status = DownloadStatus.PENDING,
                updatedAt = System.currentTimeMillis()
            ))

            // Resume with existing download item data using the correct downloader classes
            val downloadType = DownloadType.fromUrl(downloadItem.url)
            val workRequest = when (downloadType) {
                DownloadType.HLS -> {
                    OneTimeWorkRequestBuilder<HlsDownloader>()
                        .setInputData(workDataOf(
                            HlsDownloader.HlsDownloadParams.KEY_DOWNLOAD_ID to downloadId,
                            HlsDownloader.HlsDownloadParams.KEY_HLS_URL to downloadItem.url,
                            HlsDownloader.HlsDownloadParams.KEY_FILE_NAME to downloadItem.fileName,
                            HlsDownloader.HlsDownloadParams.KEY_QUALITY to "default"
                        ))
                        .addTag(DOWNLOAD_WORK_TAG)
                        .addTag(downloadId)
                        .addTag("HLS")
                        .build()
                }

                DownloadType.TORRENT -> {
                    OneTimeWorkRequestBuilder<TorrentDownloader>()
                        .setInputData(workDataOf(
                            TorrentDownloader.TorrentDownloadParams.KEY_DOWNLOAD_ID to downloadId,
                            TorrentDownloader.TorrentDownloadParams.KEY_TORRENT_URL to downloadItem.url,
                            TorrentDownloader.TorrentDownloadParams.KEY_FILE_NAME to downloadItem.fileName
                        ))
                        .addTag(DOWNLOAD_WORK_TAG)
                        .addTag(downloadId)
                        .addTag("TORRENT")
                        .build()
                }

                DownloadType.MAGNET -> {
                    OneTimeWorkRequestBuilder<TorrentDownloader>()
                        .setInputData(workDataOf(
                            TorrentDownloader.TorrentDownloadParams.KEY_DOWNLOAD_ID to downloadId,
                            TorrentDownloader.TorrentDownloadParams.KEY_MAGNET_LINK to downloadItem.url,
                            TorrentDownloader.TorrentDownloadParams.KEY_FILE_NAME to downloadItem.fileName
                        ))
                        .addTag(DOWNLOAD_WORK_TAG)
                        .addTag(downloadId)
                        .addTag("MAGNET")
                        .build()
                }

                DownloadType.HTTP -> {
                    OneTimeWorkRequestBuilder<MediaDownloader>()
                        .setInputData(workDataOf(
                            MediaDownloader.DownloadParams.KEY_DOWNLOAD_ID to downloadId,
                            MediaDownloader.DownloadParams.KEY_DOWNLOAD_URL to downloadItem.url,
                            MediaDownloader.DownloadParams.KEY_FILE_NAME to downloadItem.fileName,
                            MediaDownloader.DownloadParams.KEY_QUALITY to "default",
                            MediaDownloader.DownloadParams.KEY_DOWNLOAD_TYPE to "HTTP"
                        ))
                        .addTag(DOWNLOAD_WORK_TAG)
                        .addTag(downloadId)
                        .addTag("HTTP")
                        .build()
                }
            }

            // Enqueue work to resume download
            workManager.enqueueUniqueWork(
                "download_$downloadId",
                ExistingWorkPolicy.REPLACE, // Replace existing work
                workRequest
            )

            _activeDownloads.value = _activeDownloads.value + downloadId
            activeWorkIds.add(downloadId)
            Timber.d("Resumed download: $downloadId")
        }
    }

    /**
     * Retry a failed download
     */
    fun retryDownload(downloadId: String) {
        val downloadItem = _downloads.value[downloadId] ?: return

        if (downloadItem.canRetry()) {
            // Reset status and restart
            updateDownloadItem(downloadItem.copy(
                status = DownloadStatus.PENDING,
                progress = 0,
                downloadedBytes = 0,
                updatedAt = System.currentTimeMillis()
            ))

            startDownload(downloadItem.mediaItem, downloadItem.url)
        }
    }

    /**
     * Remove a download from the list
     */
    fun removeDownload(downloadId: String) {
        Timber.d("Removing download: $downloadId - Starting cleanup process")

        // Cancel the WorkManager task first
        val uniqueWorkName = "download_$downloadId"
        workManager.cancelUniqueWork(uniqueWorkName)

        // Also cancel by tag as backup
        workManager.cancelAllWorkByTag(downloadId)

        // Update download status to cancelled before removing
        val currentItem = _downloads.value[downloadId]
        if (currentItem != null) {
            Timber.d("Download $downloadId status before removal: ${currentItem.status}, progress: ${currentItem.progress}%")
            updateDownloadItem(currentItem.copy(
                status = DownloadStatus.CANCELLED,
                updatedAt = System.currentTimeMillis()
            ))
        }

        // Remove from tracking sets
        _activeDownloads.value = _activeDownloads.value - downloadId
        activeWorkIds.remove(downloadId)

        // Clean up caches
        lastUpdateTimes.remove(downloadId)
        cachedProgressData.remove(downloadId)

        // Remove from in-memory storage
        val currentDownloads = _downloads.value.toMutableMap()
        currentDownloads.remove(downloadId)
        _downloads.value = currentDownloads

        // Remove from persistent storage (AppDataStore)
        appDataStore.value.removeDownload(downloadId)

        Timber.d("Removed download: $downloadId - Cancelled work: $uniqueWorkName, cleaned up all references")
    }

    /**
     * Get download progress as a flow
     */
    fun getDownloadProgress(downloadId: String): Flow<Int> {
        return downloads.map { downloadMap ->
            downloadMap[downloadId]?.getProgressPercentage() ?: 0
        }
    }

    /**
     * Get all downloads for a specific media item
     */
    fun getDownloadsForMediaItem(mediaItem: AVPMediaItem): List<DownloadItem> {
        return _downloads.value.values.filter {
            it.mediaItem.id == mediaItem.id
        }
    }

    /**
     * Check if a media item is currently being downloaded
     */
    fun isDownloading(mediaItem: AVPMediaItem): Boolean {
        return _downloads.value.values.any {
            it.mediaItem.id == mediaItem.id && it.isActive()
        }
    }

    /**
     * Check if a media item has been downloaded
     */
    fun isDownloaded(mediaItem: AVPMediaItem): Boolean {
        return _downloads.value.values.any {
            it.mediaItem.id == mediaItem.id && it.isCompleted()
        }
    }

    private fun observeWorkManagerUpdates() {
        // Observe work info changes for download updates with better filtering
        Timber.d("Setting up WorkManager observer for tag: $DOWNLOAD_WORK_TAG")
        workManager.getWorkInfosByTagLiveData(DOWNLOAD_WORK_TAG).observeForever { workInfos ->
            val filteredWorkInfos = workInfos?.filter { workInfo ->
                // Only process work that's currently active or relevant
                val downloadIdFromProgress = workInfo.progress.getString("downloadId")

                // Use the same improved downloadId extraction logic as updateDownloadFromWorkInfo
                val downloadIdFromTags = workInfo.tags.find { tag ->
                    tag != DOWNLOAD_WORK_TAG && // Exclude main work tag
                    !tag.matches(Regex("(HLS|TORRENT|MAGNET|HTTP)")) && // Exclude download type tags
                    !tag.contains("cloud.app.vvf") && // Exclude class names
                    !tag.contains(".") && // Exclude any other class/package names
                    tag.isNotEmpty() && // Ensure tag is not empty
                    tag.length > 10 // Download IDs should be reasonably long
                }

                val downloadId = downloadIdFromProgress ?: downloadIdFromTags

                // Filter conditions:
                // 1. Work must have a valid downloadId
                // 2. Include all relevant states (RUNNING, ENQUEUED, SUCCEEDED, FAILED, CANCELLED)
                // 3. Either the work is in a relevant state OR we have this download in our tracking
                downloadId != null && (
                    workInfo.state in listOf(
                        WorkInfo.State.RUNNING,
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.FAILED,
                        WorkInfo.State.CANCELLED
                    ) ||
                    activeWorkIds.contains(downloadId) ||
                    _downloads.value.containsKey(downloadId)
                )
            } ?: emptyList()

            Timber.d("WorkManager observer triggered - Total WorkInfos: ${workInfos?.size ?: 0}, Filtered: ${filteredWorkInfos.size}")

            filteredWorkInfos.forEach { workInfo ->
                val downloadIdFromProgress = workInfo.progress.getString("downloadId")
                val downloadIdFromTags = workInfo.tags.find { tag ->
                    tag != DOWNLOAD_WORK_TAG && // Exclude main work tag
                    !tag.matches(Regex("(HLS|TORRENT|MAGNET|HTTP)")) && // Exclude download type tags
                    !tag.contains("cloud.app.vvf") && // Exclude class names
                    !tag.contains(".") && // Exclude any other class/package names
                    tag.isNotEmpty() && // Ensure tag is not empty
                    tag.length > 10 // Download IDs should be reasonably long
                }
                val downloadId = downloadIdFromProgress ?: downloadIdFromTags

                Timber.d("Processing filtered WorkInfo - ID: ${workInfo.id}, DownloadId: $downloadId, State: ${workInfo.state}")
                updateDownloadFromWorkInfo(workInfo)
            }
        }
    }

    // Cache to track last update times and prevent excessive UI updates
    private val lastUpdateTimes = mutableMapOf<String, Long>()
    private val progressUpdateThrottle = 1000L // Only update progress every 1 second

    // Cache to store latest progress data without triggering UI updates
    private val cachedProgressData = mutableMapOf<String, DownloadItem>()

    private fun updateDownloadFromWorkInfo(workInfo: WorkInfo) {
        // Try to get downloadId from progress data first, then from tags
        val downloadIdFromProgress = workInfo.progress.getString("downloadId")

        // If not in progress data, look for downloadId in tags
        // Exclude system tags, class names, and download type tags
        val downloadIdFromTags = workInfo.tags.find { tag ->
            tag != DOWNLOAD_WORK_TAG && // Exclude main work tag
            !tag.matches(Regex("(HLS|TORRENT|MAGNET|HTTP)")) && // Exclude download type tags
            !tag.contains("cloud.app.vvf") && // Exclude class names
            !tag.contains(".") && // Exclude any other class/package names
            tag.isNotEmpty() && // Ensure tag is not empty
            tag.length > 10 // Download IDs should be reasonably long
        }

        val downloadId = downloadIdFromProgress ?: downloadIdFromTags

        if (downloadId == null) {
            Timber.w("Could not find downloadId in WorkInfo - Progress: $downloadIdFromProgress, Available tags: ${workInfo.tags}")
            return
        }

        val currentItem = _downloads.value[downloadId]
        if (currentItem == null) {
            Timber.w("No download item found for $downloadId - Available downloads: ${_downloads.value.keys}")
            return
        }

        val currentTime = System.currentTimeMillis()

        Timber.d("Processing WorkInfo for $downloadId - State: ${workInfo.state}")

        when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> {
                updateDownloadItem(currentItem.copy(
                    status = DownloadStatus.PENDING,
                    updatedAt = currentTime
                ))
            }
            WorkInfo.State.RUNNING -> {
                val progress = workInfo.progress.getInt("progress", 0)
                val downloadedBytes = workInfo.progress.getLong("downloadedBytes", 0L)
                val totalBytes = workInfo.progress.getLong("totalBytes", 0L)

                // Create updated item with latest data
                val updatedItem = currentItem.copy(
                    status = DownloadStatus.DOWNLOADING,
                    progress = progress,
                    downloadedBytes = downloadedBytes,
                    fileSize = if (totalBytes > 0) totalBytes else currentItem.fileSize,
                    updatedAt = currentTime
                )

                // Cache the latest data
                cachedProgressData[downloadId] = updatedItem

                // Check if download is actually completed (100% but still in RUNNING state)
                if (progress == 100 && downloadedBytes == totalBytes && totalBytes > 0) {
                    Timber.d("Detected 100% completion in RUNNING state for $downloadId - Force completing")

                    // Force completion since WorkManager might not send SUCCEEDED state
                    val completedItem = updatedItem.copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 100,
                        downloadedBytes = totalBytes,
                        fileSize = totalBytes,
                        updatedAt = currentTime
                    )

                    updateDownloadItem(completedItem)

                    // Log 100% UI update for consistency
                    Timber.d("Download $downloadId UI update: 100% ($totalBytes/$totalBytes bytes) - FORCE COMPLETED")

                    _activeDownloads.value = _activeDownloads.value - downloadId
                    activeWorkIds.remove(downloadId)
                    lastUpdateTimes.remove(downloadId)
                    cachedProgressData.remove(downloadId)
                    Timber.d("Download $downloadId force completed successfully - UI should show COMPLETED")
                    return
                }

                // Throttle UI updates to reduce lag
                val lastUpdateTime = lastUpdateTimes[downloadId] ?: 0
                val timeSinceLastUpdate = currentTime - lastUpdateTime
                val shouldUpdate = timeSinceLastUpdate >= progressUpdateThrottle ||
                                 progress == 100 || // Always update for completion
                                 progress == 0 ||   // Always update for start
                                 currentItem.status != DownloadStatus.DOWNLOADING || // First time entering DOWNLOADING state
                                 abs(currentItem.progress - progress) >= 1 // Significant progress change (1%)

                if (shouldUpdate) {
                    // Update UI with cached data
                    updateDownloadItem(updatedItem)
                    lastUpdateTimes[downloadId] = currentTime
                    Timber.d("Download $downloadId UI update: $progress% ($downloadedBytes/$totalBytes bytes)")
                }
                // If not updating UI, we still have the latest data in cache
            }
            WorkInfo.State.SUCCEEDED -> {
                Timber.d("Processing SUCCEEDED state for $downloadId")
                val localPath = workInfo.outputData.getString("localPath")
                val fileSize = workInfo.outputData.getLong("fileSize", 0L)

                // Use cached data if available, otherwise use current item
                val latestData = cachedProgressData[downloadId] ?: currentItem

                val completedItem = latestData.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100,
                    downloadedBytes = fileSize,
                    fileSize = fileSize,
                    localPath = localPath,
                    updatedAt = currentTime
                )

                Timber.d("DownloadManager: About to update item to COMPLETED for $downloadId")
                updateDownloadItem(completedItem)

                // Log 100% UI update for consistency
                Timber.d("Download $downloadId UI update: 100% ($fileSize/$fileSize bytes)")

                _activeDownloads.value = _activeDownloads.value - downloadId
                activeWorkIds.remove(downloadId)
                lastUpdateTimes.remove(downloadId) // Clean up cache
                cachedProgressData.remove(downloadId) // Clean up progress cache
                Timber.d("Download $downloadId completed successfully - UI should show COMPLETED")
            }
            WorkInfo.State.FAILED -> {
                val error = workInfo.outputData.getString("error")
                updateDownloadItem(currentItem.copy(
                    status = DownloadStatus.FAILED,
                    updatedAt = currentTime
                ))

                _activeDownloads.value -= downloadId
                activeWorkIds.remove(downloadId)
                lastUpdateTimes.remove(downloadId) // Clean up cache
                cachedProgressData.remove(downloadId) // Clean up progress cache
                Timber.e("Download $downloadId failed: $error")
            }
            WorkInfo.State.CANCELLED -> {
                updateDownloadItem(currentItem.copy(
                    status = DownloadStatus.CANCELLED,
                    updatedAt = currentTime
                ))

                _activeDownloads.value = _activeDownloads.value - downloadId
                activeWorkIds.remove(downloadId)
                lastUpdateTimes.remove(downloadId) // Clean up cache
                cachedProgressData.remove(downloadId) // Clean up progress cache
                Timber.d("Download $downloadId cancelled")
            }
            WorkInfo.State.BLOCKED -> {
                // Handle blocked state if needed
                Timber.w("Download $downloadId is blocked")
            }
        }
    }

    private fun updateDownloadItem(downloadItem: DownloadItem) {
        // Update in-memory storage
        val currentDownloads = _downloads.value.toMutableMap()
        currentDownloads[downloadItem.id] = downloadItem
        _downloads.value = currentDownloads

        // Sync with persistent storage (AppDataStore)
        appDataStore.value.saveDownload(downloadItem)

        Timber.d("Updated download item: ${downloadItem.id} - Status: ${downloadItem.status} - Progress: ${downloadItem.progress}% (synced to AppDataStore)")
    }

    /**
     * Sync downloads between DownloadManager and AppDataStore
     * Called when AppDataStore data changes externally
     */
    fun syncWithAppDataStore() {
        val persistedDownloads = appDataStore.value.getAllDownloads() ?: emptyList()
        val currentDownloads = _downloads.value.toMutableMap()

        // Update downloads from AppDataStore
        persistedDownloads.forEach { persistedDownload ->
            val currentDownload = currentDownloads[persistedDownload.id]

            // If download exists in AppDataStore but not in memory, or if it's more recent
            if (currentDownload == null || persistedDownload.updatedAt > currentDownload.updatedAt) {
                currentDownloads[persistedDownload.id] = persistedDownload
            }
        }

        // Remove downloads that no longer exist in AppDataStore
        val persistedIds = persistedDownloads.map { it.id }.toSet()
        currentDownloads.keys.removeAll { id -> id !in persistedIds }

        _downloads.value = currentDownloads

        // Update active downloads
        val activeIds = persistedDownloads
            .filter { it.isActive() }
            .map { it.id }
            .toSet()
        _activeDownloads.value = activeIds

        Timber.d("DownloadManager: Synced ${persistedDownloads.size} downloads with AppDataStore")
    }

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

    /**
     * Debug method to get current WorkManager status
     */
    fun getWorkManagerDebugInfo(): String {
        val allWorkInfos = workManager.getWorkInfosByTag(DOWNLOAD_WORK_TAG).get()
        val debugInfo = StringBuilder()

        debugInfo.appendLine("=== WorkManager Debug Info ===")
        debugInfo.appendLine("Total WorkInfos with tag '$DOWNLOAD_WORK_TAG': ${allWorkInfos.size}")
        debugInfo.appendLine("Active downloads tracking: ${activeWorkIds.size}")
        debugInfo.appendLine("Downloads in memory: ${_downloads.value.size}")
        debugInfo.appendLine()

        allWorkInfos.groupBy { it.state }.forEach { (state, workInfos) ->
            debugInfo.appendLine("$state: ${workInfos.size} workers")
            workInfos.forEach { workInfo ->
                val downloadId = workInfo.progress.getString("downloadId")
                    ?: workInfo.tags.find { it != DOWNLOAD_WORK_TAG && !it.matches(Regex("(HLS|TORRENT|MAGNET|HTTP)")) }
                debugInfo.appendLine("  - WorkId: ${workInfo.id}, DownloadId: $downloadId, Tags: ${workInfo.tags}")
            }
        }

        return debugInfo.toString()
    }

    /**
     * Force cleanup all old work - call this if you have too many workers
     */
    fun forceCleanupAllWork() {
        Timber.d("DownloadManager: Force cleaning up all work")
        workManager.cancelAllWorkByTag(DOWNLOAD_WORK_TAG)
        activeWorkIds.clear()

        // Reset active downloads to only those that are actually downloading
        val actuallyActive = _downloads.value.values
            .filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
            .map { it.id }
            .toSet()
        _activeDownloads.value = actuallyActive

        Timber.d("DownloadManager: Force cleanup completed - Active downloads reset to: $actuallyActive")
    }

    companion object {
        private const val DOWNLOAD_WORK_TAG = "media_download"
    }
}
