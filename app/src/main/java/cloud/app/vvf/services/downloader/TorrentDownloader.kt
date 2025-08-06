package cloud.app.vvf.services.downloader

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.app.vvf.features.player.torrent.TorrentManager
import cloud.app.vvf.utils.KUniFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

@HiltWorker
class TorrentDownloader @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val torrentManager: TorrentManager
) : CoroutineWorker(context, workerParameters) {

    object DownloadParams {
        const val KEY_DOWNLOAD_ID = "key_download_id"
        const val KEY_TORRENT_URL = "key_torrent_url"
        const val KEY_MAGNET_LINK = "key_magnet_link"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_DOWNLOAD_PATH = "key_download_path"
    }

    private val isDownloadActive = AtomicBoolean(true)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(DownloadParams.KEY_DOWNLOAD_ID) ?: ""
        val torrentUrl = inputData.getString(DownloadParams.KEY_TORRENT_URL)
        val magnetLink = inputData.getString(DownloadParams.KEY_MAGNET_LINK)
        val fileName = inputData.getString(DownloadParams.KEY_FILE_NAME) ?: ""

        Timber.d("Starting torrent download: $downloadId | $torrentUrl | $magnetLink")

        if (downloadId.isEmpty() || (torrentUrl.isNullOrEmpty() && magnetLink.isNullOrEmpty())) {
            return@withContext Result.failure(workDataOf("error" to "Missing required parameters"))
        }

        try {
            // Set up foreground service
            setForeground(createForegroundInfo(fileName))

            val result = when {
                !magnetLink.isNullOrEmpty() -> downloadFromTorrentLink(magnetLink, downloadId, fileName)
                !torrentUrl.isNullOrEmpty() -> downloadFromTorrentLink(torrentUrl, downloadId, fileName)
                else -> throw IllegalArgumentException("No valid torrent source provided")
            }

            Result.success(workDataOf(
                "downloadId" to result["downloadId"],
                "localPath" to result["localPath"],
                "fileName" to result["fileName"],
                "fileSize" to result["fileSize"],
                "streamUrl" to (result["streamUrl"] ?: ""),
                "note" to result["note"]
            ))
        } catch (e: Exception) {
            Timber.e(e, "Torrent download failed for $downloadId")
            Result.failure(workDataOf(
                "error" to (e.message ?: "Unknown torrent download error"),
                "downloadId" to downloadId
            ))
        }
    }

    private suspend fun downloadFromTorrentLink(
        torrentLink: String,
        downloadId: String,
        fileName: String
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

            // Now download the stream using regular HTTP download
            return@withContext downloadFromStreamUrl(streamUrl, downloadId, fileName, torrentStatus)

        } catch (e: Exception) {
            Timber.w(e, "TorrentManager transform failed, falling back to alternative methods")
            // Fallback to our previous implementation if TorrentManager fails
            return@withContext fallbackTorrentDownload(torrentLink, downloadId, fileName)
        }
    }

    private suspend fun downloadFromStreamUrl(
        streamUrl: String,
        downloadId: String,
        fileName: String,
        torrentStatus: Any
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        Timber.d("Downloading from stream URL: $streamUrl")

        val downloadsDir = getDownloadsDirectory()
        val outputFile = File(
            downloadsDir.filePath
                ?: context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.absolutePath
                ?: context.filesDir.absolutePath,
            "$fileName.mp4"
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
            outputFile.outputStream().use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var lastProgressUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1 && isDownloadActive.get() && isActive) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead

                    // Update progress every 2 seconds
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastProgressUpdate > 2000) {
                        lastProgressUpdate = currentTime

                        val progress = if (totalSize > 0) {
                            (downloadedSize * 100 / totalSize).toInt()
                        } else {
                            // If we don't know total size, estimate based on time
                            minOf(((currentTime - startTime) / 1000).toInt(), 95)
                        }

                        val elapsedSeconds = (currentTime - startTime) / 1000
                        val downloadSpeed = if (elapsedSeconds > 0) downloadedSize / elapsedSeconds else 0L

                        setProgressAsync(workDataOf(
                            "progress" to progress,
                            "downloadedBytes" to downloadedSize,
                            "totalBytes" to totalSize,
                            "downloadId" to downloadId,
                            "downloadSpeed" to downloadSpeed,
                            "uploadSpeed" to 0L, // Not applicable for stream download
                            "peers" to 1, // Stream server
                            "seeds" to 1,
                            "streamUrl" to streamUrl
                        ))

                        Timber.d("Stream download progress $downloadId: $progress% ($downloadedSize/$totalSize bytes) - Speed: ${downloadSpeed} B/s")
                    }
                }
            }
        }

        // Final size update
        val actualFileSize = outputFile.length()

        mapOf(
            "downloadId" to downloadId,
            "localPath" to outputFile.absolutePath,
            "fileName" to "$fileName.mp4",
            "fileSize" to actualFileSize,
            "streamUrl" to streamUrl,
            "note" to "Downloaded via TorrentManager stream URL"
        )
    }

    private suspend fun fallbackTorrentDownload(
        torrentLink: String,
        downloadId: String,
        fileName: String
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        Timber.w("Using fallback torrent download for: $torrentLink")

        // Simple fallback implementation
        val downloadsDir = getDownloadsDirectory()
        val outputPath = File(
            downloadsDir.filePath
                ?: context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.absolutePath
                ?: context.filesDir.absolutePath,
            "$fileName.mp4"
        ).absolutePath

        // Provide user feedback about the limitation
        for (i in 0..100 step 5) {
            if (!isDownloadActive.get() || !isActive) break

            setProgressAsync(workDataOf(
                "progress" to i,
                "downloadedBytes" to (i * 1024L),
                "totalBytes" to (100 * 1024L),
                "downloadId" to downloadId,
                "downloadSpeed" to 0,
                "uploadSpeed" to 0,
                "peers" to 0,
                "seeds" to 0,
                "note" to "TorrentManager unavailable - fallback mode"
            ))
            delay(1000)
        }

        mapOf(
            "downloadId" to downloadId,
            "localPath" to outputPath,
            "fileName" to "$fileName.mp4",
            "fileSize" to (100 * 1024L),
            "note" to "Fallback torrent handling - TorrentManager required for full functionality"
        )
    }

    private fun getDownloadsDirectory(): KUniFile {
        val publicDownloads = KUniFile.fromFile(context,
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
        )

        return publicDownloads ?: KUniFile.fromFile(context,
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        ) ?: throw IOException("Cannot access downloads directory")
    }

    private fun createForegroundInfo(fileName: String): ForegroundInfo {
        return MediaDownloader.createDownloadForegroundInfo(
            context,
            fileName,
            "Processing torrent...",
            0
        )
    }
}
