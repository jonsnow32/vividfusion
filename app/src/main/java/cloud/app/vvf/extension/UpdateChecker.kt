package cloud.app.vvf.extension

import android.content.Context
import cloud.app.vvf.datastore.DataStore.Companion.getTempApkDir
import cloud.app.vvf.utils.toData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.use
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

suspend fun <T> runIOCatching(
  block: suspend () -> T
) = withContext(Dispatchers.IO) {
  runCatching {
    block()
  }
}

suspend fun downloadUpdate(
  context: Context,
  url: String,
  client: OkHttpClient
) = runIOCatching {
  val request = Request.Builder().url(url).build()
  val response = client.newCall(request).execute()
  val fileName = getFileNameFromResponse(response) ?: "temp.bin" // Fallback to "temp.apk"
  val file = File.createTempFile("temp", fileName, context.getTempApkDir())
  response.body.byteStream().use { input ->
    file.outputStream().use { output -> input.copyTo(output) }
  }
  file
}

private fun getFileNameFromResponse(response: Response): String? {
  val contentDisposition = response.header("Content-Disposition") ?: return null

  // Try to extract filename with quotes
  val quotedRegex = Regex("filename\\s*=\\s*\"(.*?)\"")
  val quotedMatch = quotedRegex.find(contentDisposition)
  if (quotedMatch != null) {
    return quotedMatch.groupValues[1]
  }

  // Try to extract unquoted filename
  val unquotedRegex = Regex("filename\\s*=\\s*(\\S+)")
  val unquotedMatch = unquotedRegex.find(contentDisposition)
  if (unquotedMatch != null) {
    return unquotedMatch.groupValues[1]
  }

  return null
}

suspend fun getUpdateFileUrl(
  currentVersion: String,
  updateUrl: String,
  client: OkHttpClient
) = runIOCatching {
  if (updateUrl.startsWith("https://api.github.com")) {
    getGithubUpdateUrl(currentVersion, updateUrl, client).getOrThrow()
  } else {
    updateUrl
  }
}


suspend fun getGithubUpdateUrl(
  currentVersion: String,
  updateUrl: String,
  client: OkHttpClient
) = runIOCatching {
  val request = Request.Builder().url(updateUrl).build()
  val res = client.newCall(request).execute().use {
    it.body.string().toData<List<GithubResponse>>()
  }.maxByOrNull {
    dateFormat.parse(it.createdAt)?.time ?: 0
  } ?: return@runIOCatching null
  if (res.tagName.substringAfter('v') != currentVersion) {
    res.assets.firstOrNull {
      it.name.endsWith(".jar")
    }?.browserDownloadUrl ?: throw Exception("No EApk assets found")
  } else {
    null
  }
}

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH).apply {
  timeZone = TimeZone.getTimeZone("UTC")
}

@Serializable
data class GithubResponse(
  @SerialName("tag_name")
  val tagName: String,
  @SerialName("created_at")
  val createdAt: String,
  val prerelease: Boolean,
  val assets: List<Asset>
)

@Serializable
data class Asset(
  val name: String,
  @SerialName("browser_download_url")
  val browserDownloadUrl: String
)

suspend fun getExtensionList(
  link: String,
  client: OkHttpClient
) = runIOCatching {
  val request = Request.Builder()
    .cacheControl(CacheControl.FORCE_NETWORK)
    .addHeader("Cookie", "preview=1")
    .url(link).build()
  client.newCall(request).execute().body.string().toData<List<ExtensionAssetResponse>>()
}.getOrElse {
  throw InvalidExtensionListException(it)
}


@Serializable
data class ExtensionAssetResponse(
  @SerialName("repoUrl")
  val repoUrl: String? = null,

  @SerialName("fileSize")
  val fileSize: Long? = null,

  @SerialName("author")
  val author: List<String>? = null,

  @SerialName("name")
  val name: String,

  @SerialName("className")
  val className: String,

  @SerialName("iconUrl")
  val iconUrl: String? = null,

  @SerialName("description")
  val description: String? = null,

  @SerialName("version")
  val version: String,

  @SerialName("url")
  val url: String,

  @SerialName("status")
  val status: Int,
)




