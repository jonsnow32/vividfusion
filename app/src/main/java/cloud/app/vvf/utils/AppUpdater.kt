package cloud.app.vvf.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cloud.app.vvf.services.downloader.ApkDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

class AppUpdater(
  private val context: Context,
  private val client: OkHttpClient,
  private val owner: String,
  private val repo: String,
  private val currentVersion: String,
) {
  private val json: Json = Json { ignoreUnknownKeys = true }
  private val baseUrl = "https://api.github.com/repos"
  private val sharedPrefs = context.getSharedPreferences("AppUpdaterPrefs", Context.MODE_PRIVATE)
  private val SHA_KEY_PREFIX = "tag_sha_"

  @Serializable
  data class GitHubRelease(
    val tag_name: String,
    val assets: List<Asset>,
    val published_at: String
  )

  @Serializable
  data class Asset(
    val name: String,
    val browser_download_url: String
  )

  @Serializable
  data class GitRef(
    val ref: String,
    val node_id: String,
    val url: String,
    val `object`: GitObject
  )

  @Serializable
  data class GitObject(
    val sha: String,
    val type: String,
    val url: String
  )

  suspend fun checkForUpdate(
    token: String? = null
  ): GitHubRelease? = withContext(Dispatchers.IO) {
    // Fetch the latest release to get the tag_name
    val release = getLatestRelease(owner, repo, token) ?: return@withContext null
    if (isNewerVersion(release.tag_name, currentVersion))
      release
    else null

  }

  suspend fun enqueueDownload(
    release: GitHubRelease,
    assetNameRegex: Regex = ".*\\.apk$".toRegex()
  ): String = withContext(Dispatchers.IO) {
    val asset = release.assets.find { it.name.matches(assetNameRegex) }
      ?: throw IllegalStateException("No APK found in release ${release.tag_name}")

    val workManager = WorkManager.getInstance(context)
    val currentTag = APK_DOWNLOAD_WORK_TAG

    val existingWorkers = workManager.getWorkInfosByTag(currentTag).get()
    val runningWorker = existingWorkers.firstOrNull {
      it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.SUCCEEDED
    }

    if (runningWorker != null) {
      Timber.d("Worker with tag $currentTag is already running. Returning existing ID.")
      return@withContext runningWorker.id.toString()
    }

    //cancel all worker
    val inputData = Data.Builder()
      .putString(ApkDownloader.FileParams.KEY_FILE_URL, asset.browser_download_url)
      .putString(ApkDownloader.FileParams.KEY_FILE_NAME, asset.name.removeSuffix(".apk"))
      .putString(ApkDownloader.FileParams.KEY_FILE_TYPE, "apk")
      .putString(ApkDownloader.FileParams.KEY_TAG_LAST_UPDATE, release.published_at)
      .build()

    val downloadRequest = OneTimeWorkRequestBuilder<ApkDownloader>()
      .setInputData(inputData)
      .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
      .addTag(currentTag)
      .build()

    workManager.enqueue(downloadRequest)
    Timber.d("Enqueued download with ID: ${downloadRequest.id}")
    return@withContext downloadRequest.id.toString()
  }

  suspend fun getTagSha(tagName: String, token: String?): String? =
    withContext(Dispatchers.IO) {
      val url = "$baseUrl/$owner/$repo/git/refs/tags/$tagName"
      val request = Request.Builder()
        .url(url)
        .addHeader("Accept", "application/vnd.github+json")
        .addHeader("Cache-Control", "no-cache, no-store")
        .addHeader("Pragma", "no-cache")
        .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
        .build()

      return@withContext try {
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            Timber.e("Failed to fetch tag SHA: ${response.code}")
            return@use null
          }
          val body = response.body?.string() ?: throw IOException("Empty response body")
          val gitRef = json.decodeFromString<GitRef>(body)
          gitRef.`object`.sha
        }
      } catch (e: Exception) {
        Timber.e(e, "Error fetching tag SHA for $tagName")
        null
      }
    }

  private fun getLatestRelease(owner: String, repo: String, token: String?): GitHubRelease? {
    val url = "$baseUrl/$owner/$repo/releases/latest"
    val request = Request.Builder()
      .url(url)
      .addHeader("Accept", "application/vnd.github+json")
      .addHeader("Cache-Control", "no-cache, no-store")
      .addHeader("Pragma", "no-cache")
      .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
      .build()

    return try {
      val noCacheClient = client.newBuilder()
        .cache(null)
        .build()

      noCacheClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Failed to fetch release: ${response.code}")
        val body = response.body.string()

        val release = json.decodeFromString<GitHubRelease>(body)
        if (hasApkFile(release)) release else null
      }
    } catch (e: Exception) {
      Timber.e(e, "Error fetching latest release")
      null
    }
  }

  private fun isNewerVersion(latest: String, current: String): Boolean {
    val latestClean = latest.trimStart('v').split(".")
    val currentClean = current.trimStart('v').split(".")
    for (i in 0 until maxOf(latestClean.size, currentClean.size)) {
      val latestPart = latestClean.getOrNull(i)?.toIntOrNull() ?: 0
      val currentPart = currentClean.getOrNull(i)?.toIntOrNull() ?: 0
      if (latestPart > currentPart) return true
      if (latestPart < currentPart) return false
    }
    return false
  }

  private fun hasApkFile(release: GitHubRelease): Boolean {
    return release.assets.any { it.name.endsWith(".apk") }
  }

  companion object {
    const val APK_DOWNLOAD_WORK_TAG = "apk_download"
  }
}
