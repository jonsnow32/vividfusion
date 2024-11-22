package cloud.app.vvf.network.Interceptors

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class OfflineCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: Exception) {
            var cc = CacheControl.Builder().onlyIfCached().maxStale(1, TimeUnit.DAYS).build()
            var offlineRequest = chain.request().newBuilder().cacheControl(cc).build()
            chain.proceed(offlineRequest)
        }
    }
}
