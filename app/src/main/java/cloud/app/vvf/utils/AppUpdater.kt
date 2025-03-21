package cloud.app.vvf.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException

class AppUpdater(
  private val context: Context,
  private val client: OkHttpClient
) {
  private val json: Json = Json { ignoreUnknownKeys = true }
  private val baseUrl = "https://api.github.com/repos"

  // Data classes for GitHub API response
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

  // Check for update and return the latest release if newer than current version
  suspend fun checkForUpdate(
    owner: String,
    repo: String,
    currentVersion: String,
    token: String? = null
  ): GitHubRelease? = withContext(Dispatchers.IO) {
    val release = getLatestRelease(owner, repo, token) ?: return@withContext null
    if (isNewerVersion(release.tag_name, currentVersion)) release else null
  }

  suspend fun downloadAndInstall(
    release: GitHubRelease,
    assetNameRegex: Regex = ".*\\.apk$".toRegex(),
    onProgress: (Long, Long) -> Unit = { _, _ -> }
  ) = withContext(Dispatchers.IO) {
    val asset = release.assets.find { it.name.matches(assetNameRegex) }
      ?: throw IllegalStateException("No APK found in release ${release.tag_name}")

    val cacheDir = KUniFile.fromFile(context, context.cacheDir)
      ?: throw IOException("Failed to access cache directory")
    val apkFile = cacheDir.createFile(asset.name, "application/vnd.android.package-archive")
      ?: throw IOException("Failed to create APK file")

    downloadFile(asset.browser_download_url, apkFile, onProgress)
    installApk(apkFile) // Back to launching directly
  }
  private fun downloadFile(url: String, kUniFile: KUniFile, onProgress: (Long, Long) -> Unit) {
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Failed to download file: ${response.code}")
      val totalBytes = response.body?.contentLength() ?: -1L
      var downloadedBytes = 0L

      Timber.d("Starting download: totalBytes=$totalBytes")
      response.body?.byteStream()?.use { input ->
        kUniFile.openOutputStream().use { output ->
          val buffer = ByteArray(1024)
          var bytesRead: Int
          while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            downloadedBytes += bytesRead
            Timber.d("Download progress: $downloadedBytes / $totalBytes")
            onProgress(downloadedBytes, totalBytes)
            output.flush()
          }
          Timber.d("Download complete: $downloadedBytes / $totalBytes")
          if (totalBytes == -1L) {
            onProgress(downloadedBytes, downloadedBytes)
          }
        }
      } ?: throw IOException("Empty response body")
    }
  }


  private fun getLatestRelease(owner: String, repo: String, token: String?): GitHubRelease? {
    val url = "$baseUrl/$owner/$repo/releases/latest"
    val request = Request.Builder()
      .url(url)
      .addHeader("Accept", "application/vnd.github+json")
      .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
      .build()

    return try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Failed to fetch release: ${response.code}")
        val body = response.body?.string() ?: throw IOException("Empty response body")
        json.decodeFromString<GitHubRelease>(body)
      }
    } catch (e: Exception) {
      println("Error fetching latest release: ${e.message}")
      null
    }
  }



  private fun installApk(apkFile: KUniFile) {
    val apkUri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      File(apkFile.filePath ?: throw IOException("APK file path unavailable"))
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(apkUri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  }

  // Compare version strings (e.g., "v1.0.0" vs "1.0.0")
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
}
