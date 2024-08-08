package cloud.app.avp.network.api.alldebrid;

import android.content.SharedPreferences;

import cloud.app.avp.network.api.alldebrid.model.User;
import com.google.gson.Gson;

public class AllDebridOAuthSettings {

    private static final String KEY_ACCESS_TOKEN_EXPIRY_DATE = "alldebrid.access_token_expiry";
    private static final String SETTINGS_FILE = "alldebrid-oauth-settings";
    private static final String KEY_API_KEY = "alldebrid.api-key";

    public static String getApiKey(SharedPreferences prefs) {
        return prefs.getString(KEY_API_KEY, null);
    }

    public static boolean storeApiKey(SharedPreferences prefs, String apiKey) {
        return prefs.edit().putString(KEY_API_KEY, apiKey).commit();
    }


    public static boolean clearData(SharedPreferences prefs) {
        return prefs.edit().putString(SETTINGS_FILE, "").putString(KEY_API_KEY, "").putLong(KEY_ACCESS_TOKEN_EXPIRY_DATE, 0).commit();
    }

    public static boolean storeSettings(SharedPreferences prefs, User settings, Gson gson) {
        return prefs.edit().putString(SETTINGS_FILE, gson.toJson(settings)).commit();
    }

    public static String getSettings(SharedPreferences prefs, Gson gson) {
        User settings = gson.fromJson(prefs.getString(SETTINGS_FILE, ""), User.class);
        if (settings != null) {
            return settings.toString();
        }
        return "";
    }

    public static User getSettingsObject(SharedPreferences prefs, Gson gson) {
        return gson.fromJson(prefs.getString(SETTINGS_FILE, ""), User.class);
    }
}
