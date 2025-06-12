package cloud.app.vvf.extension.tmdb.services.trakt

import android.content.Context
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.extension.tmdb.services.trakt.services.ExtendService
import com.uwetrottmann.trakt5.TraktV2
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response

class AppTrakt(
    okHttpClient: OkHttpClient,
    client_id: String,
    clientSecret: String,
    prefSettings: PrefSettings
) : TraktV2(client_id, clientSecret, CALLBACK_URL) {
    private val okHttpClient: OkHttpClient
    private val prefSettings: PrefSettings

    init {
        val builder = okHttpClient.newBuilder()
        setOkHttpClientDefaults(builder)
        this.okHttpClient = builder.build()
        this.prefSettings = prefSettings
    }

    override fun accessToken(): String? {
        return TraktOAuthSettings.getAccessToken(prefSettings)
    }

    override fun refreshToken(): String? {
        return TraktOAuthSettings.getRefreshToken(prefSettings)
    }

    @Synchronized
    override fun okHttpClient(): OkHttpClient {
        return okHttpClient
    }

    fun extendService(): ExtendService {
        return retrofit().create<ExtendService>(ExtendService::class.java)
    }

    companion object {
        var CALLBACK_URL: String = "mmat://trakt/auth/callback"

        /**
         * Check if the request was unauthorized.
         *
         * @see .isUnauthorized
         */
        fun isUnauthorized(response: Response<*>): Boolean {
            return response.code() == 401
        }

        /**
         * Check if the associated Trakt account is locked.
         *
         *
         * https://trakt.docs.apiary.io/#introduction/locked-user-account
         */
        fun isAccountLocked(response: Response<*>): Boolean {
            return response.code() == 423
        }

        fun isUnauthorized(context: Context?, response: Response<*>): Boolean {
            // current access token is invalid, remove it and notify user to re-connect
            //TraktOAuthSettings.setCredentialsInvalid();
            return response.code() == 401
        }

        fun checkForTraktError(trakt: TraktV2, response: Response<*>): String? {
            val error = trakt.checkForTraktError(response)
            if (error != null && error.message != null) {
                return error.message
            } else {
                return null
            }
        }

        fun checkForTraktOAuthError(trakt: TraktV2, response: Response<*>): String? {
            val error = trakt.checkForTraktOAuthError(response)
            if (error != null && error.error != null && error.error_description != null) {
                return error.error + " " + error.error_description
            } else {
                return null
            }
        }

        /**
         * Executes the given call. Will return null if the call fails for any reason, including auth
         * failures.
         */
        fun <T> executeCall(call: Call<T?>, action: String?): T? {
            try {
                val response = call.execute()
                if (response.isSuccessful()) {
                    return response.body()
                } else {
                    //Errors.logAndReport(action, response);
                }
            } catch (e: Exception) {
                //Errors.logAndReport(action, e);
            }
            return null
        }

        /**
         * Executes the given call. If the call fails because auth is invalid, removes the current
         * access token and displays a warning notification to the user.
         */
        fun <T> executeAuthenticatedCall(context: Context?, call: Call<T?>, action: String?): T? {
            try {
                val response = call.execute()
                if (response.isSuccessful()) {
                    return response.body()
                } else {
                    if (!isUnauthorized(context, response)) {
                        //Errors.logAndReport(action, response);
                    }
                }
            } catch (e: Exception) {
                // Errors.logAndReport(action, e);
            }
            return null
        }
    }
}
