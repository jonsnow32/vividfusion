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
import javax.inject.Inject

@HiltWorker
class TorrentDownloader @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

  @Inject
  lateinit var torrentManager: TorrentManager

  companion object {
    const val KEY_DOWNLOAD_ID = "key_download_id"
    const val KEY_TORRENT_URL = "key_torrent_url"
    const val KEY_MAGNET_LINK = "key_magnet_link"
    const val KEY_DOWNLOAD_PATH = "key_download_path"
  }

  private val notificationManager = DownloadNotificationManager(context)
  private val fileManager = DownloadFileManager(context)
  private val isDownloadActive = AtomicBoolean(true)


  private var currentTorrentStatus : TorrentStatus? = null


  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val downloadParams = extractDownloadParams()
    if (!downloadParams.isValid()) {
      return@withContext Result.failure(workDataOf("error" to "Missing required parameters"))
    }

    Timber.d("Starting torrent download: ${downloadParams.downloadId} | ${downloadParams.torrentUrl} | ${downloadParams.magnetLink}")


    notificationManager.updateNotification(
      this@TorrentDownloader,
      downloadParams.downloadId,
      0
    )


    try {
      val result = when {
        downloadParams.magnetLink.isNotEmpty() -> downloadFromTorrentLink(
          downloadParams.magnetLink,
          downloadParams
        )

        downloadParams.torrentUrl.isNotEmpty() -> downloadFromTorrentLink(
          downloadParams.torrentUrl,
          downloadParams
        )

        else -> throw IllegalArgumentException("No valid torrent source provided")
      }
      val keys = DownloadData.Companion.Keys
      Result.success(
        workDataOf(
          keys.DOWNLOAD_ID to result[keys.DOWNLOAD_ID],
          keys.FILE_PATH to result[keys.FILE_PATH],
          keys.DISPLAY_NAME to result[keys.DISPLAY_NAME],
          keys.FILE_SIZE to result[keys.FILE_SIZE],
          keys.STREAM_URL to (result[keys.STREAM_URL] ?: ""),
          keys.NOTE to result[keys.NOTE]
        )
      )
    } catch (e: Exception) {
      Timber.e(e, "Torrent download failed for ${downloadParams.downloadId}")
      val keys = DownloadData.Companion.Keys
      Result.failure(
        workDataOf(
          keys.ERROR to (e.message ?: "Unknown torrent download error"),
          keys.DOWNLOAD_ID to downloadParams.downloadId
        )
      )
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
      return@withContext downloadFromStreamUrl(streamUrl, params)

    } catch (e: Exception) {
      Timber.w(e, "TorrentManager transform failed, reporting error to UI")
      // Update progress to UI with error state
      setProgressAsync(
        workDataOf(
          DownloadData.Companion.Keys.ERROR to (e.message ?: "TorrentManager transform failed"),
          DownloadData.Companion.Keys.DOWNLOAD_ID to params.downloadId
        )
      )
      throw e // propagate error to be handled by doWork
    }
  }

  private fun monitorTorrentProgress(downloadId: String, torrentStatus: TorrentStatus) {

    currentTorrentStatus = torrentStatus
    // Monitor torrent status in background using proper coroutine scope
    CoroutineScope(Dispatchers.IO).launch {
      while (isDownloadActive.get() && isActive && !isStopped) {
        try {
          // Get real torrent status from TorrentManager
          val status = torrentStatus.hash?.let { torrentManager.get(it) }
          if (currentTorrentStatus != null) {
            currentTorrentStatus = status
          }
          // Update every 3 seconds
          delay(1000)
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
  ): Map<String, Any> = withContext(Dispatchers.IO) {
    Timber.d("Downloading from stream URL: $streamUrl")

    // Create output file using fileManager
    val (outputFile, _) = fileManager.createOrGetFile(
      params.downloadId,
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

          // Update progress directly
          updateProgress(
            params.downloadId,
            currentTorrentStatus?.name ?: params.downloadId,
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
    val displayName = currentTorrentStatus?.name ?: params.downloadId
    // Final progress update
    updateFinalProgress(
      params.downloadId,
      displayName,
      filePath,
      localPath,
      actualFileSize
    )

    val keys = DownloadData.Companion.Keys
    mapOf(
      keys.DISPLAY_NAME to displayName,
      keys.DOWNLOAD_ID to params.downloadId,
      keys.FILE_PATH to localPath,
      keys.FILE_SIZE to actualFileSize,
      keys.STREAM_URL to streamUrl,
      keys.NOTE to "Downloaded via TorrentManager stream URL"
    )
  }

  private var lastProgressUpdateTime = 0L

  private suspend fun updateProgress(
    downloadId: String,
    displayName: String,
    downloadedBytes: Long,
    totalBytes: Long
  ) {
    val now = System.currentTimeMillis()
    if (now - lastProgressUpdateTime < 1000) return // Only update once per second
    lastProgressUpdateTime = now

    val progress = if (totalBytes > 0) {
      ((downloadedBytes * 100) / totalBytes).toInt()
    } else 0

    val keys = DownloadData.Companion.Keys
    setProgressAsync(
      workDataOf(
        keys.PROGRESS to progress,
        keys.DOWNLOADED_BYTES to downloadedBytes,
        keys.TOTAL_BYTES to totalBytes,
        keys.DOWNLOAD_ID to downloadId,
        keys.DISPLAY_NAME to displayName,
        keys.PEERS to (currentTorrentStatus?.totalPeers ?: 0),
        keys.SEEDS to (currentTorrentStatus?.connectedSeeders ?: 0),
        keys.UPLOAD_SPEED to (currentTorrentStatus?.uploadSpeed?.toLong() ?: 0L),
        keys.DOWNLOAD_SPEED to (currentTorrentStatus?.downloadSpeed?.toLong() ?: 0L),
      )
    )

    // Update notification using optimized notification manager
    notificationManager.updateNotification(
      this,
      downloadId,
      displayName,
      progress,
      DownloadStatus.DOWNLOADING,
      "Downloading... $progress% • ${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}"
    )
  }

  private suspend fun updateFinalProgress(
    downloadId: String,
    displayName: String,
    filePath: String,
    localPath: String,
    fileSize: Long
  ) {
    val keys = DownloadData.Companion.Keys
    setProgressAsync(
      workDataOf(
        keys.PROGRESS to 100,
        keys.DOWNLOADED_BYTES to fileSize,
        keys.TOTAL_BYTES to fileSize,
        keys.DOWNLOAD_ID to downloadId,
        keys.DISPLAY_NAME to displayName,
        keys.FILE_PATH to localPath,
        keys.FILE_PATH to filePath,
        keys.FILE_SIZE to fileSize
      )
    )

    // Show completion notification
    notificationManager.showCompletionNotification(downloadId, displayName, localPath)
  }

  private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return String.format(
      java.util.Locale.getDefault(),
      "%.1f %s",
      bytes / Math.pow(1024.0, digitGroups.toDouble()),
      units[digitGroups]
    )
  }

  private fun extractDownloadParams(): TorrentDownloadParams {
    return TorrentDownloadParams(
      downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: "",
      torrentUrl = inputData.getString(KEY_TORRENT_URL) ?: "",
      magnetLink = inputData.getString(KEY_MAGNET_LINK) ?: "",
      downloadPath = inputData.getString(KEY_DOWNLOAD_PATH) ?: ""
    )
  }

  data class TorrentDownloadParams(
    val downloadId: String,
    val torrentUrl: String,
    val magnetLink: String,
    val downloadPath: String
  ) {
    fun isValid(): Boolean = downloadId.isNotEmpty() &&
      (torrentUrl.isNotEmpty() || magnetLink.isNotEmpty())
  }
}
