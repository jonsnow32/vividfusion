package cloud.app.vvf.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import cloud.app.vvf.ads.providers.*
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdWaterfallManager @Inject constructor() {

    private val providers = mutableListOf<AdProvider>()
    private var isInitialized = false

    // Thống kê performance của từng provider
    private val providerStats = mutableMapOf<AdProvider.ProviderType, ProviderStats>()

    data class ProviderStats(
        var totalRequests: Int = 0,
        var successfulShows: Int = 0,
        var failedShows: Int = 0,
        var loadFailures: Int = 0,
        var avgLoadTime: Long = 0L
    ) {
        val successRate: Float
            get() = if (totalRequests > 0) successfulShows.toFloat() / totalRequests else 0f

        val fillRate: Float
            get() = if (totalRequests > 0) (totalRequests - loadFailures).toFloat() / totalRequests else 0f
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
            // Preload interstitial và rewarded ads
            launch { provider.preloadAd(context, AdProvider.AdType.INTERSTITIAL) }
            launch { provider.preloadAd(context, AdProvider.AdType.REWARDED) }
        } catch (e: Exception) {
            Timber.w(e, "Error preloading ads for ${provider.providerType}")
        }
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
                            // Preload lại ad cho lần sau
                            GlobalScope.launch {
                                provider.preloadAd(activity, AdProvider.AdType.INTERSTITIAL)
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
                            // Preload lại ad cho lần sau
                            GlobalScope.launch {
                                provider.preloadAd(activity, AdProvider.AdType.REWARDED)
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
     * Cleanup tất cả providers
     */
    fun cleanup() {
        providers.forEach { it.cleanup() }
        providers.clear()
        providerStats.clear()
        isInitialized = false
    }
}
