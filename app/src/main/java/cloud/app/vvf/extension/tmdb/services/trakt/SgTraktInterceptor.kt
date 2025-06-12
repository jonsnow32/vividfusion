package cloud.app.vvf.extension.tmdb.services.trakt

import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.TraktV2Interceptor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * A custom [TraktV2Interceptor] which does not require a [ ] instance until intercepting.
 */
class SgTraktInterceptor constructor(val trakt: TraktV2) : Interceptor {


    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return TraktV2Interceptor.handleIntercept(
            chain, trakt.apiKey(),
            trakt.accessToken()
        )
    }
}
