package cloud.app.vvf.services.downloader

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.app.vvf.R
import cloud.app.vvf.utils.KUniFile
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

    object HlsDownloadParams {
        const val KEY_DOWNLOAD_ID = "key_download_id"
        const val KEY_HLS_URL = "key_hls_url"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_QUALITY = "key_quality"
    }

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(HlsDownloadParams.KEY_DOWNLOAD_ID) ?: ""
        val hlsUrl = inputData.getString(HlsDownloadParams.KEY_HLS_URL) ?: ""
        val fileName = inputData.getString(HlsDownloadParams.KEY_FILE_NAME) ?: ""
        val quality = inputData.getString(HlsDownloadParams.KEY_QUALITY) ?: "default"

        Timber.d("Starting HLS download: $downloadId | $hlsUrl | $fileName")

        if (downloadId.isEmpty() || hlsUrl.isEmpty() || fileName.isEmpty()) {
            return@withContext Result.failure(workDataOf("error" to "Missing required parameters"))
        }

        try {
            // Set up foreground service for long-running download
            setForeground(createForegroundInfo(fileName))

            val result = downloadHlsStream(hlsUrl, fileName, downloadId, quality)
            Result.success(workDataOf(
                "downloadId" to result["downloadId"],
                "localPath" to result["localPath"],
                "fileName" to result["fileName"],
                "fileSize" to result["fileSize"],
                "segmentsDownloaded" to result["segmentsDownloaded"]
            ))
        } catch (e: Exception) {
            Timber.e(e, "HLS download failed for $downloadId")
            Result.failure(workDataOf(
                "error" to (e.message ?: "Unknown HLS download error"),
                "downloadId" to downloadId
            ))
        }
    }

    private suspend fun downloadHlsStream(
        hlsUrl: String,
        fileName: String,
        downloadId: String,
        quality: String
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        // Parse M3U8 playlist
        val masterPlaylist = fetchPlaylist(hlsUrl)
        val selectedVariant = selectBestVariant(masterPlaylist, quality)

        // If it's a master playlist, fetch the variant playlist
        val segmentPlaylist = if (selectedVariant != hlsUrl) {
            fetchPlaylist(selectedVariant)
        } else {
            masterPlaylist
        }

        val segments = parseSegments(segmentPlaylist, hlsUrl)
        val totalSegments = segments.size
        val downloadedBytes = AtomicLong(0)
        var totalBytes = 0L

        // Create output file
        val downloadsDir = getDownloadsDirectory()
        val outputFile = downloadsDir.createFile("$fileName.mp4", "video/mp4")
            ?: throw IOException("Failed to create output file")

        // Download segments and merge
        outputFile.openOutputStream().use { outputStream ->
            segments.forEachIndexed { index, segmentUrl ->
                val segmentData = downloadSegment(segmentUrl)
                outputStream.write(segmentData)
                outputStream.flush()

                downloadedBytes.addAndGet(segmentData.size.toLong())
                totalBytes += segmentData.size

                val progress = ((index + 1) * 100 / totalSegments)

                // Update progress
                setProgressAsync(workDataOf(
                    "progress" to progress,
                    "downloadedBytes" to downloadedBytes.get(),
                    "totalBytes" to totalBytes,
                    "downloadId" to downloadId,
                    "segmentsDownloaded" to (index + 1),
                    "totalSegments" to totalSegments
                ))

                Timber.d("Downloaded segment ${index + 1}/$totalSegments for $downloadId")
            }
        }

        mapOf(
            "downloadId" to downloadId,
            "localPath" to outputFile.uri.toString(),
            "fileName" to "$fileName.mp4",
            "fileSize" to totalBytes,
            "segmentsDownloaded" to totalSegments
        )
    }

    private suspend fun fetchPlaylist(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to fetch playlist: HTTP ${response.code}")
        }

        response.body?.string() ?: throw IOException("Empty playlist response")
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

    private suspend fun downloadSegment(url: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to download segment: HTTP ${response.code}")
        }

        response.body?.bytes() ?: throw IOException("Empty segment response")
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
            "Downloading HLS stream...",
            0
        )
    }
}
