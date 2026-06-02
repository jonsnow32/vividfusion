package cloud.app.vvf.network.debrid

import android.content.SharedPreferences
import cloud.app.vvf.network.api.alldebrid.AllDebridApi
import cloud.app.vvf.network.api.alldebrid.AllDebridOAuthSettings
import cloud.app.vvf.network.api.premiumize.PremiumizeApi
import cloud.app.vvf.network.api.premiumize.PremiumizeOAuthSettings
import cloud.app.vvf.network.api.realdebrid.RealDebirdOAuthSettings
import cloud.app.vvf.network.api.realdebrid.RealDebridApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebridResolver @Inject constructor(
  private val sharedPreferences: SharedPreferences,
  private val realDebridApi: RealDebridApi,
  private val allDebridApi: AllDebridApi,
  private val premiumizeApi: PremiumizeApi,
) {
  sealed class Result {
    data class Resolved(
      val url: String,
      val filename: String?,
      val filesize: Long,
      val provider: String,
      val alternatives: List<Link> = emptyList(),
    ) : Result()

    data class Link(val url: String, val label: String)

    object NotConfigured : Result()
    data class Error(val cause: String) : Result()
  }

  fun isConfigured(): Boolean =
    !RealDebirdOAuthSettings.getAccessToken(sharedPreferences).isNullOrEmpty() ||
      !AllDebridOAuthSettings.getApiKey(sharedPreferences).isNullOrEmpty() ||
      !PremiumizeOAuthSettings.getApiKey(sharedPreferences).isNullOrEmpty()

  fun providerName(): String? = when {
    !RealDebirdOAuthSettings.getAccessToken(sharedPreferences).isNullOrEmpty() -> "RealDebrid"
    !AllDebridOAuthSettings.getApiKey(sharedPreferences).isNullOrEmpty() -> "AllDebrid"
    !PremiumizeOAuthSettings.getApiKey(sharedPreferences).isNullOrEmpty() -> "Premiumize"
    else -> null
  }

  suspend fun unrestrict(url: String): Result = withContext(Dispatchers.IO) {
    when {
      !RealDebirdOAuthSettings.getAccessToken(sharedPreferences).isNullOrEmpty() -> unrestrictRD(url)
      !AllDebridOAuthSettings.getApiKey(sharedPreferences).isNullOrEmpty() -> unrestrictAD(url)
      !PremiumizeOAuthSettings.getApiKey(sharedPreferences).isNullOrEmpty() -> unrestrictPM(url)
      else -> Result.NotConfigured
    }
  }

  private fun unrestrictRD(url: String): Result = try {
    val response = realDebridApi.unrestrictLink(url, null, null).execute()
    val body = response.body()
    if (response.isSuccessful && body?.download != null) {
      val alts = body.alternative?.mapNotNull { alt ->
        val altUrl = alt.download ?: return@mapNotNull null
        Result.Link(altUrl, alt.filename ?: alt.type ?: "Alternative")
      } ?: emptyList()
      Result.Resolved(
        url = body.download!!,
        filename = body.filename,
        filesize = body.filesize,
        provider = "RealDebrid",
        alternatives = alts,
      )
    } else {
      Result.Error("RealDebrid ${response.code()}: ${response.message()}")
    }
  } catch (e: Exception) {
    Result.Error(e.message ?: "RealDebrid unrestrict failed")
  }

  private fun unrestrictAD(url: String): Result = try {
    val response = allDebridApi.getdownloadlink(url).execute()
    val data = response.body()?.data
    if (response.isSuccessful && data?.link != null) {
      Result.Resolved(
        url = data.link,
        filename = data.filename,
        filesize = data.filesize,
        provider = "AllDebrid",
      )
    } else {
      Result.Error("AllDebrid ${response.code()}")
    }
  } catch (e: Exception) {
    Result.Error(e.message ?: "AllDebrid unrestrict failed")
  }

  private fun unrestrictPM(url: String): Result = try {
    val apiKey = PremiumizeOAuthSettings.getApiKey(sharedPreferences)
      ?: return Result.NotConfigured
    val response = premiumizeApi.getPremiumizeDirectDL(apiKey, url).execute()
    val content = response.body()?.content?.firstOrNull()
    if (response.isSuccessful && content?.link != null) {
      val alts = response.body()!!.content!!.drop(1).mapNotNull { c ->
        if (c.link != null) Result.Link(c.link, c.path?.substringAfterLast("/") ?: "File")
        else null
      }
      Result.Resolved(
        url = content.link,
        filename = content.path?.substringAfterLast("/"),
        filesize = content.size,
        provider = "Premiumize",
        alternatives = alts,
      )
    } else {
      Result.Error("Premiumize ${response.code()}")
    }
  } catch (e: Exception) {
    Result.Error(e.message ?: "Premiumize unrestrict failed")
  }
}
