package cloud.app.vvf.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import cloud.app.vvf.ads.providers.AdProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    private val waterfallManager: AdWaterfallManager,
    private val preloadManager: AdPreloadManager,
    private val loadStrategy: AdLoadStrategy
) : DefaultLifecycleObserver {

    companion object {
        private const val AD_FREQUENCY_LIMIT = 3 // Show interstitial every 3 actions
        private const val MIN_TIME_BETWEEN_ADS = 30_000L // 30 seconds
    }

    // Ad frequency control
    private var actionCount = 0
    private var lastAdShownTime = 0L

    fun initialize(context: Context) {
        GlobalScope.launch {
            try {
                waterfallManager.initialize(context)

                // Start intelligent preloading
                preloadManager.startIntelligentPreload(true)

                // Load ads at app startup
                loadStrategy.loadAdsForContext(
                    context,
                    AdLoadStrategy.LoadTrigger.APP_START,
                    AdLoadStrategy.AdPriority.HIGH
                )

                Timber.i("AdManager with optimized waterfall initialized successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize AdManager waterfall")
            }
        }
    }

    /**
     * Create banner ad using waterfall system (optimized)
     */
    suspend fun createBannerAd(context: Context, container: ViewGroup): Boolean {
        return try {
            // Preload banner ad if needed
            val success = waterfallManager.showBannerAd(
                context = context,
                container = container,
                onAdLoaded = {
                    Timber.d("Optimized banner ad loaded successfully")
                },
                onAllProvidersFailed = { error ->
                    Timber.w("All waterfall providers failed for banner: $error")
                    // Trigger reload for next time
                    GlobalScope.launch {
                        delay(30000L) // Retry after 30s
                        loadStrategy.loadAdsForContext(
                            context,
                            AdLoadStrategy.LoadTrigger.USER_IDLE
                        )
                    }
                }
            )
            success
        } catch (e: Exception) {
            Timber.e(e, "Error creating optimized banner ad")
            false
        }
    }

    /**
     * Show interstitial ad with smart preloading
     */
    fun showInterstitialAd(activity: Activity, onAdClosed: (() -> Unit)? = null) {
        actionCount++

        // Check frequency and time limits
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastAdShownTime

        if (actionCount < AD_FREQUENCY_LIMIT || timeSinceLastAd < MIN_TIME_BETWEEN_ADS) {
            onAdClosed?.invoke()
            return
        }

        GlobalScope.launch {
            // Check if ad is ready
            if (!waterfallManager.isAdReady(AdProvider.AdType.INTERSTITIAL)) {
                // Load immediately if not available
                val loaded = loadStrategy.loadAdImmediately(
                    activity,
                    AdProvider.AdType.INTERSTITIAL
                )
                if (!loaded) {
                    Timber.w("Failed to load interstitial ad immediately")
                    onAdClosed?.invoke()
                    return@launch
                }
            }

            val success = waterfallManager.showInterstitialAd(
                activity = activity,
                onAdShown = {
                    lastAdShownTime = System.currentTimeMillis()
                    Timber.d("Optimized interstitial ad shown")
                },
                onAdClosed = {
                    actionCount = 0 // Reset counter

                    // Preload ad for next time
                    GlobalScope.launch {
                        loadStrategy.loadAdsForContext(
                            activity,
                            AdLoadStrategy.LoadTrigger.VIDEO_COMPLETE
                        )
                    }

                    onAdClosed?.invoke()
                    Timber.d("Optimized interstitial ad closed")
                },
                onAllProvidersFailed = { error ->
                    Timber.w("All providers failed for interstitial: $error")

                    // Retry strategy
                    GlobalScope.launch {
                        delay(60000L) // Retry after 1 minute
                        loadStrategy.loadAdsForContext(
                            activity,
                            AdLoadStrategy.LoadTrigger.USER_IDLE,
                            AdLoadStrategy.AdPriority.LOW
                        )
                    }

                    onAdClosed?.invoke()
                }
            )

            if (!success) {
                onAdClosed?.invoke()
            }
        }
    }

    /**
     * Show rewarded ad with smart preloading
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (rewardAmount: Int) -> Unit,
        onAdClosed: (() -> Unit)? = null
    ) {
        GlobalScope.launch {
            // Check if ad is ready
            if (!waterfallManager.isAdReady(AdProvider.AdType.REWARDED)) {
                // Load immediately if not available
                val loaded = loadStrategy.loadAdImmediately(
                    activity,
                    AdProvider.AdType.REWARDED
                )
                if (!loaded) {
                    Timber.w("Failed to load rewarded ad immediately")
                    onAdClosed?.invoke()
                    return@launch
                }
            }

            waterfallManager.showRewardedAd(
                activity = activity,
                onRewardEarned = { amount ->
                    Timber.d("User earned reward from optimized waterfall: $amount")
                    onUserEarnedReward(amount)
                },
                onAdClosed = {
                    // Preload ad for next time
                    GlobalScope.launch {
                        loadStrategy.loadAdsForContext(
                            activity,
                            AdLoadStrategy.LoadTrigger.VIDEO_COMPLETE
                        )
                    }

                    onAdClosed?.invoke()
                    Timber.d("Optimized rewarded ad closed")
                },
                onAllProvidersFailed = { error ->
                    Timber.w("All providers failed for rewarded: $error")

                    // Retry strategy
                    GlobalScope.launch {
                        delay(60000L)
                        loadStrategy.loadAdsForContext(
                            activity,
                            AdLoadStrategy.LoadTrigger.USER_IDLE,
                            AdLoadStrategy.AdPriority.LOW
                        )
                    }

                    onAdClosed?.invoke()
                }
            )
        }
    }

    /**
     * Trigger ad loading based on user behavior
     */
    fun onUserAction(context: Context, trigger: AdLoadStrategy.LoadTrigger) {
        GlobalScope.launch {
            loadStrategy.loadAdsForContext(context, trigger)
        }
    }

    /**
     * Check if rewarded ad is available (optimized)
     */
    fun isRewardedAdReady(): Boolean = waterfallManager.isAdReady(AdProvider.AdType.REWARDED)

    /**
     * Check if interstitial ad is available (optimized)
     */
    fun isInterstitialAdReady(): Boolean = waterfallManager.isAdReady(AdProvider.AdType.INTERSTITIAL)

    /**
     * Get provider performance stats
     */
    fun getProviderStats() = waterfallManager.getProviderStats()

    /**
     * Get which provider is ready for specific ad type (optimized)
     */
    fun getReadyProvider(adType: AdProvider.AdType): AdProvider? {
        return waterfallManager.getReadyProvider(adType)
    }

    /**
     * Get detailed ad status for debugging
     */
    fun getAdStatus(): Map<String, Any> {
        return mapOf(
            "interstitial_ready" to isInterstitialAdReady(),
            "rewarded_ready" to isRewardedAdReady(),
            "interstitial_provider" to (waterfallManager.getReadyProvider(AdProvider.AdType.INTERSTITIAL)?.providerType?.name ?: "none"),
            "rewarded_provider" to (waterfallManager.getReadyProvider(AdProvider.AdType.REWARDED)?.providerType?.name ?: "none"),
            "loading_stats" to loadStrategy.getLoadingStats(),
            "provider_stats" to getProviderStats()
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        loadStrategy.cleanup()
        preloadManager.cleanup()
        waterfallManager.cleanup()
    }
}
