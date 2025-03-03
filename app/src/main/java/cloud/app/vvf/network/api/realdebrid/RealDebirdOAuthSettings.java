package cloud.app.vvf.network.api.realdebrid;

import android.content.SharedPreferences;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cloud.app.vvf.network.api.realdebrid.model.RealDebridUserInfor;
import com.google.gson.Gson;

public class RealDebirdOAuthSettings {

    private static final String KEY_ACCESS_TOKEN = "realdebird.access_token";
    private static final String KEY_REFRESH_TOKEN = "realdebird.refresh_token";
    private static final String KEY_ACCESS_TOKEN_EXPIRY_DATE = "realdebird.access_token_expiry";
    private static final String SETTINGS_FILE = "realdebird-oauth-settings";
    private static final String KEY_CLIENT_ID = "realdebird.extensionId";
    private static final String KEY_CLIENT_SECRET = "realdebird.clientsecret";

    public static String getextensionId(SharedPreferences prefs) {
        return prefs.getString(KEY_CLIENT_ID, null);
    }

    public static boolean storeextensionId(SharedPreferences prefs, String extensionId) {
        return prefs.edit()
                .putString(KEY_CLIENT_ID, extensionId)
                .commit();
    }

    public static String getClientSecret(SharedPreferences prefs) {
        return prefs.getString(KEY_CLIENT_SECRET, null);
    }

    public static boolean storeClientSecret(SharedPreferences prefs, String clientSecret) {
        return prefs.edit()
                .putString(KEY_CLIENT_SECRET, clientSecret)
                .commit();
    }

    /**
     * Returns the accessToken if there is none.
     */
    @Nullable
    public static String getAccessToken(SharedPreferences prefs) {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Returns the refresh token or {@code null} if there is none.
     */
    @Nullable
    public static String getRefreshToken(SharedPreferences prefs) {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * @param refreshToken The realdebird refresh token.
     * @param expiresIn    The realdebird access token expires duration in seconds.
     * @return Returns true if the new values were successfully written to persistent storage.
     */
    public static boolean storeRefreshData(SharedPreferences prefs, @NonNull String accessToken, @NonNull String refreshToken,
                                           long expiresIn) {
        return prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_ACCESS_TOKEN_EXPIRY_DATE,
                        System.currentTimeMillis() + expiresIn * DateUtils.SECOND_IN_MILLIS)
                .commit();
    }

    public static boolean clearData(SharedPreferences prefs) {
        return prefs.edit()
                .putString(KEY_ACCESS_TOKEN, "")
                .putString(KEY_REFRESH_TOKEN, "")
                .putString(SETTINGS_FILE, "")
                .putLong(KEY_ACCESS_TOKEN_EXPIRY_DATE,
                        0)
                .commit();
    }

    public static boolean storeSettings(SharedPreferences prefs, RealDebridUserInfor settings, Gson gson) {
        return prefs.edit()
                .putString(SETTINGS_FILE, gson.toJson(settings))
                .commit();
    }

    public static String getSettings(SharedPreferences prefs, Gson gson) {
        RealDebridUserInfor settings = gson.fromJson(prefs.getString(SETTINGS_FILE, ""), RealDebridUserInfor.class);
        if (settings != null) {
            return settings.toString();
        }
        return "";
    }

    public static RealDebridUserInfor getSettingsObject(SharedPreferences prefs, Gson gson) {
        return gson.fromJson(prefs.getString(SETTINGS_FILE, ""), RealDebridUserInfor.class);
    }
}
