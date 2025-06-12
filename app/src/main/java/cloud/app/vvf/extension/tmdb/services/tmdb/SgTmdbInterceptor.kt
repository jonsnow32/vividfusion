package cloud.app.vvf.extension.tmdb.services.tmdb

import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.TmdbInterceptor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * A custom [TmdbInterceptor] which does not require a [Tmdb] instance until
 * intercepting.
 */
class SgTmdbInterceptor constructor(private val tmdb: Tmdb) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return TmdbInterceptor.handleIntercept(chain, tmdb)
    }
}
