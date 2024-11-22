package cloud.app.vvf.network.Interceptors

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class CacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val cacheControlHeaders = listOf("no-store", "no-cache", "must-revalidate", "max-stale=0")

        // Check if any of the cache control headers are present
        val foreCache = cacheControlHeaders.any { request.header("Cache-Control")?.contains(it) == true }

        return if (foreCache) {
            chain.proceed(request)
        } else {
            // Force the request to bypass cache and fetch from the network
            val newRequest = request.newBuilder()
                .cacheControl(CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build())
                .build()
            chain.proceed(newRequest)
        }
    }
}
