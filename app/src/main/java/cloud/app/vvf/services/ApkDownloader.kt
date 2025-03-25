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
class ApkDownloader @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

  object FileParams {
    const val KEY_FILE_URL = "key_file_url"
    const val KEY_FILE_TYPE = "key_file_type"
    const val KEY_FILE_NAME = "key_file_name"
    const val KEY_FILE_URI = "key_file_uri"
    const val KEY_TAG_LAST_UPDATE = "key_tag_last_update"
  }

  object NotificationConstants {
    const val CHANNEL_NAME = "download_file_worker_channel"
    const val CHANNEL_DESCRIPTION = "download_file_worker_description"
    const val CHANNEL_ID = "download_file_worker_channel_34"
    const val NOTIFICATION_ID = 187209 //random
  }

  private val notificationBuilder =
    NotificationCompat.Builder(applicationContext, NotificationConstants.CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Downloading APK")
      .setOngoing(true)
      .setProgress(100, 0, false)

  private fun createForegroundInfo(progress: Int = 0): ForegroundInfo {
    val notification = notificationBuilder.setProgress(100, progress, false).build()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
      ForegroundInfo(
        NotificationConstants.NOTIFICATION_ID,
        notification,
        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      )
    } else { // API < 29
      ForegroundInfo(
        NotificationConstants.NOTIFICATION_ID,
        notification
      )
    }
  }

  @SuppressLint("Range")
  override suspend fun doWork(): Result {
    val fileUrl = inputData.getString(FileParams.KEY_FILE_URL) ?: ""
    val fileName = inputData.getString(FileParams.KEY_FILE_NAME) ?: ""
    val fileType = inputData.getString(FileParams.KEY_FILE_TYPE) ?: ""
    val lastUpdate = inputData.getString(FileParams.KEY_TAG_LAST_UPDATE) ?: ""

    Timber.d("doWork: $fileUrl | $fileName | $fileType | $lastUpdate")

    if (fileName.isEmpty() || fileType.isEmpty() || fileUrl.isEmpty()) {
      return Result.failure()
    }

    // Setup notification channel (Android O+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        NotificationConstants.CHANNEL_ID,
        NotificationConstants.CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = NotificationConstants.CHANNEL_DESCRIPTION
      }
      val manager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
      manager?.createNotificationChannel(channel)
    }

    // Set initial foreground info if permission is granted
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    if (hasNotificationPermission) {
      setForeground(createForegroundInfo(0))
    } else {
      Timber.w("Notification permission not granted; running without foreground service")
    }

    // Configure OkHttp client with progress interceptor
    val client = OkHttpClient.Builder()
      .addNetworkInterceptor(ProgressInterceptor { downloadedBytes, totalBytes ->
        val progress = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else 0
        // Update WorkManager progress
        setProgressAsync(workDataOf("progress" to progress))
        // Update notification progress if permission is granted
        if (hasNotificationPermission) {
          CoroutineScope(Dispatchers.Main).launch {
            setForeground(createForegroundInfo(progress))
          }
        }
      })
      .build()

    val request = Request.Builder().url(fileUrl).build()

    // Download and save file
    var finalUri: String? = null
    try {
      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        Timber.e("Download failed: HTTP ${response.code}")
        return Result.failure()
      }

      response.body.let { responseBody ->
        responseBody.byteStream().use { input ->
          val cacheDir = KUniFile.fromFile(context, context.cacheDir)
            ?: throw IOException("Failed to access cache directory")
          val apkFile =
            cacheDir.createFile("$fileName.apk", "application/vnd.android.package-archive")
              ?: throw IOException("Failed to create APK file")

          apkFile.openOutputStream().use { output ->
            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
              output.write(buffer, 0, bytesRead)
            }
            output.flush()
          }
          finalUri = apkFile.uri.toString()
        }
      }
    } catch (e: Exception) {
      Timber.e(e, "Download failed")
      return Result.failure()
    }

    return if (finalUri != null) {
      Result.success(
        workDataOf(
          FileParams.KEY_FILE_URI to finalUri,
          FileParams.KEY_TAG_LAST_UPDATE to lastUpdate
        )
      )
    } else {
      Result.failure()
    }
  }

  class ProgressInterceptor(val listener: (Long, Long) -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val originalResponse = chain.proceed(chain.request())
      return originalResponse.newBuilder()
        .body(ProgressResponseBody(originalResponse.body, listener))
        .build()
    }
  }

  class ProgressResponseBody(
    val responseBody: ResponseBody,
    val listener: (Long, Long) -> Unit
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
