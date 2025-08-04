package cloud.app.vvf.services.downloader

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.app.vvf.R
import cloud.app.vvf.utils.KUniFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import timber.log.Timber
import java.io.IOException

@HiltWorker
class MediaDownloader @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    object DownloadParams {
        const val KEY_DOWNLOAD_ID = "key_download_id"
        const val KEY_DOWNLOAD_URL = "key_download_url"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_QUALITY = "key_quality"
        const val KEY_DOWNLOAD_TYPE = "key_download_type"
    }

    object NotificationConstants {
        const val CHANNEL_NAME = "media_download_channel"
        const val CHANNEL_DESCRIPTION = "Media download notifications"
        const val CHANNEL_ID = "media_download_channel_1"
        const val NOTIFICATION_ID = 187210
    }

    companion object {
        fun createDownloadForegroundInfo(
            context: Context,
            fileName: String,
            subtitle: String = "Downloading media...",
            progress: Int = 0
        ): ForegroundInfo {
            val notificationBuilder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(fileName)
                .setContentText(subtitle)
                .setOngoing(true)
                .setProgress(100, progress, progress == 0)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NotificationConstants.NOTIFICATION_ID,
                    notificationBuilder.build(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(NotificationConstants.NOTIFICATION_ID, notificationBuilder.build())
            }
        }
    }

    private val notificationBuilder =
        NotificationCompat.Builder(applicationContext, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading Media")
            .setOngoing(true)
            .setProgress(100, 0, false)

    private fun createForegroundInfo(fileName: String, progress: Int = 0): ForegroundInfo {
        val notification = notificationBuilder
            .setContentText(fileName)
            .setProgress(100, progress, false)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
              NotificationConstants.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationConstants.NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("Range")
    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(DownloadParams.KEY_DOWNLOAD_ID) ?: ""
        val downloadUrl = inputData.getString(DownloadParams.KEY_DOWNLOAD_URL) ?: ""
        val fileName = inputData.getString(DownloadParams.KEY_FILE_NAME) ?: ""
        val downloadType = inputData.getString(DownloadParams.KEY_DOWNLOAD_TYPE) ?: "HTTP"

        Timber.d("Starting download: $downloadId | $downloadUrl | $fileName | Type: $downloadType")

        if (downloadId.isEmpty() || downloadUrl.isEmpty() || fileName.isEmpty()) {
            return Result.failure(workDataOf("error" to "Missing required parameters"))
        }

        // Setup notification channel
        setupNotificationChannel()

        // Check notification permission and set foreground if available
        val hasNotificationPermission = checkNotificationPermission()
        if (hasNotificationPermission) {
            setForeground(createDownloadForegroundInfo(context, fileName, "Starting download...", 0))
        }

        // Route to appropriate downloader based on type
        return when (downloadType.uppercase()) {
            "HLS" -> {
                // For HLS, we could delegate to HlsDownloader or handle inline
                downloadHlsInline(downloadUrl, fileName, downloadId)
            }
            "HTTP", "HTTPS" -> {
                downloadHttpFile(downloadUrl, fileName, downloadId, hasNotificationPermission)
            }
            else -> {
                downloadHttpFile(downloadUrl, fileName, downloadId, hasNotificationPermission)
            }
        }
    }

    private suspend fun downloadHttpFile(
        downloadUrl: String,
        fileName: String,
        downloadId: String,
        hasNotificationPermission: Boolean
    ): Result {
        // Configure OkHttp client with progress tracking
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(ProgressInterceptor { downloadedBytes, totalBytes ->
                val progress = if (totalBytes > 0) {
                    (downloadedBytes * 100 / totalBytes).toInt()
                } else 0

                // Update WorkManager progress
                setProgressAsync(workDataOf(
                    "progress" to progress,
                    "downloadedBytes" to downloadedBytes,
                    "totalBytes" to totalBytes,
                    "downloadId" to downloadId
                ))

                // Update notification if permission granted
                if (hasNotificationPermission) {
                    CoroutineScope(Dispatchers.Main).launch {
                        setForeground(createDownloadForegroundInfo(context, fileName, "Downloading...", progress))
                    }
                }
            })
            .build()

        return try {
            downloadFile(client, downloadUrl, fileName, downloadId)
        } catch (e: Exception) {
            Timber.e(e, "Download failed for $downloadId")
            Result.failure(workDataOf(
                "error" to (e.message ?: "Unknown error"),
                "downloadId" to downloadId
            ))
        }
    }

    private suspend fun downloadHlsInline(
        hlsUrl: String,
        fileName: String,
        downloadId: String
    ): Result {
        // Basic HLS support - for full HLS support, use HlsDownloader
        return try {
            // For now, treat as regular HTTP download
            // In production, you'd want to use the dedicated HlsDownloader
            val client = OkHttpClient()
            downloadFile(client, hlsUrl, fileName, downloadId)
        } catch (e: Exception) {
            Timber.e(e, "HLS download failed for $downloadId")
            Result.failure(workDataOf(
                "error" to (e.message ?: "HLS download error"),
                "downloadId" to downloadId
            ))
        }
    }

    private fun downloadFile(
        client: OkHttpClient,
        url: String,
        fileName: String,
        downloadId: String
    ): Result {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            return Result.failure(workDataOf(
                "error" to "HTTP ${response.code}",
                "downloadId" to downloadId
            ))
        }

        val responseBody = response.body

        // Determine file extension based on media type
        val fileExtension = getFileExtension(fileName, response)
        val fullFileName = if (fileName.contains(".")) fileName else "$fileName$fileExtension"

        // Create downloads directory
        val downloadsDir = getDownloadsDirectory()
        val mediaFile = responseBody.contentType()?.toString()
            ?.let { downloadsDir.createFile(fullFileName, it) }
            ?: throw IOException("Failed to create media file")

        // Download the file
        responseBody.byteStream().use { input ->
            mediaFile.openOutputStream().use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
        }

        return Result.success(workDataOf(
            "downloadId" to downloadId,
            "localPath" to mediaFile.uri.toString(),
            "fileName" to fullFileName,
            "fileSize" to responseBody.contentLength()
        ))
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
              NotificationConstants.CHANNEL_ID,
              NotificationConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = NotificationConstants.CHANNEL_DESCRIPTION
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            manager?.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun getDownloadsDirectory(): KUniFile {
        // Try to use public Downloads directory, fallback to app-specific directory
        val publicDownloads = KUniFile.fromFile(context,
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
        )

        return publicDownloads ?: KUniFile.fromFile(context,
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        ) ?: throw IOException("Cannot access downloads directory")
    }

    private fun getFileExtension(fileName: String, response: Response): String {
        if (fileName.contains(".")) return ""

        return when (response.body.contentType()?.toString()) {
            "video/mp4" -> ".mp4"
            "video/webm" -> ".webm"
            "video/x-matroska" -> ".mkv"
            "audio/mpeg" -> ".mp3"
            "audio/mp4" -> ".m4a"
            "audio/webm" -> ".webm"
            else -> ".mp4" // Default for video content
        }
    }

    class ProgressInterceptor(private val listener: (Long, Long) -> Unit) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalResponse = chain.proceed(chain.request())
            return originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body, listener))
                .build()
        }
    }

    class ProgressResponseBody(
        private val responseBody: ResponseBody,
        private val listener: (Long, Long) -> Unit
    ) : ResponseBody() {
        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? = responseBody.contentType()
        override fun contentLength(): Long = responseBody.contentLength()

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = source(responseBody.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                private var totalBytesRead = 0L

                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                    listener(totalBytesRead, responseBody.contentLength())
                    return bytesRead
                }
            }
        }
    }
}
