package cloud.app.vvf.network.opensubtitles

import android.content.SharedPreferences
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenSubtitlesClient @Inject constructor(
  private val okHttpClient: OkHttpClient,
  private val sharedPreferences: SharedPreferences,
) {
  companion object {
    private const val BASE = "https://api.opensubtitles.com/api/v1"
    private const val PREF_API_KEY = "opensubtitles_api_key"
    private const val UA = "VividFusion v1.0"
  }

  data class SubtitleResult(
    val fileId: Int,
    val filename: String,
    val language: String,
    val downloadCount: Int,
    val release: String,
  ) {
    val displayLabel: String
      get() = buildString {
        if (language.isNotEmpty()) append("[${language.uppercase()}] ")
        append(if (release.isNotEmpty()) release else filename)
        if (downloadCount > 0) append(" (${downloadCount}↓)")
      }
  }

  suspend fun search(query: String, language: String = ""): List<SubtitleResult> =
    withContext(Dispatchers.IO) {
      try {
        val urlBuilder = "$BASE/subtitles".toHttpUrl().newBuilder()
          .addQueryParameter("query", query)
        if (language.isNotEmpty()) urlBuilder.addQueryParameter("languages", language)

        val request = Request.Builder()
          .url(urlBuilder.build())
          .header("User-Agent", UA)
          .header("Accept", "application/json")
          .build()

        val body = okHttpClient.newCall(request).execute().use { it.body?.string() }
          ?: return@withContext emptyList()
        parseSearch(body)
      } catch (_: Exception) {
        emptyList()
      }
    }

  private fun parseSearch(json: String): List<SubtitleResult> = try {
    val data = JsonParser.parseString(json).asJsonObject.getAsJsonArray("data")
      ?: return emptyList()
    data.mapNotNull { el ->
      val attrs = el.asJsonObject.getAsJsonObject("attributes") ?: return@mapNotNull null
      val files = attrs.getAsJsonArray("files") ?: return@mapNotNull null
      if (files.size() == 0) return@mapNotNull null
      val file = files[0].asJsonObject
      SubtitleResult(
        fileId = file.get("file_id")?.asInt ?: return@mapNotNull null,
        filename = file.get("file_name")?.asString ?: "subtitle.srt",
        language = attrs.get("language")?.asString ?: "",
        downloadCount = attrs.get("download_count")?.asInt ?: 0,
        release = attrs.get("release")?.asString ?: "",
      )
    }
  } catch (_: Exception) {
    emptyList()
  }

  // Returns direct download URL, or null on failure.
  suspend fun getDownloadUrl(fileId: Int): String? = withContext(Dispatchers.IO) {
    try {
      val apiKey = sharedPreferences.getString(PREF_API_KEY, "") ?: ""
      val body = """{"file_id":$fileId}""".toRequestBody("application/json".toMediaType())
      val requestBuilder = Request.Builder()
        .url("$BASE/download")
        .post(body)
        .header("User-Agent", UA)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
      if (apiKey.isNotEmpty()) requestBuilder.header("Api-Key", apiKey)

      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val responseBody = response.body?.string() ?: return@withContext null
      if (!response.isSuccessful) return@withContext null
      JsonParser.parseString(responseBody).asJsonObject.get("link")?.asString
    } catch (_: Exception) {
      null
    }
  }
}
