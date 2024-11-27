package cloud.app.vvf.network.api.trakt;

import android.content.SharedPreferences;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.uwetrottmann.trakt5.entities.Settings;

import cloud.app.vvf.common.settings.PrefSettings;

public class TraktOAuthSettings {

  private static final String KEY_ACCESS_TOKEN = "trakt.access_token";
  private static final String KEY_REFRESH_TOKEN = "trakt.refresh_token";
  private static final String KEY_ACCESS_TOKEN_EXPIRY_DATE = "trakt.access_token_expiry";
  private static final String SETTINGS_FILE = "trakt-oauth-settings";

  /**
   * Returns the refresh token or {@code null} if there is none.
   */
  @Nullable
  public static String getAccessToken(PrefSettings prefs) {
    return prefs.getString(KEY_ACCESS_TOKEN);
  }

  /**
   * Returns the refresh token or {@code null} if there is none.
   */
  @Nullable
  static String getRefreshToken(PrefSettings prefs) {
    return prefs.getString(KEY_REFRESH_TOKEN);
  }

  /**
   * @param refreshToken The trakt refresh token.
   * @param expiresIn    The trakt access token expires duration in seconds.
   * @return Returns true if the new values were successfully written to persistent storage.
   */
  public static void storeRefreshData(PrefSettings prefs, @NonNull String accessToken, @NonNull String refreshToken,
                                 long expiresIn) {
    prefs.putString(KEY_ACCESS_TOKEN, accessToken);
    prefs.putString(KEY_REFRESH_TOKEN, refreshToken);
    prefs.putLong(KEY_ACCESS_TOKEN_EXPIRY_DATE, System.currentTimeMillis() + expiresIn * DateUtils.SECOND_IN_MILLIS);
  }

  public static boolean clearData(SharedPreferences prefs) {
    return prefs.edit()
      .putString(KEY_ACCESS_TOKEN, "")
      .putString(KEY_REFRESH_TOKEN, "")
      .putString(SETTINGS_FILE, "")
      .putLong(KEY_ACCESS_TOKEN_EXPIRY_DATE,
        0)
      .putString(SETTINGS_FILE, "")
      .commit();
  }

  public static boolean storeSettings(SharedPreferences prefs, Settings settings, Gson gson) {
    return prefs.edit()
      .putString(SETTINGS_FILE, gson.toJson(settings))
      .commit();
  }

  public static String getSettings(SharedPreferences prefs, Gson gson) {
    Settings settings = gson.fromJson(prefs.getString(SETTINGS_FILE, ""), Settings.class);
    if (settings != null) {
      return String.format("UserName: %s\nAccount Type: %s\nName: %s\nJoined At: %s", settings.user.username, settings.user.isPrivate.toString(), settings.user.name, settings.user.joined_at.toLocalDateTime().toString());
    }
    return "";
  }

  public static Settings getSettingsObject(SharedPreferences prefs, Gson gson) {
    return gson.fromJson(prefs.getString(SETTINGS_FILE, ""), Settings.class);
  }
}
