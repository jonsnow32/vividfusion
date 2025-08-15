package cloud.app.vvf.ads

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.ads.providers.AdProvider
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to easily access enhanced ad preload functionality
 */
@Singleton
class AdPreloadHelper @Inject constructor(
    private val adPreloadManager: AdPreloadManager,
    private val adWaterfallManager: AdWaterfallManager
) {

    /**
     * Preload ads before showing them (recommended to call 30 seconds before showing ad)
     */
    fun preloadBeforeShow(context: Context, adType: AdProvider.AdType) {
        try {
            (context as? LifecycleOwner)?.lifecycleScope?.launch {
                adPreloadManager.preloadOnDemand(context, adType)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error preloading ad before show")
        }
    }

    /**
     * Check if ad is ready to be shown
     */
    fun isAdReady(adType: AdProvider.AdType): Boolean {
        return try {
            adWaterfallManager.isAdReady(adType)
        } catch (e: Exception) {
            Timber.w(e, "Error checking ad readiness")
            false
        }
    }

    /**
     * Get preload statistics for debugging/monitoring
     */
    fun getPreloadStats(): Map<String, Any> {
        return try {
            adPreloadManager.getPreloadStats()
        } catch (e: Exception) {
            Timber.w(e, "Error getting preload stats")
            emptyMap()
        }
    }

    /**
     * Update preload settings
     */
    fun updatePreloadSettings(
        enabled: Boolean = true,
        onWifi: Boolean = true,
        onMobile: Boolean = false,
        maxDaily: Int = 50
    ) {
        try {
            adPreloadManager.updateSettings(enabled, onWifi, onMobile, maxDaily)
        } catch (e: Exception) {
            Timber.w(e, "Error updating preload settings")
        }
    }

    /**
     * Force preload all ad types (use sparingly)
     */
    fun forcePreloadAll(context: Context) {
        try {
            (context as? LifecycleOwner)?.lifecycleScope?.launch {
                adWaterfallManager.preloadAllAds(context)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error force preloading all ads")
        }
    }

    /**
     * Show interstitial ad with automatic preload after consumption
     */
    suspend fun showInterstitialWithPreload(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdClosed: () -> Unit = {},
        onAllProvidersFailed: (String) -> Unit = {}
    ): Boolean {
        return try {
            adWaterfallManager.showInterstitialAd(
                activity = activity,
                onAdShown = onAdShown,
                onAdClosed = {
                    onAdClosed()
                    // Automatic preload for next time
                    preloadBeforeShow(activity, AdProvider.AdType.INTERSTITIAL)
                },
                onAllProvidersFailed = onAllProvidersFailed
            )
        } catch (e: Exception) {
            Timber.e(e, "Error showing interstitial with preload")
            onAllProvidersFailed("Error: ${e.message}")
            false
        }
    }

    /**
     * Show rewarded ad with automatic preload after consumption
     */
    suspend fun showRewardedWithPreload(
        activity: Activity,
        onRewardEarned: (Int) -> Unit = {},
        onAdClosed: () -> Unit = {},
        onAllProvidersFailed: (String) -> Unit = {}
    ): Boolean {
        return try {
            adWaterfallManager.showRewardedAd(
                activity = activity,
                onRewardEarned = onRewardEarned,
                onAdClosed = {
                    onAdClosed()
                    // Automatic preload for next time
                    preloadBeforeShow(activity, AdProvider.AdType.REWARDED)
                },
                onAllProvidersFailed = onAllProvidersFailed
            )
        } catch (e: Exception) {
            Timber.e(e, "Error showing rewarded with preload")
            onAllProvidersFailed("Error: ${e.message}")
            false
        }
    }
}
