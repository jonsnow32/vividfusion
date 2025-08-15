package cloud.app.vvf.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import cloud.app.vvf.ads.providers.AdProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    private val waterfallManager: AdWaterfallManager
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
                Timber.i("AdManager with waterfall initialized successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize AdManager waterfall")
            }
        }
    }

    /**
     * Create banner ad using waterfall system
     */
    suspend fun createBannerAd(context: Context, container: ViewGroup): Boolean {
        return waterfallManager.showBannerAd(
            context = context,
            container = container,
            onAdLoaded = {
                Timber.d("Waterfall banner ad loaded successfully")
            },
            onAllProvidersFailed = { error ->
                Timber.w("All waterfall providers failed for banner: $error")
            }
        )
    }

    /**
     * Show interstitial ad with frequency control using waterfall
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
            val success = waterfallManager.showInterstitialAd(
                activity = activity,
                onAdShown = {
                    lastAdShownTime = System.currentTimeMillis()
                    Timber.d("Waterfall interstitial ad shown")
                },
                onAdClosed = {
                    actionCount = 0 // Reset counter
                    onAdClosed?.invoke()
                    Timber.d("Waterfall interstitial ad closed")
                },
                onAllProvidersFailed = { error ->
                    Timber.w("All waterfall providers failed for interstitial: $error")
                    onAdClosed?.invoke()
                }
            )

            if (!success) {
                onAdClosed?.invoke()
            }
        }
    }

    /**
     * Show rewarded ad using waterfall
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (rewardAmount: Int) -> Unit,
        onAdClosed: (() -> Unit)? = null
    ) {
        GlobalScope.launch {
            waterfallManager.showRewardedAd(
                activity = activity,
                onRewardEarned = { amount ->
                    Timber.d("User earned reward from waterfall: $amount")
                    onUserEarnedReward(amount)
                },
                onAdClosed = {
                    onAdClosed?.invoke()
                    Timber.d("Waterfall rewarded ad closed")
                },
                onAllProvidersFailed = { error ->
                    Timber.w("All waterfall providers failed for rewarded: $error")
                    onAdClosed?.invoke()
                }
            )
        }
    }

    /**
     * Check if rewarded ad is available from any provider
     */
    fun isRewardedAdReady(): Boolean = waterfallManager.isAdReady(AdProvider.AdType.REWARDED)

    /**
     * Check if interstitial ad is available from any provider
     */
    fun isInterstitialAdReady(): Boolean = waterfallManager.isAdReady(AdProvider.AdType.INTERSTITIAL)

    /**
     * Get provider performance stats
     */
    fun getProviderStats() = waterfallManager.getProviderStats()

    /**
     * Get which provider is ready for specific ad type
     */
    fun getReadyProvider(adType: AdProvider.AdType) = waterfallManager.getReadyProvider(adType)

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        waterfallManager.cleanup()
    }
}
