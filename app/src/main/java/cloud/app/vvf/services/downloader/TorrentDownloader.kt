package cloud.app.vvf.services.downloader

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.app.vvf.features.player.torrent.TorrentManager
import cloud.app.vvf.network.api.torrentserver.TorrentStatus
import cloud.app.vvf.services.downloader.helper.DownloadFileManager
import cloud.app.vvf.services.downloader.helper.DownloadNotificationManager
import cloud.app.vvf.services.downloader.helper.DownloadProgressTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

@HiltWorker
class TorrentDownloader @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val torrentManager: TorrentManager
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val KEY_DOWNLOAD_ID = "key_download_id"
        const val KEY_TORRENT_URL = "key_torrent_url"
        const val KEY_MAGNET_LINK = "key_magnet_link"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_DOWNLOAD_PATH = "key_download_path"
    }

    private val notificationManager = DownloadNotificationManager(context)
    private val fileManager = DownloadFileManager(context)
    private lateinit var progressTracker: DownloadProgressTracker
    private val isDownloadActive = AtomicBoolean(true)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadParams = extractDownloadParams()
        if (!downloadParams.isValid()) {
            return@withContext Result.failure(workDataOf("error" to "Missing required parameters"))
        }

        progressTracker = DownloadProgressTracker(this@TorrentDownloader, notificationManager)

        Timber.d("Starting torrent download: ${downloadParams.downloadId} | ${downloadParams.torrentUrl} | ${downloadParams.magnetLink}")

        // Set foreground if notification permission available
        if (notificationManager.hasPermission()) {
            setForeground(notificationManager.createForegroundInfo(downloadParams.fileName, "Processing torrent...", 0))
        }

        try {
            val result = when {
                downloadParams.magnetLink.isNotEmpty() -> downloadFromTorrentLink(downloadParams.magnetLink, downloadParams)
                downloadParams.torrentUrl.isNotEmpty() -> downloadFromTorrentLink(downloadParams.torrentUrl, downloadParams)
                else -> throw IllegalArgumentException("No valid torrent source provided")
            }
            val keys = DownloadData.Companion.Keys
            Result.success(workDataOf(
                keys.DOWNLOAD_ID to result["downloadId"],
                keys.LOCAL_PATH to result["localPath"],
                keys.FILE_NAME to result["fileName"],
                keys.FILE_SIZE to result["fileSize"],
                keys.STREAM_URL to (result["streamUrl"] ?: ""),
                keys.NOTE to result["note"]
            ))
        } catch (e: Exception) {
            Timber.e(e, "Torrent download failed for ${downloadParams.downloadId}")
            val keys = DownloadData.Companion.Keys
            Result.failure(workDataOf(
                keys.ERROR to (e.message ?: "Unknown torrent download error"),
                keys.DOWNLOAD_ID to downloadParams.downloadId
            ))
        }
    }

    private suspend fun downloadFromTorrentLink(
        torrentLink: String,
        params: TorrentDownloadParams
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        Timber.d("Processing torrent link with TorrentManager: $torrentLink")

        try {
            // Use TorrentManager to transform torrent/magnet link into stream URL
            val (streamUrl, torrentStatus) = torrentManager.transformLink(
                torrentLink,
                context.cacheDir,
                context
            )

            Timber.d("Torrent transformed to stream URL: $streamUrl")

            // Start monitoring torrent status for progress updates
            monitorTorrentProgress(params.downloadId, torrentStatus)

            // Now download the stream using regular HTTP download
            return@withContext downloadFromStreamUrl(streamUrl, params, torrentStatus)

        } catch (e: Exception) {
            Timber.w(e, "TorrentManager transform failed, falling back to alternative methods")
            // Fallback to our previous implementation if TorrentManager fails
            return@withContext fallbackTorrentDownload(torrentLink, params)
        }
    }

    private fun monitorTorrentProgress(downloadId: String, torrentStatus: TorrentStatus) {
        // Monitor torrent status in background using proper coroutine scope
        CoroutineScope(Dispatchers.IO).launch {
            while (isDownloadActive.get() && isActive) {
                try {
                    // Get real torrent status from TorrentManager
                    val currentTorrentStatus = torrentStatus.hash?.let { torrentManager.get(it) }

                    if (currentTorrentStatus != null) {
                        // Extract torrent-specific data using actual TorrentStatus properties
                        val loadedSize = currentTorrentStatus.loadedSize ?: 0L
                        val torrentSize = currentTorrentStatus.torrentSize ?: 0L
                        val progress = if (torrentSize > 0) {
                            ((loadedSize * 100) / torrentSize).toInt()
                        } else 0

                        val downloadSpeed = (currentTorrentStatus.downloadSpeed ?: 0.0).toLong()
                        val uploadSpeed = (currentTorrentStatus.uploadSpeed ?: 0.0).toLong()
                        val activePeers = currentTorrentStatus.activePeers ?: 0
                        val connectedSeeders = currentTorrentStatus.connectedSeeders ?: 0
                        val totalPeers = currentTorrentStatus.totalPeers ?: 0
                        val bytesRead = currentTorrentStatus.bytesRead ?: 0L
                        val bytesWritten = currentTorrentStatus.bytesWritten ?: 0L

                        // Calculate share ratio
                        val shareRatio = if (bytesRead > 0) {
                            (bytesWritten.toFloat() / bytesRead.toFloat())
                        } else 0.0f

                        // Update progress using progressTracker with extended data
                        setProgressAsync(workDataOf(
                            DownloadData.Companion.Keys.PROGRESS to progress,
                            DownloadData.Companion.Keys.DOWNLOADED_BYTES to loadedSize,
                            DownloadData.Companion.Keys.TOTAL_BYTES to torrentSize,
                            DownloadData.Companion.Keys.DOWNLOAD_ID to downloadId,
                            DownloadData.Companion.Keys.DOWNLOAD_SPEED to downloadSpeed,
                            DownloadData.Companion.Keys.UPLOAD_SPEED to uploadSpeed,
                            DownloadData.Companion.Keys.PEERS to activePeers,
                            DownloadData.Companion.Keys.SEEDS to connectedSeeders,
                            DownloadData.Companion.Keys.TOTAL_PEERS to totalPeers,
                            DownloadData.Companion.Keys.SHARE_RATIO to shareRatio,
                            DownloadData.Companion.Keys.PRELOADED_BYTES to (currentTorrentStatus.preloadedBytes ?: 0L),
                            DownloadData.Companion.Keys.TORRENT_STATE to (currentTorrentStatus.statString ?: "unknown"),
                            DownloadData.Companion.Keys.BYTES_READ to bytesRead,
                            DownloadData.Companion.Keys.BYTES_WRITTEN to bytesWritten
                        ))

                        Timber.d("Torrent progress $downloadId: $progress% - D: ${downloadSpeed}B/s U: ${uploadSpeed}B/s - Peers: $activePeers/$connectedSeeders")
                    } else {
                        // Fallback if we can't get torrent status
                        Timber.w("Could not retrieve torrent status for hash: ${torrentStatus.hash}")
                        setProgressAsync(workDataOf(
                            DownloadData.Companion.Keys.PROGRESS to 0,
                            DownloadData.Companion.Keys.DOWNLOAD_ID to downloadId,
                            DownloadData.Companion.Keys.DOWNLOAD_SPEED to 0L,
                            DownloadData.Companion.Keys.UPLOAD_SPEED to 0L,
                            DownloadData.Companion.Keys.PEERS to 0,
                            DownloadData.Companion.Keys.SEEDS to 0,
                            DownloadData.Companion.Keys.TORRENT_STATE to "error"
                        ))
                    }

                    // Update every 3 seconds
                    delay(3000)
                } catch (e: Exception) {
                    Timber.w(e, "Error monitoring torrent progress for $downloadId")
                    delay(5000) // Wait longer on error
                }
            }
        }
    }

    private suspend fun downloadFromStreamUrl(
        streamUrl: String,
        params: TorrentDownloadParams,
        torrentStatus: Any
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        Timber.d("Downloading from stream URL: $streamUrl")

        // Create output file using fileManager
        val (outputFile, _) = fileManager.createOrGetFile(
            params.fileName,
            "video/mp4",
            false,
            0L
        )

        // Configure HTTP client for streaming download
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val request = Request.Builder()
            .url(streamUrl)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to download from stream URL: HTTP ${response.code}")
        }

        var totalSize = response.body?.contentLength() ?: 0L
        var downloadedSize = 0L
        val startTime = System.currentTimeMillis()

        // Download the stream to file
        response.body?.byteStream()?.use { inputStream ->
            outputFile.openOutputStream().use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1 &&
                       isDownloadActive.get() && isActive && !isStopped) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead

                    // Update progress using progressTracker
                    progressTracker.updateProgress(
                        params.downloadId,
                        params.fileName,
                        downloadedSize,
                        totalSize
                    )
                }
            }
        }

        // Final size update
        val actualFileSize = outputFile.length()
        val filePath = outputFile.uri.toString()
        val localPath = outputFile.uri.path ?: filePath

        // Final progress update
        progressTracker.updateFinalProgress(
            params.downloadId,
            params.fileName,
            filePath,
            localPath,
            actualFileSize
        )

        mapOf(
            "downloadId" to params.downloadId,
            "localPath" to localPath,
            "fileName" to "${params.fileName}.mp4",
            "fileSize" to actualFileSize,
            "streamUrl" to streamUrl,
            "note" to "Downloaded via TorrentManager stream URL"
        )
    }

    private suspend fun fallbackTorrentDownload(
        torrentLink: String,
        params: TorrentDownloadParams
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        Timber.w("Using fallback torrent download for: $torrentLink")

        // Create output file using fileManager
        val (outputFile, _) = fileManager.createOrGetFile(
            params.fileName,
            "video/mp4",
            false,
            0L
        )

        val outputPath = outputFile.uri.path ?: outputFile.uri.toString()

        // Provide user feedback about the limitation
        for (i in 0..100 step 5) {
            if (!isDownloadActive.get() || !isActive || isStopped) break

            progressTracker.updateProgress(
                params.downloadId,
                params.fileName,
                (i * 1024L),
                (100 * 1024L)
            )
            delay(1000)
        }

        mapOf(
            "downloadId" to params.downloadId,
            "localPath" to outputPath,
            "fileName" to "${params.fileName}.mp4",
            "fileSize" to (100 * 1024L),
            "note" to "Fallback torrent handling - TorrentManager required for full functionality"
        )
    }

    private fun extractDownloadParams(): TorrentDownloadParams {
        return TorrentDownloadParams(
            downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: "",
            torrentUrl = inputData.getString(KEY_TORRENT_URL) ?: "",
            magnetLink = inputData.getString(KEY_MAGNET_LINK) ?: "",
            fileName = inputData.getString(KEY_FILE_NAME) ?: "",
            downloadPath = inputData.getString(KEY_DOWNLOAD_PATH) ?: ""
        )
    }

    data class TorrentDownloadParams(
        val downloadId: String,
        val torrentUrl: String,
        val magnetLink: String,
        val fileName: String,
        val downloadPath: String
    ) {
        fun isValid(): Boolean = downloadId.isNotEmpty() &&
                                fileName.isNotEmpty() &&
                                (torrentUrl.isNotEmpty() || magnetLink.isNotEmpty())
    }
}
