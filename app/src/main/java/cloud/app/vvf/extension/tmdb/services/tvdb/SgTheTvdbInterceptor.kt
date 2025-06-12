package cloud.app.vvf.extension.tmdb.services.tvdb

import com.uwetrottmann.thetvdb.TheTvdb
import com.uwetrottmann.thetvdb.TheTvdbInterceptor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * A custom [TheTvdbInterceptor] which does not require a [ ] instance until intercepting.
 */
class SgTheTvdbInterceptor constructor(val theTvdb: TheTvdb) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return TheTvdbInterceptor.handleIntercept(chain, theTvdb.jsonWebToken())
    }
}
