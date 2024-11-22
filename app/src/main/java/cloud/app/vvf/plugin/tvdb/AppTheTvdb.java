package cloud.app.vvf.plugin.tvdb;

import com.uwetrottmann.thetvdb.TheTvdb;

import cloud.app.vvf.common.settings.PrefSettings;
import okhttp3.OkHttpClient;

/**
 * Extends {@link TheTvdb} to use our own caching OkHttp client and a preferences file to store the
 * current JSON web token long term. The token is currently valid 24 hours, so preserve it if the
 * app gets killed to avoid an additional login endpoint call.
 */
public class AppTheTvdb extends TheTvdb {

    private static final String PREFERENCE_FILE = "thetvdb-prefs";
    private static final String KEY_JSON_WEB_TOKEN = "token";

    private final OkHttpClient okHttpClient;
    private final PrefSettings prefSettings;

    public AppTheTvdb(OkHttpClient okHttpClient, String api_key, PrefSettings prefSettings) {
        super(api_key);
        this.okHttpClient = okHttpClient;
        this.prefSettings = prefSettings;
    }

    @Override
    public String jsonWebToken() {
        return prefSettings.getString(KEY_JSON_WEB_TOKEN);
    }

    @Override
    public void jsonWebToken(String value) {
      prefSettings.putString(KEY_JSON_WEB_TOKEN, value);
    }

    @Override
    protected synchronized OkHttpClient okHttpClient() {
        return okHttpClient;
    }
}
