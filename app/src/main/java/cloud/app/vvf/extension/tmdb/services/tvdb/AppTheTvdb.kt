package cloud.app.vvf.extension.tmdb.services.tvdb

import cloud.app.vvf.common.settings.PrefSettings
import com.uwetrottmann.thetvdb.TheTvdb
import okhttp3.OkHttpClient

/**
 * Extends [TheTvdb] to use our own caching OkHttp client and a preferences file to store the
 * current JSON web token long term. The token is currently valid 24 hours, so preserve it if the
 * app gets killed to avoid an additional login endpoint call.
 */
class AppTheTvdb(
    private val okHttpClient: OkHttpClient,
    api_key: String,
    private val prefSettings: PrefSettings
) : TheTvdb(api_key) {
    override fun jsonWebToken(): String? {
        return prefSettings.getString(KEY_JSON_WEB_TOKEN)
    }

    override fun jsonWebToken(value: String?) {
        prefSettings.putString(KEY_JSON_WEB_TOKEN, value)
    }

    @Synchronized
    override fun okHttpClient(): OkHttpClient {
        return okHttpClient
    }

    companion object {
        private const val PREFERENCE_FILE = "thetvdb-prefs"
        private const val KEY_JSON_WEB_TOKEN = "token"
    }
}
