package cloud.app.vvf.ads

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class AdPreloader @Inject constructor(
    private val waterfallManager: AdWaterfallManager
) {

    // Cache loaded ads
    private val adCache = ConcurrentHashMap<String, CachedAd>()

    // Loading state of each ad type
    private val _loadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val loadingStates: StateFlow<Map<String, Boolean>> = _loadingStates

    // Job to preload ads
    private var preloadJob: Job? = null

    data class CachedAd(
        val adType: cloud.app.vvf.ads.providers.AdProvider.AdType,
        val timestamp: Long,
        val providerType: cloud.app.vvf.ads.providers.AdProvider.ProviderType
    )

    companion object {
        private const val CACHE_EXPIRY_TIME = 30 * 60 * 1000L // 30 minutes
        private const val PRELOAD_DELAY = 5000L // 5 seconds
    }

    /**
     * Start intelligent ad preloading
     */
    fun startIntelligentPreloading(context: Context) {
        preloadJob?.cancel()
        preloadJob = CoroutineScope(Dispatchers.IO).launch {
            // Wait for app to fully start
            delay(PRELOAD_DELAY)

            while (isActive) {
                try {
                    preloadAdsIfNeeded(context)
                    // Check again every 5 minutes
                    delay(300_000L)
                } catch (e: Exception) {
                    Timber.w(e, "Error in intelligent preloading")
                    delay(60_000L) // Retry after 1 minute if error
                }
            }
        }
    }

    /**
     * Preload ads nếu cần thiết
     */
    private suspend fun preloadAdsIfNeeded(context: Context) {
        val currentTime = System.currentTimeMillis()

        // Kiểm tra và preload interstitial ads
        if (shouldPreload("interstitial", currentTime)) {
            preloadAdType(context, cloud.app.vvf.ads.providers.AdProvider.AdType.INTERSTITIAL, "interstitial")
        }

        // Kiểm tra và preload rewarded ads
        if (shouldPreload("rewarded", currentTime)) {
            preloadAdType(context, cloud.app.vvf.ads.providers.AdProvider.AdType.REWARDED, "rewarded")
        }
    }

    /**
     * Kiểm tra xem có nên preload ad hay không
     */
    private fun shouldPreload(adKey: String, currentTime: Long): Boolean {
        val cachedAd = adCache[adKey]
        return cachedAd == null ||
               (currentTime - cachedAd.timestamp) > CACHE_EXPIRY_TIME ||
               !waterfallManager.isAdReady(cachedAd.adType)
    }

    /**
     * Preload một loại ad cụ thể
     */
    private suspend fun preloadAdType(
        context: Context,
        adType: cloud.app.vvf.ads.providers.AdProvider.AdType,
        adKey: String
    ) {
        updateLoadingState(adKey, true)

        try {
            val provider = waterfallManager.getReadyProvider(adType)
            if (provider != null) {
                val success = provider.preloadAd(context, adType)
                if (success) {
                    adCache[adKey] = CachedAd(
                        adType = adType,
                        timestamp = System.currentTimeMillis(),
                        providerType = provider.providerType
                    )
                    Timber.d("Successfully preloaded $adKey ad with ${provider.providerType}")
                } else {
                    Timber.w("Failed to preload $adKey ad with ${provider.providerType}")
                }
            } else {
                Timber.w("No provider available for $adKey ad")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error preloading $adKey ad")
        } finally {
            updateLoadingState(adKey, false)
        }
    }

    /**
     * Preload ad ngay lập tức khi cần
     */
    suspend fun preloadAdImmediately(
        context: Context,
        adType: cloud.app.vvf.ads.providers.AdProvider.AdType
    ): Boolean {
        val adKey = when (adType) {
            cloud.app.vvf.ads.providers.AdProvider.AdType.INTERSTITIAL -> "interstitial"
            cloud.app.vvf.ads.providers.AdProvider.AdType.REWARDED -> "rewarded"
            else -> return false
        }

        return withContext(Dispatchers.IO) {
            preloadAdType(context, adType, adKey)
            adCache[adKey] != null
        }
    }

    /**
     * Cập nhật trạng thái loading
     */
    private fun updateLoadingState(adKey: String, isLoading: Boolean) {
        val currentStates = _loadingStates.value.toMutableMap()
        currentStates[adKey] = isLoading
        _loadingStates.value = currentStates
    }

    /**
     * Kiểm tra ad có sẵn sàng không
     */
    fun isAdReady(adType: cloud.app.vvf.ads.providers.AdProvider.AdType): Boolean {
        val adKey = when (adType) {
            cloud.app.vvf.ads.providers.AdProvider.AdType.INTERSTITIAL -> "interstitial"
            cloud.app.vvf.ads.providers.AdProvider.AdType.REWARDED -> "rewarded"
            else -> return false
        }

        val cachedAd = adCache[adKey]
        val isValid = cachedAd != null &&
                     (System.currentTimeMillis() - cachedAd.timestamp) < CACHE_EXPIRY_TIME

        return isValid && waterfallManager.isAdReady(adType)
    }

    /**
     * Lấy thông tin provider cho ad đã cache
     */
    fun getCachedAdProvider(adType: cloud.app.vvf.ads.providers.AdProvider.AdType): cloud.app.vvf.ads.providers.AdProvider.ProviderType? {
        val adKey = when (adType) {
            cloud.app.vvf.ads.providers.AdProvider.AdType.INTERSTITIAL -> "interstitial"
            cloud.app.vvf.ads.providers.AdProvider.AdType.REWARDED -> "rewarded"
            else -> return null
        }

        return adCache[adKey]?.providerType
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        preloadJob?.cancel()
        adCache.clear()
        _loadingStates.value = emptyMap()
    }
}
