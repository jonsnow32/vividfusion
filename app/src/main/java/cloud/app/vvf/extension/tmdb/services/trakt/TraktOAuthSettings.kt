package cloud.app.vvf.extension.tmdb.services.trakt

import android.content.SharedPreferences
import android.text.format.DateUtils
import cloud.app.vvf.common.settings.PrefSettings
import com.google.gson.Gson
import com.uwetrottmann.trakt5.entities.Settings

object TraktOAuthSettings {
    private const val KEY_ACCESS_TOKEN = "trakt.access_token"
    private const val KEY_REFRESH_TOKEN = "trakt.refresh_token"
    private const val KEY_ACCESS_TOKEN_EXPIRY_DATE = "trakt.access_token_expiry"
    private const val SETTINGS_FILE = "trakt-oauth-settings"

    /**
     * Returns the refresh token or `null` if there is none.
     */
    fun getAccessToken(prefs: PrefSettings): String? {
        return prefs.getString(KEY_ACCESS_TOKEN)
    }

    /**
     * Returns the refresh token or `null` if there is none.
     */
    fun getRefreshToken(prefs: PrefSettings): String? {
        return prefs.getString(KEY_REFRESH_TOKEN)
    }

    /**
     * @param refreshToken The trakt refresh token.
     * @param expiresIn    The trakt access token expires duration in seconds.
     * @return Returns true if the new values were successfully written to persistent storage.
     */
    fun storeRefreshData(
        prefs: PrefSettings,  accessToken: String,  refreshToken: String,
        expiresIn: Long
    ) {
        prefs.putString(KEY_ACCESS_TOKEN, accessToken)
        prefs.putString(KEY_REFRESH_TOKEN, refreshToken)
        prefs.putLong(
            KEY_ACCESS_TOKEN_EXPIRY_DATE,
            System.currentTimeMillis() + expiresIn * DateUtils.SECOND_IN_MILLIS
        )
    }

    fun clearData(prefs: SharedPreferences): Boolean {
        return prefs.edit()
            .putString(KEY_ACCESS_TOKEN, "")
            .putString(KEY_REFRESH_TOKEN, "")
            .putString(SETTINGS_FILE, "")
            .putLong(
                KEY_ACCESS_TOKEN_EXPIRY_DATE,
                0
            )
            .putString(SETTINGS_FILE, "")
            .commit()
    }

    fun storeSettings(prefs: SharedPreferences, settings: Settings?, gson: Gson): Boolean {
        return prefs.edit()
            .putString(SETTINGS_FILE, gson.toJson(settings))
            .commit()
    }

    fun getSettings(prefs: SharedPreferences, gson: Gson): String {
        val settings =
            gson.fromJson<Settings?>(prefs.getString(SETTINGS_FILE, ""), Settings::class.java)
        if (settings != null) {
            return String.format(
                "UserName: %s\nAccount Type: %s\nName: %s\nJoined At: %s",
                settings.user.username,
                settings.user.isPrivate.toString(),
                settings.user.name,
                settings.user.joined_at.toLocalDateTime().toString()
            )
        }
        return ""
    }

    fun getSettingsObject(prefs: SharedPreferences, gson: Gson): Settings? {
        return gson.fromJson<Settings?>(prefs.getString(SETTINGS_FILE, ""), Settings::class.java)
    }
}
