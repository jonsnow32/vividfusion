package cloud.app.vvf.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import cloud.app.vvf.ads.providers.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdWaterfallManager @Inject constructor() {

    private val providers = mutableListOf<AdProvider>()
    private var isInitialized = false

    // Enhanced preload management
    private val preloadJob = AtomicBoolean(false)
    private var preloadScope: CoroutineScope? = null
    private val preloadQueue = ConcurrentHashMap<String, Long>() // provider_adtype -> last_preload_time
    private val preloadInterval = 30_000L // 30 seconds
    private val maxPreloadRetries = 3

    // Thống kê performance của từng provider
    private val providerStats = mutableMapOf<AdProvider.ProviderType, ProviderStats>()

    data class ProviderStats(
        var totalRequests: Int = 0,
        var successfulShows: Int = 0,
        var failedShows: Int = 0,
        var loadFailures: Int = 0,
        var preloadAttempts: Int = 0,
        var preloadSuccesses: Int = 0,
        var avgLoadTime: Long = 0L
    ) {
        val successRate: Float
            get() = if (totalRequests > 0) successfulShows.toFloat() / totalRequests else 0f

        val fillRate: Float
            get() = if (totalRequests > 0) (totalRequests - loadFailures).toFloat() / totalRequests else 0f

        val preloadSuccessRate: Float
            get() = if (preloadAttempts > 0) preloadSuccesses.toFloat() / preloadAttempts else 0f
    }

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        // Thêm các providers theo thứ tự ưu tiên
        providers.addAll(listOf(
            AdMobProvider(),           // Priority 1 - AdMob (cao nhất)
            FacebookAdProvider(),     // Priority 2 - Facebook Audience Network
            UnityAdProvider(),        // Priority 3 - Unity Ads
            // Tạm thời bỏ Vungle vì dependency issues
            // Có thể thêm IronSource hoặc AppLovin providers khác...
        ))

        // Initialize tất cả providers song song
        val initJobs = providers.map { provider ->
            async {
                try {
                    val startTime = System.currentTimeMillis()
                    val success = provider.initialize(context)
                    val initTime = System.currentTimeMillis() - startTime

                    if (success) {
                        Timber.i("${provider.providerType} initialized successfully in ${initTime}ms")
                        // Preload ads cho providers đã init thành công
                        preloadAdsForProvider(context, provider)
                    } else {
                        Timber.w("${provider.providerType} initialization failed")
                    }

                    // Initialize stats
                    providerStats[provider.providerType] = ProviderStats()

                    success
                } catch (e: Exception) {
                    Timber.e(e, "Error initializing ${provider.providerType}")
                    false
                }
            }
        }

        val results = initJobs.awaitAll()
        val successCount = results.count { it }

        Timber.i("Ad Waterfall initialized: $successCount/${providers.size} providers ready")
        isInitialized = true
    }

    private suspend fun preloadAdsForProvider(context: Context, provider: AdProvider) = withContext(Dispatchers.IO) {
        try {
            val jobs = listOf(
                async { preloadAdWithRetry(context, provider, AdProvider.AdType.INTERSTITIAL) },
                async { preloadAdWithRetry(context, provider, AdProvider.AdType.REWARDED) },
                async { preloadAdWithRetry(context, provider, AdProvider.AdType.BANNER) }
            )
            jobs.awaitAll()
        } catch (e: Exception) {
            Timber.w(e, "Error preloading ads for ${provider.providerType}")
        }
    }

    /**
     * Preload ad with retry mechanism
     */
    private suspend fun preloadAdWithRetry(
        context: Context,
        provider: AdProvider,
        adType: AdProvider.AdType,
        retryCount: Int = 0
    ): Boolean {
        val stats = providerStats[provider.providerType]
        stats?.preloadAttempts?.let { stats.preloadAttempts++ }

        return try {
            val key = "${provider.providerType}_${adType}"
            val lastPreload = preloadQueue[key] ?: 0L
            val currentTime = System.currentTimeMillis()

            // Don't preload if recently preloaded
            if (currentTime - lastPreload < preloadInterval) {
                return true
            }

            // Check if ad is already ready
            if (provider.isAdReady(adType)) {
                Timber.d("Ad already ready for ${provider.providerType} - $adType")
                return true
            }

            Timber.d("Preloading $adType for ${provider.providerType}")
            val startTime = System.currentTimeMillis()
            val success = provider.preloadAd(context, adType)
            val loadTime = System.currentTimeMillis() - startTime

            if (success) {
                preloadQueue[key] = currentTime
                stats?.preloadSuccesses?.let { stats.preloadSuccesses++ }
                Timber.i("Successfully preloaded $adType for ${provider.providerType} in ${loadTime}ms")
            } else if (retryCount < maxPreloadRetries) {
                Timber.w("Preload failed for ${provider.providerType} - $adType, retrying... ($retryCount/$maxPreloadRetries)")
                delay(1000L * (retryCount + 1)) // Exponential backoff
                return preloadAdWithRetry(context, provider, adType, retryCount + 1)
            } else {
                Timber.w("Preload failed for ${provider.providerType} - $adType after $maxPreloadRetries retries")
            }

            success
        } catch (e: Exception) {
            if (retryCount < maxPreloadRetries) {
                Timber.w(e, "Exception during preload for ${provider.providerType} - $adType, retrying...")
                delay(1000L * (retryCount + 1))
                return preloadAdWithRetry(context, provider, adType, retryCount + 1)
            } else {
                Timber.e(e, "Exception during preload for ${provider.providerType} - $adType")
                false
            }
        }
    }

    /**
     * Start periodic ad preloading
     */
    fun startPeriodicPreload(context: Context) {
        if (preloadJob.getAndSet(true)) {
            Timber.d("Periodic preload already running")
            return
        }

        preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        preloadScope?.launch {
            while (isActive && isInitialized) {
                try {
                    preloadAllAds(context)
                    delay(preloadInterval * 2) // Check every minute
                } catch (e: Exception) {
                    Timber.e(e, "Error in periodic preload")
                    delay(10_000L) // Wait 10 seconds before retry
                }
            }
        }

        Timber.i("Started periodic ad preloading")
    }

    /**
     * Stop periodic ad preloading
     */
    fun stopPeriodicPreload() {
        if (preloadJob.getAndSet(false)) {
            preloadScope?.cancel()
            preloadScope = null
            Timber.i("Stopped periodic ad preloading")
        }
    }

    /**
     * Preload ads for all providers
     */
    suspend fun preloadAllAds(context: Context) = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext

        val activeProviders = providers.filter { provider ->
            try {
                // Only preload for providers that are properly initialized
                provider.isAdReady(AdProvider.AdType.BANNER) ||
                provider.isAdReady(AdProvider.AdType.INTERSTITIAL) ||
                provider.isAdReady(AdProvider.AdType.REWARDED) ||
                !provider.isAdReady(AdProvider.AdType.BANNER) // New providers
            } catch (e: Exception) {
                Timber.w(e, "Error checking provider ${provider.providerType} readiness")
                false
            }
        }

        val preloadJobs = activeProviders.map { provider ->
            async {
                try {
                    preloadAdsForProvider(context, provider)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to preload ads for ${provider.providerType}")
                }
            }
        }

        preloadJobs.awaitAll()
        Timber.d("Completed preload cycle for ${activeProviders.size} providers")
    }

    /**
     * Force preload specific ad type for all providers
     */
    suspend fun forcePreloadAdType(context: Context, adType: AdProvider.AdType) = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext

        val jobs = providers.map { provider ->
            async {
                try {
                    // Clear preload timestamp to force reload
                    val key = "${provider.providerType}_${adType}"
                    preloadQueue.remove(key)
                    preloadAdWithRetry(context, provider, adType)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to force preload $adType for ${provider.providerType}")
                    false
                }
            }
        }

        val results = jobs.awaitAll()
        val successCount = results.count { it }

        Timber.i("Force preload $adType completed: $successCount/${providers.size} successful")
    }

    /**
     * Hiển thị banner ad - thử từng provider theo thứ tự ưu tiên
     */
    suspend fun showBannerAd(
        context: Context,
        container: ViewGroup,
        onAdLoaded: () -> Unit = {},
        onAllProvidersFailed: (String) -> Unit = {}
    ): Boolean {
        if (!isInitialized) {
            onAllProvidersFailed("Waterfall not initialized")
            return false
        }

        val sortedProviders = providers.sortedBy { it.priority }

        for (provider in sortedProviders) {
            try {
                if (provider.isAdReady(AdProvider.AdType.BANNER)) {
                    Timber.d("Trying banner with ${provider.providerType}")

                    val startTime = System.currentTimeMillis()
                    val success = provider.showBannerAd(
                        context = context,
                        container = container,
                        onAdLoaded = {
                            updateStats(provider.providerType, true, System.currentTimeMillis() - startTime)
                            onAdLoaded()
                        },
                        onAdFailed = { error ->
                            updateStats(provider.providerType, false, System.currentTimeMillis() - startTime)
                            Timber.w("${provider.providerType} banner failed: $error")
                        }
                    )

                    if (success) {
                        Timber.i("Banner shown successfully with ${provider.providerType}")
                        return true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error showing banner with ${provider.providerType}")
            }
        }

        val errorMsg = "All providers failed to show banner ad"
        Timber.w(errorMsg)
        onAllProvidersFailed(errorMsg)
        return false
    }

    /**
     * Hiển thị interstitial ad - thử từng provider theo thứ tự ưu tiên
     */
    suspend fun showInterstitialAd(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdClosed: () -> Unit = {},
        onAllProvidersFailed: (String) -> Unit = {}
    ): Boolean {
        if (!isInitialized) {
            onAllProvidersFailed("Waterfall not initialized")
            return false
        }

        val sortedProviders = providers.sortedBy { it.priority }

        for (provider in sortedProviders) {
            try {
                if (provider.isAdReady(AdProvider.AdType.INTERSTITIAL)) {
                    Timber.d("Trying interstitial with ${provider.providerType}")

                    val startTime = System.currentTimeMillis()
                    val success = provider.showInterstitialAd(
                        activity = activity,
                        onAdShown = {
                            updateStats(provider.providerType, true, System.currentTimeMillis() - startTime)
                            onAdShown()
                        },
                        onAdClosed = {
                            // Enhanced preload after ad consumption
                            preloadScope?.launch {
                                preloadAdWithRetry(activity, provider, AdProvider.AdType.INTERSTITIAL)
                            }
                            onAdClosed()
                        },
                        onAdFailed = { error ->
                            updateStats(provider.providerType, false, System.currentTimeMillis() - startTime)
                            Timber.w("${provider.providerType} interstitial failed: $error")
                        }
                    )

                    if (success) {
                        Timber.i("Interstitial shown successfully with ${provider.providerType}")
                        return true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error showing interstitial with ${provider.providerType}")
            }
        }

        val errorMsg = "All providers failed to show interstitial ad"
        Timber.w(errorMsg)
        onAllProvidersFailed(errorMsg)
        return false
    }

    /**
     * Hiển thị rewarded ad - thử từng provider theo thứ tự ưu tiên
     */
    suspend fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (Int) -> Unit = {},
        onAdClosed: () -> Unit = {},
        onAllProvidersFailed: (String) -> Unit = {}
    ): Boolean {
        if (!isInitialized) {
            onAllProvidersFailed("Waterfall not initialized")
            return false
        }

        val sortedProviders = providers.sortedBy { it.priority }

        for (provider in sortedProviders) {
            try {
                if (provider.isAdReady(AdProvider.AdType.REWARDED)) {
                    Timber.d("Trying rewarded with ${provider.providerType}")

                    val startTime = System.currentTimeMillis()
                    val success = provider.showRewardedAd(
                        activity = activity,
                        onRewardEarned = { amount ->
                            updateStats(provider.providerType, true, System.currentTimeMillis() - startTime)
                            onRewardEarned(amount)
                        },
                        onAdClosed = {
                            // Enhanced preload after ad consumption
                            preloadScope?.launch {
                                preloadAdWithRetry(activity, provider, AdProvider.AdType.REWARDED)
                            }
                            onAdClosed()
                        },
                        onAdFailed = { error ->
                            updateStats(provider.providerType, false, System.currentTimeMillis() - startTime)
                            Timber.w("${provider.providerType} rewarded failed: $error")
                        }
                    )

                    if (success) {
                        Timber.i("Rewarded shown successfully with ${provider.providerType}")
                        return true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error showing rewarded with ${provider.providerType}")
            }
        }

        val errorMsg = "All providers failed to show rewarded ad"
        Timber.w(errorMsg)
        onAllProvidersFailed(errorMsg)
        return false
    }

    /**
     * Kiểm tra ad có sẵn hay không từ bất kỳ provider nào
     */
    fun isAdReady(adType: AdProvider.AdType): Boolean {
        return providers.any { it.isAdReady(adType) }
    }

    /**
     * Lấy provider đang có ad sẵn sàng với priority cao nhất
     */
    fun getReadyProvider(adType: AdProvider.AdType): AdProvider? {
        return providers
            .filter { it.isAdReady(adType) }
            .minByOrNull { it.priority }
    }

    /**
     * Cập nhật thống kê performance
     */
    private fun updateStats(providerType: AdProvider.ProviderType, success: Boolean, loadTime: Long) {
        val stats = providerStats[providerType] ?: return

        stats.totalRequests++
        if (success) {
            stats.successfulShows++
        } else {
            stats.failedShows++
        }

        // Cập nhật average load time
        stats.avgLoadTime = (stats.avgLoadTime + loadTime) / 2

        Timber.d("${providerType} stats - Success rate: ${(stats.successRate * 100).toInt()}%, Avg load: ${stats.avgLoadTime}ms")
    }

    /**
     * Lấy thống kê performance của tất cả providers
     */
    fun getProviderStats(): Map<AdProvider.ProviderType, ProviderStats> {
        return providerStats.toMap()
    }

    /**
     * Get preload statistics
     */
    fun getPreloadStats(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()

        stats["periodic_preload_active"] = preloadJob.get()
        stats["preload_queue_size"] = preloadQueue.size
        stats["total_providers"] = providers.size
        stats["ready_providers"] = providers.count { provider ->
            AdProvider.AdType.values().any { adType ->
                try {
                    provider.isAdReady(adType)
                } catch (e: Exception) {
                    false
                }
            }
        }

        // Provider-specific preload stats
        providerStats.forEach { (providerType, providerStats) ->
            stats["${providerType}_preload_success_rate"] =
                "${(providerStats.preloadSuccessRate * 100).toInt()}%"
            stats["${providerType}_preload_attempts"] = providerStats.preloadAttempts
            stats["${providerType}_preload_successes"] = providerStats.preloadSuccesses
        }

        return stats
    }

    /**
     * Cleanup tất cả providers
     */
    fun cleanup() {
        stopPeriodicPreload()
        providers.forEach { it.cleanup() }
        providers.clear()
        providerStats.clear()
        preloadQueue.clear()
        isInitialized = false
    }
}
