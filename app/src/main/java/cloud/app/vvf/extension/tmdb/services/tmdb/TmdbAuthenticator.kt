package cloud.app.vvf.extension.tmdb.services.tmdb

import cloud.app.vvf.common.exceptions.MissingApiKeyException
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.extension.tmdb.TmdbTvdbClient.Companion.PREF_TMDB_API_KEY
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.exceptions.TmdbAuthenticationFailedException
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

class TmdbAuthenticator(private val tmdb: Tmdb) : Authenticator {

  override fun authenticate(route: Route?, response: Response): Request? {
    return handleRequest(response, tmdb)
  }

  @Throws(IOException::class)
  fun handleRequest(response: Response, tmdb: Tmdb): Request? {
    if (response.request.url.pathSegments[0] == Tmdb.PATH_AUTHENTICATION) {
      return null
    }

    if (responseCount(response) >= 2) {
      throw TmdbAuthenticationFailedException(
        30, "Authentication failed: You do not have permissions to access the service."
      )
    }

    val urlBuilder = response.request.url.newBuilder()

    when {
      tmdb.useAccountSession() -> {
        if (tmdb.username.isNullOrEmpty() || tmdb.password.isNullOrEmpty()) {
          throw TmdbAuthenticationFailedException(26, "You must provide a username and password.")
        }
        val session = acquireAccountSession() ?: return null
        urlBuilder.setEncodedQueryParameter(Tmdb.PARAM_SESSION_ID, session)
      }

      tmdb.useGuestSession() -> {
        val session = acquireGuestSession() ?: return null
        urlBuilder.setEncodedQueryParameter(Tmdb.PARAM_GUEST_SESSION_ID, tmdb.guestSessionId)
      }

      else -> throw MissingApiKeyException(
        "cloud.app.vvf.plugin.BuiltInDatabaseClient",
        "builtIn",
        ExtensionType.DATABASE,
        PREF_TMDB_API_KEY
      )
    }

    return response.request.newBuilder().url(urlBuilder.build()).build()
  }

  @Throws(IOException::class)
  fun acquireAccountSession(): String? {
    val authService = tmdb.authenticationService()
    val token = authService.requestToken().execute().body() ?: return null
    val validatedToken =
      authService.validateToken(tmdb.username, tmdb.password, token.request_token).execute().body()
        ?: return null
    val session =
      authService.createSession(validatedToken.request_token).execute().body() ?: return null

    tmdb.sessionId = session.session_id
    return session.session_id
  }

  @Throws(IOException::class)
  fun acquireGuestSession(): String? {
    val authService = tmdb.authenticationService()
    val session = authService.createGuestSession().execute().body() ?: return null

    tmdb.guestSessionId = session.guest_session_id
    return session.guest_session_id
  }

  private fun responseCount(response: Response): Int {
    var result = 1
    var priorResponse = response.priorResponse
    while (priorResponse != null) {
      result++
      priorResponse = priorResponse.priorResponse
    }
    return result
  }
}
