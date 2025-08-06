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
        // Resume parameters
        const val KEY_RESUME_PROGRESS = "resume_progress"
        const val KEY_RESUME_BYTES = "resume_bytes"
        const val KEY_RESUME_FROM_PAUSE = "resume_from_pause"
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
          // Ensure notification channel is created first
            createNotificationChannel(context)

            val notificationBuilder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Downloading: $fileName")
                .setContentText(subtitle)
                .setOngoing(true)
                .setAutoCancel(false)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, progress, progress == 0)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

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

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NotificationConstants.CHANNEL_ID,
                    NotificationConstants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = NotificationConstants.CHANNEL_DESCRIPTION
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                manager?.createNotificationChannel(channel)
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

        // Get resume parameters
        val resumeProgress = inputData.getInt(DownloadParams.KEY_RESUME_PROGRESS, 0)
        val resumeBytes = inputData.getLong(DownloadParams.KEY_RESUME_BYTES, 0L)
        val isResuming = inputData.getBoolean(DownloadParams.KEY_RESUME_FROM_PAUSE, false)

        Timber.d("Starting download: $downloadId | $downloadUrl | $fileName | Type: $downloadType | Resume: $isResuming (${resumeProgress}%, ${resumeBytes} bytes)")

        if (downloadId.isEmpty() || downloadUrl.isEmpty() || fileName.isEmpty()) {
            return Result.failure(workDataOf("error" to "Missing required parameters"))
        }

        // Setup notification channel
        setupNotificationChannel()

        // Check notification permission and set foreground if available
        val hasNotificationPermission = checkNotificationPermission()
        if (hasNotificationPermission) {
            val statusText = if (isResuming) "Resuming download..." else "Starting download..."
            setForeground(createDownloadForegroundInfo(context, fileName, statusText, resumeProgress))
        }

        // Route to appropriate downloader based on type
        return when (downloadType.uppercase()) {
            "HLS" -> {
                // For HLS, we could delegate to HlsDownloader or handle inline
                downloadHlsInline(downloadUrl, fileName, downloadId)
            }
            "HTTP", "HTTPS" -> {
                downloadHttpFile(downloadUrl, fileName, downloadId, hasNotificationPermission, resumeBytes, isResuming)
            }
            else -> {
                downloadHttpFile(downloadUrl, fileName, downloadId, hasNotificationPermission, resumeBytes, isResuming)
            }
        }
    }

    private suspend fun downloadHttpFile(
        downloadUrl: String,
        fileName: String,
        downloadId: String,
        hasNotificationPermission: Boolean,
        resumeBytes: Long,
        isResuming: Boolean
    ): Result {
        // Track last progress update to avoid too frequent updates
        var lastProgressUpdate = 0
        var lastNotificationUpdate = 0L

        // For resume, we need different progress calculation
        val progressCalculator = if (isResuming && resumeBytes > 0) {
            // Create a progress interceptor that accounts for already downloaded bytes
            ProgressInterceptor { downloadedInSession, remainingBytes ->
                val totalDownloaded = resumeBytes + downloadedInSession
                val totalFileSize = resumeBytes + remainingBytes
                val progress = if (totalFileSize > 0) {
                    (totalDownloaded * 100 / totalFileSize).toInt()
                } else 0

                // Only update if progress changed significantly (at least 1%)
                if (progress != lastProgressUpdate) {
                    lastProgressUpdate = progress

                    // Update WorkManager progress with correct values
                    setProgressAsync(workDataOf(
                        "progress" to progress,
                        "downloadedBytes" to totalDownloaded,
                        "totalBytes" to totalFileSize,
                        "downloadId" to downloadId
                    ))

                    // Update notification less frequently (every 2 seconds or 5% progress change)
                    val currentTime = System.currentTimeMillis()
                    if (hasNotificationPermission &&
                        (currentTime - lastNotificationUpdate > 2000 || progress % 5 == 0)) {
                        lastNotificationUpdate = currentTime
                        CoroutineScope(Dispatchers.Main).launch {
                            setForeground(createDownloadForegroundInfo(context, fileName, "Downloading...", progress))
                        }
                    }
                }
            }
        } else {
            // Normal progress interceptor for fresh downloads
            ProgressInterceptor { downloadedBytes, totalBytes ->
                val progress = if (totalBytes > 0) {
                    (downloadedBytes * 100 / totalBytes).toInt()
                } else 0

                // Only update if progress changed significantly (at least 1%)
                if (progress != lastProgressUpdate) {
                    lastProgressUpdate = progress

                    // Update WorkManager progress
                    setProgressAsync(workDataOf(
                        "progress" to progress,
                        "downloadedBytes" to downloadedBytes,
                        "totalBytes" to totalBytes,
                        "downloadId" to downloadId
                    ))

                    // Update notification less frequently (every 2 seconds or 5% progress change)
                    val currentTime = System.currentTimeMillis()
                    if (hasNotificationPermission &&
                        (currentTime - lastNotificationUpdate > 2000 || progress % 5 == 0)) {
                        lastNotificationUpdate = currentTime
                        CoroutineScope(Dispatchers.Main).launch {
                            setForeground(createDownloadForegroundInfo(context, fileName, "Downloading...", progress))
                        }
                    }
                }
            }
        }

        // Configure OkHttp client with appropriate progress tracking
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(progressCalculator)
            .build()

        return try {
            downloadFile(client, downloadUrl, fileName, downloadId, resumeBytes, isResuming)
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

    private suspend fun downloadFile(
        client: OkHttpClient,
        url: String,
        fileName: String,
        downloadId: String,
        resumeBytes: Long = 0L,
        isResuming: Boolean = false
    ): Result {
        // Check if work is stopped before starting
        if (isStopped) {
            Timber.d("Download $downloadId: Work is stopped, returning early")
            return Result.failure(workDataOf(
                "error" to "Download was stopped",
                "downloadId" to downloadId
            ))
        }

        val requestBuilder = Request.Builder().url(url)

        // Add Range header for resuming downloads
        if (isResuming && resumeBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$resumeBytes-")
            Timber.d("Download $downloadId: Resuming from byte $resumeBytes")
        }

        val request = requestBuilder.build()
        val response = client.newCall(request).execute()

        // Check if work is stopped after HTTP request
        if (isStopped) {
            response.close()
            Timber.d("Download $downloadId: Work stopped after HTTP request")
            return Result.failure(workDataOf(
                "error" to "Download was stopped",
                "downloadId" to downloadId
            ))
        }

        // For resume requests, expect 206 Partial Content or 200 OK
        // Some servers might return 200 OK even for range requests
        if (!response.isSuccessful) {
            if (isResuming && response.code == 416) {
                // Range not satisfiable - file might be already complete
                Timber.w("Download $downloadId: Range not satisfiable, file might be complete")
                return Result.success(workDataOf("downloadId" to downloadId))
            }
            return Result.failure(workDataOf(
                "error" to "HTTP ${response.code}",
                "downloadId" to downloadId
            ))
        }

        val responseBody = response.body
        if (responseBody == null) {
            return Result.failure(workDataOf(
                "error" to "Empty response body",
                "downloadId" to downloadId
            ))
        }

        // Determine file extension based on media type
        val fileExtension = getFileExtension(fileName, response)
        val fullFileName = if (fileName.contains(".")) fileName else "$fileName$fileExtension"

        // Create downloads directory
        val downloadsDir = getDownloadsDirectory()

        // Handle file creation/append for resume
        val mediaFile = if (isResuming && resumeBytes > 0) {
            // Try to find existing file for resume
            try {
                val existingFile = downloadsDir.findFile(fullFileName)
                if (existingFile != null) {
                    val existingSize = existingFile.length()
                    Timber.d("Download $downloadId: Found existing file, size: $existingSize, expected resume from: $resumeBytes")

                    if (existingSize >= resumeBytes) {
                        // File exists and has expected size or more
                        if (existingSize > resumeBytes) {
                            Timber.w("Download $downloadId: Existing file is larger than expected resume point, truncating to $resumeBytes")
                            // Truncate file to resume point if it's larger
                            truncateFile(existingFile, resumeBytes)
                        }
                        existingFile
                    } else {
                        Timber.w("Download $downloadId: Existing file is smaller than resume point, starting fresh")
                        // Delete corrupted file and start fresh
                        existingFile.delete()
                        responseBody.contentType()?.toString()
                            ?.let { downloadsDir.createFile(fullFileName, it) }
                            ?: throw IOException("Failed to create media file")
                    }
                } else {
                    Timber.w("Download $downloadId: No existing file found for resume, starting fresh")
                    responseBody.contentType()?.toString()
                        ?.let { downloadsDir.createFile(fullFileName, it) }
                        ?: throw IOException("Failed to create media file")
                }
            } catch (e: Exception) {
                Timber.w(e, "Download $downloadId: Error accessing existing file, creating new one")
                responseBody.contentType()?.toString()
                    ?.let { downloadsDir.createFile(fullFileName, it) }
                    ?: throw IOException("Failed to create media file")
            }
        } else {
            // Create new file for fresh download
            responseBody.contentType()?.toString()
                ?.let { downloadsDir.createFile(fullFileName, it) }
                ?: throw IOException("Failed to create media file")
        }

        // Calculate total file size
        val contentLength = responseBody.contentLength()
        val totalBytes = if (isResuming && resumeBytes > 0 && response.code == 206) {
            // For partial content (206), total size = resume point + remaining content
            resumeBytes + contentLength
        } else if (response.code == 200) {
            // For full content (200), total size = content length
            contentLength
        } else {
            contentLength
        }

        Timber.d("Download $downloadId: Starting file write - Response code: ${response.code}, Content length: $contentLength, Total bytes: $totalBytes, Resume from: $resumeBytes")

        // Download the remaining content with cancellation checks
        var totalBytesWritten = 0L
        var shouldAppend = false
        try {
            responseBody.byteStream().use { input ->
                // For resume, we need to append to existing file
                shouldAppend = isResuming && resumeBytes > 0 && response.code == 206

                if (shouldAppend) {
                    mediaFile.openOutputStream(true).use { output -> // true for append mode
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var iterationCount = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // Check for cancellation every few iterations to be responsive
                            if (iterationCount % 100 == 0 && isStopped) {
                                Timber.d("Download $downloadId: Work stopped during file write (resume mode)")
                                return Result.failure(workDataOf(
                                    "error" to "Download was stopped",
                                    "downloadId" to downloadId
                                ))
                            }

                            output.write(buffer, 0, bytesRead)
                            totalBytesWritten += bytesRead
                            iterationCount++

                            // Update progress more frequently for better UX
                            if (iterationCount % 50 == 0) {
                                val actualTotal = resumeBytes + totalBytesWritten
                                val progressPercent = if (totalBytes > 0) (actualTotal * 100 / totalBytes).toInt() else 0

                                // Update WorkManager progress
                                setProgressAsync(workDataOf(
                                    "progress" to progressPercent,
                                    "downloadedBytes" to actualTotal,
                                    "totalBytes" to totalBytes,
                                    "downloadId" to downloadId
                                ))
                            }
                        }
                        output.flush()
                    }
                } else {
                    // Fresh download or server doesn't support range requests
                    mediaFile.openOutputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var iterationCount = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // Check for cancellation every few iterations to be responsive
                            if (iterationCount % 100 == 0 && isStopped) {
                                Timber.d("Download $downloadId: Work stopped during file write (normal mode)")
                                return Result.failure(workDataOf(
                                    "error" to "Download was stopped",
                                    "downloadId" to downloadId
                                ))
                            }

                            output.write(buffer, 0, bytesRead)
                            totalBytesWritten += bytesRead
                            iterationCount++

                            // Update progress more frequently for better UX
                            if (iterationCount % 50 == 0) {
                                val progressPercent = if (totalBytes > 0) (totalBytesWritten * 100 / totalBytes).toInt() else 0

                                // Update WorkManager progress
                                setProgressAsync(workDataOf(
                                    "progress" to progressPercent,
                                    "downloadedBytes" to totalBytesWritten,
                                    "totalBytes" to totalBytes,
                                    "downloadId" to downloadId
                                ))
                            }
                        }
                        output.flush()
                    }
                }
            }

            val finalSize = mediaFile.length()
            val expectedSize = if (shouldAppend) resumeBytes + totalBytesWritten else totalBytesWritten

            Timber.d("Download $downloadId: File write completed - Final size: $finalSize, Expected: $expectedSize, Written: $totalBytesWritten")

            // Final progress update with correct file path
            val filePath = mediaFile.uri.toString()
            val localPath = mediaFile.uri.path ?: filePath

            setProgressAsync(workDataOf(
                "progress" to 100,
                "downloadedBytes" to finalSize,
                "totalBytes" to finalSize,
                "downloadId" to downloadId,
                "filePath" to filePath,
                "localPath" to localPath
            ))

            return Result.success(workDataOf(
                "downloadId" to downloadId,
                "filePath" to filePath,
                "localPath" to localPath,
                "fileSize" to finalSize
            ))

        } catch (e: Exception) {
            Timber.e(e, "Download $downloadId: Error during file write")
            return Result.failure(workDataOf(
                "error" to (e.message ?: "File write error"),
                "downloadId" to downloadId
            ))
        } finally {
            responseBody.close()
        }
    }

    /**
     * Truncate file to specified size
     */
    private fun truncateFile(file: cloud.app.vvf.utils.KUniFile, targetSize: Long) {
        try {
            // For KUniFile, we need to read the content up to targetSize and rewrite
            val tempBuffer = ByteArray(targetSize.toInt())
            var bytesRead = 0

            file.openInputStream().use { input ->
                bytesRead = input.read(tempBuffer, 0, targetSize.toInt())
            }

            if (bytesRead > 0) {
                file.openOutputStream().use { output ->
                    output.write(tempBuffer, 0, bytesRead)
                    output.flush()
                }
            }

            Timber.d("Truncated file to $targetSize bytes")
        } catch (e: Exception) {
            Timber.e(e, "Failed to truncate file")
            throw e
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConstants.CHANNEL_ID,
                NotificationConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = NotificationConstants.CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
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
        return try {
            // For Android 10+ (API 29+), use MediaStore or app-specific directory
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use app-specific external directory (doesn't require WRITE_EXTERNAL_STORAGE permission)
                val appSpecificDownloads = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (appSpecificDownloads != null) {
                    Timber.d("Using app-specific Downloads directory: ${appSpecificDownloads.absolutePath}")
                    return KUniFile.fromFile(context, appSpecificDownloads)
                        ?: throw IOException("Failed to create KUniFile from app-specific directory")
                }
            }

            // For older Android versions or fallback, try public downloads if we have permission
            if (hasWriteExternalStoragePermission()) {
                val publicDownloads = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                if (publicDownloads != null && (publicDownloads.exists() || publicDownloads.mkdirs())) {
                    Timber.d("Using public Downloads directory: ${publicDownloads.absolutePath}")
                    return KUniFile.fromFile(context, publicDownloads)
                        ?: throw IOException("Failed to create KUniFile from public directory")
                }
            }

            // Final fallback: use internal app directory
            val internalDownloads = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir

            Timber.d("Using fallback directory: ${internalDownloads.absolutePath}")
            KUniFile.fromFile(context, internalDownloads)
                ?: throw IOException("Failed to create KUniFile from fallback directory")

        } catch (e: Exception) {
            Timber.e(e, "Error accessing downloads directory, using internal storage")
            // Last resort: use internal files directory
            val internalDir = context.filesDir
            KUniFile.fromFile(context, internalDir)
                ?: throw IOException("Cannot access any storage directory")
        }
    }

    private fun hasWriteExternalStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ doesn't need WRITE_EXTERNAL_STORAGE for app-specific directories
            true
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
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
