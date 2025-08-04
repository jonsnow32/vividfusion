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

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    private val _downloads = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadItem>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Set<String>>(emptySet())
    val activeDownloads: StateFlow<Set<String>> = _activeDownloads.asStateFlow()

    init {
        // Monitor work manager for download progress updates
        observeWorkManagerUpdates()
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
        val downloadId = generateDownloadId(mediaItem)
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
        workManager.enqueueUniqueWork(
            "download_$downloadId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        _activeDownloads.value = _activeDownloads.value + downloadId

        Timber.d("Started ${downloadType.name} download for ${mediaItem.title} with ID: $downloadId")
        return downloadId
    }

    /**
     * Cancel an active download
     */
    fun cancelDownload(downloadId: String) {
        workManager.cancelUniqueWork("download_$downloadId")

        val currentItem = _downloads.value[downloadId]
        if (currentItem != null) {
            updateDownloadItem(currentItem.copy(
                status = DownloadStatus.CANCELLED,
                updatedAt = System.currentTimeMillis()
            ))
        }

        _activeDownloads.value = _activeDownloads.value - downloadId
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

      _activeDownloads.value -= downloadId
        Timber.d("Paused download: $downloadId")
    }

    /**
     * Resume a paused download
     */
    fun resumeDownload(downloadId: String) {
        val downloadItem = _downloads.value[downloadId] ?: return

        if (downloadItem.status == DownloadStatus.PAUSED) {
            // Restart the download
            startDownload(downloadItem.mediaItem, downloadItem.url)
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
        cancelDownload(downloadId)

        val currentDownloads = _downloads.value.toMutableMap()
        currentDownloads.remove(downloadId)
        _downloads.value = currentDownloads

        Timber.d("Removed download: $downloadId")
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
        // Observe work info changes for download updates
        workManager.getWorkInfosByTagLiveData(DOWNLOAD_WORK_TAG).observeForever { workInfos ->
            workInfos?.forEach { workInfo ->
                updateDownloadFromWorkInfo(workInfo)
            }
        }
    }

    private fun updateDownloadFromWorkInfo(workInfo: WorkInfo) {
        val downloadId = workInfo.progress.getString("downloadId") ?: return
        val currentItem = _downloads.value[downloadId] ?: return

        when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> {
                updateDownloadItem(currentItem.copy(
                    status = DownloadStatus.PENDING,
                    updatedAt = System.currentTimeMillis()
                ))
            }
            WorkInfo.State.RUNNING -> {
                val progress = workInfo.progress.getInt("progress", 0)
                val downloadedBytes = workInfo.progress.getLong("downloadedBytes", 0L)
                val totalBytes = workInfo.progress.getLong("totalBytes", 0L)

                updateDownloadItem(currentItem.copy(
                    status = DownloadStatus.DOWNLOADING,
                    progress = progress,
                    downloadedBytes = downloadedBytes,
                    fileSize = if (totalBytes > 0) totalBytes else currentItem.fileSize,
                    updatedAt = System.currentTimeMillis()
                ))
            }
            WorkInfo.State.SUCCEEDED -> {
                val localPath = workInfo.outputData.getString("localPath")
                val fileSize = workInfo.outputData.getLong("fileSize", 0L)

                updateDownloadItem(currentItem.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100,
                    downloadedBytes = fileSize,
                    fileSize = fileSize,
                    localPath = localPath,
                    updatedAt = System.currentTimeMillis()
                ))

                _activeDownloads.value = _activeDownloads.value - downloadId
            }
            WorkInfo.State.FAILED -> {
                updateDownloadItem(currentItem.copy(
                    status = DownloadStatus.FAILED,
                    updatedAt = System.currentTimeMillis()
                ))

                _activeDownloads.value = _activeDownloads.value - downloadId
            }
            WorkInfo.State.CANCELLED -> {
                updateDownloadItem(currentItem.copy(
                    status = DownloadStatus.CANCELLED,
                    updatedAt = System.currentTimeMillis()
                ))

                _activeDownloads.value = _activeDownloads.value - downloadId
            }
            WorkInfo.State.BLOCKED -> {
                // Handle blocked state if needed
            }
        }
    }

    private fun updateDownloadItem(downloadItem: DownloadItem) {
        val currentDownloads = _downloads.value.toMutableMap()
        currentDownloads[downloadItem.id] = downloadItem
        _downloads.value = currentDownloads
    }

    private fun generateDownloadId(mediaItem: AVPMediaItem): String {
        return "${mediaItem.id}_${UUID.randomUUID().toString().take(8)}"
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
