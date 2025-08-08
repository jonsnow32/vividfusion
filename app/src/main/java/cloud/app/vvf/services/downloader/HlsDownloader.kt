package cloud.app.vvf.services.downloader

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.helper.DownloadFileManager
import cloud.app.vvf.services.downloader.helper.DownloadNotificationManager
import cloud.app.vvf.services.downloader.helper.DownloadProgressTracker
import cloud.app.vvf.services.downloader.helper.HttpDownloadClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.net.URI
import java.util.concurrent.atomic.AtomicLong

@HiltWorker
class HlsDownloader @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParameters: WorkerParameters
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
    private lateinit var progressTracker: DownloadProgressTracker

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadParams = extractDownloadParams()
        if (!downloadParams.isValid()) {
            return@withContext Result.failure(workDataOf("error" to "Missing required parameters"))
        }

        progressTracker = DownloadProgressTracker(this@HlsDownloader, notificationManager)

        Timber.d("Starting HLS download: ${downloadParams.downloadId} | ${downloadParams.hlsUrl} | ${downloadParams.fileName}")

        // Set foreground if notification permission available
        if (notificationManager.hasPermission()) {
            setForeground(notificationManager.createForegroundInfo(downloadParams.fileName, "Starting HLS download...", 0))
        }

        try {
            val result = downloadHlsStream(downloadParams)
            val keys = DownloadData.Companion.Keys
            Result.success(workDataOf(
                keys.DOWNLOAD_ID to result["downloadId"],
                keys.LOCAL_PATH to result["localPath"],
                keys.FILE_NAME to result["fileName"],
                keys.FILE_SIZE to result["fileSize"],
                keys.SEGMENTS_DOWNLOADED to result["segmentsDownloaded"]
            ))
        } catch (e: Exception) {
            Timber.e(e, "HLS download failed for ${downloadParams.downloadId}")
            val keys = DownloadData.Companion.Keys
            Result.failure(workDataOf(
                keys.ERROR to (e.message ?: "Unknown HLS download error"),
                keys.DOWNLOAD_ID to downloadParams.downloadId
            ))
        }
    }

    private suspend fun downloadHlsStream(params: HlsDownloadParams): Map<String, Any> = withContext(Dispatchers.IO) {
        // Parse M3U8 playlist
        val masterPlaylist = fetchPlaylist(params.hlsUrl)
        val selectedVariant = selectBestVariant(masterPlaylist, params.quality)

        // If it's a master playlist, fetch the variant playlist
        val segmentPlaylist = if (selectedVariant != params.hlsUrl) {
            fetchPlaylist(selectedVariant)
        } else {
            masterPlaylist
        }

        val segments = parseSegments(segmentPlaylist, params.hlsUrl)
        val totalSegments = segments.size
        val downloadedBytes = AtomicLong(0)
        var totalBytes = 0L

        // Create output file using fileManager
        val (outputFile, _) = fileManager.createOrGetFile(
            params.fileName,
            "video/mp4",
            false,
            0L
        )

        // Create HTTP client with progress callback for segment downloads
        val client = httpClient.createClient { currentSegmentBytes, segmentSize ->
            // Update total progress across all segments
            val currentTotalBytes = downloadedBytes.get() + currentSegmentBytes
            kotlinx.coroutines.runBlocking {
                progressTracker.updateProgress(
                    params.downloadId,
                    params.fileName,
                    currentTotalBytes,
                    totalBytes
                )
            }
        }

        // Download segments and merge
        outputFile.openOutputStream().use { outputStream ->
            segments.forEachIndexed { index, segmentUrl ->
                if (isStopped) {
                    throw InterruptedException("Download was stopped")
                }

                // Download segment using HttpDownloadClient
                val segmentData = downloadSegmentWithProgress(client, segmentUrl)
                outputStream.write(segmentData)
                outputStream.flush()

                downloadedBytes.addAndGet(segmentData.size.toLong())
                totalBytes += segmentData.size

                // Update progress after each segment
                progressTracker.updateProgress(
                    params.downloadId,
                    params.fileName,
                    downloadedBytes.get(),
                    totalBytes
                )

                Timber.d("Downloaded segment ${index + 1}/$totalSegments for ${params.downloadId}")
            }
        }

        val finalSize = outputFile.length()
        val filePath = outputFile.uri.toString()
        val localPath = outputFile.uri.path ?: filePath

        // Final progress update
        progressTracker.updateFinalProgress(
            params.downloadId,
            params.fileName,
            filePath,
            localPath,
            finalSize
        )

        mapOf(
            "downloadId" to params.downloadId,
            "localPath" to localPath,
            "fileName" to "${params.fileName}.mp4",
            "fileSize" to finalSize,
            "segmentsDownloaded" to totalSegments
        )
    }

    private suspend fun fetchPlaylist(url: String): String = withContext(Dispatchers.IO) {
        // Use simple client for playlist fetching (no progress needed)
        val simpleClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val request = Request.Builder().url(url).build()
        val response = simpleClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to fetch playlist: HTTP ${response.code}")
        }

        response.body.string()
    }

    private suspend fun downloadSegmentWithProgress(client: OkHttpClient, url: String): ByteArray = withContext(Dispatchers.IO) {
        val request = httpClient.createRequest(url)
        val response = client.newCall(request).execute()

        if (!httpClient.validateResponse(response, false)) {
            throw IOException("Failed to download segment: HTTP ${response.code}")
        }

        response.body.bytes()
    }

    private fun selectBestVariant(masterPlaylist: String, quality: String): String {
        val lines = masterPlaylist.lines()
        val variants = mutableListOf<Pair<String, Int>>() // URL to bandwidth

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val bandwidth = extractBandwidth(line)
                if (i + 1 < lines.size && !lines[i + 1].startsWith("#")) {
                    variants.add(Pair(lines[i + 1].trim(), bandwidth))
                }
            }
            i++
        }

        if (variants.isEmpty()) {
            // Not a master playlist, return original URL
            return masterPlaylist.lines().first { !it.startsWith("#") && it.isNotBlank() }
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

    private fun parseSegments(playlist: String, baseUrl: String): List<String> {
        val baseUri = URI.create(baseUrl)
        return playlist.lines()
            .filter { !it.startsWith("#") && it.isNotBlank() }
            .map { segmentUrl ->
                if (segmentUrl.startsWith("http")) {
                    segmentUrl
                } else {
                    baseUri.resolve(segmentUrl).toString()
                }
            }
    }

    private fun extractDownloadParams(): HlsDownloadParams {
        return HlsDownloadParams(
            downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: "",
            hlsUrl = inputData.getString(KEY_HLS_URL) ?: "",
            fileName = inputData.getString(KEY_FILE_NAME) ?: "",
            quality = inputData.getString(KEY_QUALITY) ?: "default"
        )
    }

    data class HlsDownloadParams(
        val downloadId: String,
        val hlsUrl: String,
        val fileName: String,
        val quality: String
    ) {
        fun isValid(): Boolean = downloadId.isNotEmpty() && hlsUrl.isNotEmpty() && fileName.isNotEmpty()
    }
}
