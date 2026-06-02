package cloud.app.vvf.ui.setting

import android.content.SharedPreferences
import cloud.app.vvf.network.api.alldebrid.AllDebridApi
import cloud.app.vvf.network.api.alldebrid.AllDebridOAuthSettings
import cloud.app.vvf.network.api.alldebrid.model.ADPin
import cloud.app.vvf.network.api.premiumize.PremiumizeOAuthSettings
import cloud.app.vvf.network.api.realdebrid.RealDebirdOAuthSettings
import cloud.app.vvf.network.api.realdebrid.RealDebridOauthApi
import cloud.app.vvf.network.api.realdebrid.model.RealDebridGetDeviceCodeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import javax.inject.Inject

@HiltViewModel
class ServicesViewModel @Inject constructor(
  private val sharedPreferences: SharedPreferences,
  private val rdOauthApi: RealDebridOauthApi,
  private val adApi: AllDebridApi,
) : ViewModel() {

  // ── Status checks ──────────────────────────────────────────────

  fun isRDConnected() =
    !RealDebirdOAuthSettings.getAccessToken(sharedPreferences).isNullOrEmpty()

  fun isADConnected() =
    !AllDebridOAuthSettings.getApiKey(sharedPreferences).isNullOrEmpty()

  fun isPMConnected() =
    !PremiumizeOAuthSettings.getApiKey(sharedPreferences).isNullOrEmpty()

  fun isOSConfigured() =
    !sharedPreferences.getString(PREF_OS_KEY, "").isNullOrEmpty()

  fun rdSummary() = if (isRDConnected()) "Connected" else "Not connected"
  fun adSummary() = if (isADConnected()) "Connected" else "Not connected"
  fun pmSummary(): String {
    val key = PremiumizeOAuthSettings.getApiKey(sharedPreferences)
    return if (key.isNullOrEmpty()) "Not connected" else "Connected (${key.take(6)}…)"
  }
  fun osSummary(): String {
    val key = sharedPreferences.getString(PREF_OS_KEY, "") ?: ""
    return if (key.isEmpty()) "Anonymous (5 downloads/day)" else "API key configured (${key.take(6)}…)"
  }

  // ── RealDebrid OAuth device flow ───────────────────────────────

  suspend fun rdStartAuth(): RealDebridGetDeviceCodeResult? = withContext(Dispatchers.IO) {
    runCatching { rdOauthApi.oauthDeviceCode().execute().body() }.getOrNull()
  }

  /** Polls for device credentials, then exchanges for tokens. Returns true on success. */
  suspend fun rdPollCredentials(deviceCode: String): Boolean = withContext(Dispatchers.IO) {
    repeat(24) { // 2-min window, 5s interval
      runCatching {
        val creds = rdOauthApi.oauthDeviceCredentials(code = deviceCode).execute().body()
        if (!creds?.client_id.isNullOrEmpty()) {
          val params = mapOf(
            "client_id" to creds!!.client_id,
            "client_secret" to creds.client_secret,
            "code" to deviceCode,
            "grant_type" to "http://oauth.net/grant_type/device/1.0",
          )
          val token = rdOauthApi.oauthtoken(params).execute().body()
          if (!token?.access_token.isNullOrEmpty()) {
            RealDebirdOAuthSettings.storeextensionId(sharedPreferences, creds.client_id!!)
            RealDebirdOAuthSettings.storeClientSecret(sharedPreferences, creds.client_secret!!)
            RealDebirdOAuthSettings.storeRefreshData(
              sharedPreferences,
              token!!.access_token!!,
              token.refresh_token!!,
              token.expires_in,
            )
            return@withContext true
          }
        }
      }
      delay(5_000)
    }
    false
  }

  fun rdDisconnect() = RealDebirdOAuthSettings.clearData(sharedPreferences)

  // ── AllDebrid PIN flow ─────────────────────────────────────────

  suspend fun adStartAuth(): ADPin? = withContext(Dispatchers.IO) {
    runCatching { adApi.getPin().execute().body() }.getOrNull()
  }

  suspend fun adPollToken(check: String, pin: String): Boolean = withContext(Dispatchers.IO) {
    repeat(24) {
      runCatching {
        val result = adApi.check(check, pin).execute().body()
        val apiKey = result?.data?.apikey
        if (!apiKey.isNullOrEmpty()) {
          AllDebridOAuthSettings.storeApiKey(sharedPreferences, apiKey)
          return@withContext true
        }
      }
      delay(5_000)
    }
    false
  }

  fun adDisconnect() = AllDebridOAuthSettings.clearData(sharedPreferences)

  // ── Premiumize (API key) ───────────────────────────────────────

  fun pmSaveKey(key: String) = PremiumizeOAuthSettings.storeApiKey(sharedPreferences, key)
  fun pmDisconnect() = sharedPreferences.edit().remove("premiumize.extensionId").apply()

  // ── OpenSubtitles (API key) ────────────────────────────────────

  fun osSaveKey(key: String) = sharedPreferences.edit().putString(PREF_OS_KEY, key).apply()
  fun osClear() = sharedPreferences.edit().remove(PREF_OS_KEY).apply()

  companion object {
    const val PREF_OS_KEY = "opensubtitles_api_key"
  }
}
