package cloud.app.avp.network.api.tmdb

import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.TmdbAuthenticator
import com.uwetrottmann.tmdb2.TmdbInterceptor
import okhttp3.OkHttpClient

/**
 * Creates a custom [Tmdb] using the given API key and HTTP client.
 */
class AppTmdb(
    private val okHttpClient: OkHttpClient,
    apiKey: String,
) : Tmdb(apiKey) {
    override fun okHttpClient(): OkHttpClient {
        var builder = okHttpClient.newBuilder().addInterceptor(TmdbInterceptor(this))
            .authenticator(TmdbAuthenticator(this))
        return builder.build()
    }
    fun extendService(): ExtendService = retrofit.create(ExtendService::class.java)
}
