package cloud.app.vvf.network.api.trakt;

import android.content.Context;

import androidx.annotation.Nullable;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.TraktError;
import com.uwetrottmann.trakt5.entities.TraktOAuthError;

import cloud.app.vvf.common.settings.PrefSettings;
import cloud.app.vvf.network.api.trakt.services.ExtendService;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;


public class AppTrakt extends TraktV2 {

    public static String CALLBACK_URL = "mmat://trakt/auth/callback";
    private final OkHttpClient okHttpClient;
    private final PrefSettings prefSettings;

    public AppTrakt(OkHttpClient okHttpClient, String client_id, String clientSecret, PrefSettings prefSettings) {
        super(client_id, clientSecret, CALLBACK_URL);
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        setOkHttpClientDefaults(builder);
        this.okHttpClient = builder.build();
        this.prefSettings = prefSettings;
    }

    /**
     * Check if the request was unauthorized.
     *
     * @see #isUnauthorized(Context, Response)
     */
    public static boolean isUnauthorized(Response<?> response) {
        return response.code() == 401;
    }

    /**
     * Check if the associated Trakt account is locked.
     * <p>
     * https://trakt.docs.apiary.io/#introduction/locked-user-account
     */
    public static boolean isAccountLocked(Response<?> response) {
        return response.code() == 423;
    }

    public static boolean isUnauthorized(Context context, Response<?> response) {
        // current access token is invalid, remove it and notify user to re-connect
        //TraktOAuthSettings.setCredentialsInvalid();
        return response.code() == 401;
    }

    @Nullable
    public static String checkForTraktError(TraktV2 trakt, Response<?> response) {
        TraktError error = trakt.checkForTraktError(response);
        if (error != null && error.message != null) {
            return error.message;
        } else {
            return null;
        }
    }

    @Nullable
    public static String checkForTraktOAuthError(TraktV2 trakt, Response<?> response) {
        TraktOAuthError error = trakt.checkForTraktOAuthError(response);
        if (error != null && error.error != null && error.error_description != null) {
            return error.error + " " + error.error_description;
        } else {
            return null;
        }
    }

    /**
     * Executes the given call. Will return null if the call fails for any reason, including auth
     * failures.
     */
    public static <T> T executeCall(Call<T> call, String action) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                //Errors.logAndReport(action, response);
            }
        } catch (Exception e) {
            //Errors.logAndReport(action, e);
        }
        return null;
    }

    /**
     * Executes the given call. If the call fails because auth is invalid, removes the current
     * access token and displays a warning notification to the user.
     */

    public static <T> T executeAuthenticatedCall(Context context, Call<T> call, String action) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                if (!isUnauthorized(context, response)) {
                    //Errors.logAndReport(action, response);
                }
            }
        } catch (Exception e) {
            // Errors.logAndReport(action, e);
        }
        return null;
    }

    @Override
    public String accessToken() {
        return TraktOAuthSettings.getAccessToken(prefSettings);
    }

    @Override
    public String refreshToken() {
        return TraktOAuthSettings.getRefreshToken(prefSettings);
    }

    protected synchronized OkHttpClient okHttpClient() {

        return okHttpClient;
    }

    public ExtendService extendService() {
        return retrofit().create(ExtendService.class);
    }
}
