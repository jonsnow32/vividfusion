package cloud.app.vvf.ads

import android.content.Context
import cloud.app.vvf.ads.providers.AdProvider
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdLoadStrategy @Inject constructor(
    private val adWaterfallManager: AdWaterfallManager,
    private val adPreloadManager: AdPreloadManager
) {

    private var loadScope: CoroutineScope? = null
    private val loadingQueue = mutableMapOf<String, Long>() // adType_trigger -> timestamp
    private val loadCooldown = 10_000L // 10 seconds cooldown between loads

    enum class LoadTrigger {
        APP_START,           // App started
        USER_IDLE,           // User is idle
        VIDEO_COMPLETE,      // After watching video
        LEVEL_COMPLETE,      // After completing a level
        SCREEN_TRANSITION,   // Screen transition
        BEFORE_CONTENT,      // Before showing important content
        NETWORK_AVAILABLE,   // When network is available
        WIFI_CONNECTED,      // When connected to WiFi
        LOW_BATTERY_MODE,    // Battery saving mode
        BACKGROUND_REFRESH   // Background refresh
    }

    enum class AdPriority {
        HIGH,    // Load immediately
        MEDIUM,  // Load within 5 seconds
        LOW      // Load when idle
    }

    init {
        loadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Load ads dựa trên context và trigger
     */
    suspend fun loadAdsForContext(
        context: Context,
        trigger: LoadTrigger,
        priority: AdPriority = AdPriority.MEDIUM
    ) = withContext(Dispatchers.IO) {
        try {
            val strategy = getLoadStrategyForTrigger(trigger)
            val delay = getDelayForPriority(priority)

            if (delay > 0) {
                delay(delay)
            }

            Timber.d("Loading ads for trigger: $trigger with priority: $priority")

            // Execute loading strategy
            when (strategy) {
                StrategyType.PRELOAD_ALL -> {
                    adWaterfallManager.preloadAllAds(context)
                }
                StrategyType.PRELOAD_INTERSTITIAL -> {
                    preloadAdTypeIfNeeded(context, AdProvider.AdType.INTERSTITIAL, trigger)
                }
                StrategyType.PRELOAD_REWARDED -> {
                    preloadAdTypeIfNeeded(context, AdProvider.AdType.REWARDED, trigger)
                }
                StrategyType.PRELOAD_CRITICAL -> {
                    // Load interstitial and rewarded in parallel
                    val jobs = listOf(
                        async { preloadAdTypeIfNeeded(context, AdProvider.AdType.INTERSTITIAL, trigger) },
                        async { preloadAdTypeIfNeeded(context, AdProvider.AdType.REWARDED, trigger) }
                    )
                    jobs.awaitAll()
                }
                StrategyType.SMART_PRELOAD -> {
                    smartPreloadBasedOnUsage(context, trigger)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error loading ads for trigger: $trigger")
        }
    }

    /**
     * Load ad ngay lập tức (blocking)
     */
    suspend fun loadAdImmediately(context: Context, adType: AdProvider.AdType): Boolean {
        return try {
            Timber.d("Loading $adType immediately")

            // Kiểm tra xem ad đã sẵn sàng chưa
            if (adWaterfallManager.isAdReady(adType)) {
                Timber.d("$adType already ready")
                return true
            }

            // Force preload with timeout
            withTimeout(10_000L) { // 10 second timeout
                adWaterfallManager.forcePreloadAdType(context, adType)

                // Wait cho đến khi ad ready hoặc timeout
                var attempts = 0
                while (!adWaterfallManager.isAdReady(adType) && attempts < 20) {
                    delay(500L) // Check every 500ms
                    attempts++
                }

                adWaterfallManager.isAdReady(adType)
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("Timeout loading $adType immediately")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error loading $adType immediately")
            false
        }
    }

    /**
     * Preload ad type nếu cần thiết (với cooldown)
     */
    private suspend fun preloadAdTypeIfNeeded(
        context: Context,
        adType: AdProvider.AdType,
        trigger: LoadTrigger
    ) {
        val key = "${adType}_${trigger}"
        val lastLoad = loadingQueue[key] ?: 0L
        val currentTime = System.currentTimeMillis()

        // Check cooldown
        if (currentTime - lastLoad < loadCooldown) {
            Timber.d("Skipping preload for $adType - cooldown active")
            return
        }

        // Check if already ready
        if (adWaterfallManager.isAdReady(adType)) {
            Timber.d("Skipping preload for $adType - already ready")
            return
        }

        try {
            adPreloadManager.preloadOnDemand(context, adType)
            loadingQueue[key] = currentTime
            Timber.d("Preloaded $adType for trigger: $trigger")
        } catch (e: Exception) {
            Timber.e(e, "Failed to preload $adType for trigger: $trigger")
        }
    }

    /**
     * Smart preload dựa trên usage pattern
     */
    private suspend fun smartPreloadBasedOnUsage(context: Context, trigger: LoadTrigger) {
        try {
            // Lấy thống kê để quyết định strategy
            val stats = adWaterfallManager.getProviderStats()

            // Preload ads dựa trên success rate và usage
            val interstitialReady = adWaterfallManager.isAdReady(AdProvider.AdType.INTERSTITIAL)
            val rewardedReady = adWaterfallManager.isAdReady(AdProvider.AdType.REWARDED)

            // Execute preloads in parallel using coroutineScope
            coroutineScope {
                val preloadJobs = mutableListOf<Deferred<Unit>>()

                // Prioritize interstitial nếu chưa ready
                if (!interstitialReady) {
                    preloadJobs.add(async {
                        preloadAdTypeIfNeeded(context, AdProvider.AdType.INTERSTITIAL, trigger)
                    })
                }

                // Preload rewarded nếu chưa ready
                if (!rewardedReady) {
                    preloadJobs.add(async {
                        preloadAdTypeIfNeeded(context, AdProvider.AdType.REWARDED, trigger)
                    })
                }

                // Preload banner nếu các ad khác đã ready
                if (interstitialReady && rewardedReady) {
                    preloadJobs.add(async {
                        preloadAdTypeIfNeeded(context, AdProvider.AdType.BANNER, trigger)
                    })
                }

                preloadJobs.awaitAll()
            }

            Timber.d("Smart preload completed for trigger: $trigger")

        } catch (e: Exception) {
            Timber.e(e, "Error in smart preload for trigger: $trigger")
        }
    }

    /**
     * Lấy strategy dựa trên trigger
     */
    private fun getLoadStrategyForTrigger(trigger: LoadTrigger): StrategyType {
        return when (trigger) {
            LoadTrigger.APP_START -> StrategyType.PRELOAD_CRITICAL
            LoadTrigger.VIDEO_COMPLETE -> StrategyType.PRELOAD_INTERSTITIAL
            LoadTrigger.LEVEL_COMPLETE -> StrategyType.PRELOAD_REWARDED
            LoadTrigger.USER_IDLE -> StrategyType.SMART_PRELOAD
            LoadTrigger.SCREEN_TRANSITION -> StrategyType.PRELOAD_INTERSTITIAL
            LoadTrigger.BEFORE_CONTENT -> StrategyType.PRELOAD_CRITICAL
            LoadTrigger.NETWORK_AVAILABLE -> StrategyType.PRELOAD_ALL
            LoadTrigger.WIFI_CONNECTED -> StrategyType.PRELOAD_ALL
            LoadTrigger.LOW_BATTERY_MODE -> StrategyType.SMART_PRELOAD
            LoadTrigger.BACKGROUND_REFRESH -> StrategyType.SMART_PRELOAD
        }
    }

    /**
     * Lấy delay dựa trên priority
     */
    private fun getDelayForPriority(priority: AdPriority): Long {
        return when (priority) {
            AdPriority.HIGH -> 0L           // Load ngay
            AdPriority.MEDIUM -> 2_000L     // Load sau 2 giây
            AdPriority.LOW -> 5_000L        // Load sau 5 giây
        }
    }

    /**
     * Strategy types for different loading scenarios
     */
    private enum class StrategyType {
        PRELOAD_ALL,         // Preload tất cả ad types
        PRELOAD_INTERSTITIAL, // Chỉ preload interstitial
        PRELOAD_REWARDED,    // Chỉ preload rewarded
        PRELOAD_CRITICAL,    // Preload interstitial + rewarded
        SMART_PRELOAD        // Preload dựa trên AI/statistics
    }

    /**
     * Get loading statistics
     */
    fun getLoadingStats(): Map<String, Any> {
        return mapOf(
            "active_loads" to loadingQueue.size,
            "last_loads" to loadingQueue.mapValues { System.currentTimeMillis() - it.value },
            "cooldown_ms" to loadCooldown
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        loadScope?.cancel()
        loadScope = null
        loadingQueue.clear()
        Timber.d("AdLoadStrategy cleaned up")
    }
}
