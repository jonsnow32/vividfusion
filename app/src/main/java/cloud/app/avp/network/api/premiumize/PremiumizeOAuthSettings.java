package cloud.app.avp.network.api.premiumize;

import android.content.SharedPreferences;

import cloud.app.avp.network.api.premiumize.models.PremiumizeUserInfo;
import com.google.gson.Gson;

public class PremiumizeOAuthSettings {

    private static final String KEY_ACCESS_TOKEN_EXPIRY_DATE = "premiumize.access_token_expiry";
    private static final String SETTINGS_FILE = "premiumize-oauth-settings";
    private static final String KEY_API_KEY = "premiumize.clientid";

    public static String getApiKey(SharedPreferences prefs) {
        return prefs.getString(KEY_API_KEY, null);
    }

    public static boolean storeApiKey(SharedPreferences prefs, String apiKey) {
        return prefs.edit()
                .putString(KEY_API_KEY, apiKey)
                .commit();
    }


    public static boolean clearData(SharedPreferences prefs) {
        return prefs.edit()
                .putString(SETTINGS_FILE, "")
                .putString(KEY_API_KEY, "")
                .putLong(KEY_ACCESS_TOKEN_EXPIRY_DATE,
                        0)
                .commit();
    }

    public static boolean storeSettings(SharedPreferences prefs, PremiumizeUserInfo settings, Gson gson) {
        return prefs.edit()
                .putString(SETTINGS_FILE, gson.toJson(settings))
                .commit();
    }

    public static String getSettings(SharedPreferences prefs, Gson gson) {
        PremiumizeUserInfo settings = gson.fromJson(prefs.getString(SETTINGS_FILE, ""), PremiumizeUserInfo.class);
        if (settings != null) {
            return settings.toString();
        }
        return "";
    }

    public static PremiumizeUserInfo getSettingsObject(SharedPreferences prefs, Gson gson) {
        return gson.fromJson(prefs.getString(SETTINGS_FILE, ""), PremiumizeUserInfo.class);
    }
}
