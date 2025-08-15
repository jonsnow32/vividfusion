package cloud.app.vvf.ads

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdView
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdPlacementHelper @Inject constructor(
    private val adManager: AdManager
) {

    /**
     * Strategic ad placements for a media app
     */
    enum class AdPlacement {
        // Video/Media related
        BEFORE_VIDEO_PLAY,
        AFTER_VIDEO_COMPLETE,
        BEFORE_DOWNLOAD_START,
        AFTER_DOWNLOAD_COMPLETE,

        // Navigation related
        FRAGMENT_TRANSITION,
        BACK_FROM_PLAYER,

        // User actions
        SEARCH_RESULTS,
        LIBRARY_BROWSE,
        SETTINGS_EXIT,

        // Rewarded opportunities
        UNLOCK_PREMIUM_FEATURE,
        REMOVE_ADS_TEMPORARILY,
        BONUS_DOWNLOAD_SPEED
    }

    /**
     * Show interstitial ad at strategic moments
     */
    fun showInterstitialForPlacement(
        activity: Activity,
        placement: AdPlacement,
        onComplete: () -> Unit
    ) {
        when (placement) {
            AdPlacement.BEFORE_VIDEO_PLAY -> {
                // Show ad before video starts (but not too frequently)
                adManager.showInterstitialAd(activity) {
                    Timber.d("Interstitial shown before video play")
                    onComplete()
                }
            }

            AdPlacement.AFTER_DOWNLOAD_COMPLETE -> {
                // Show ad after download completes
                adManager.showInterstitialAd(activity) {
                    Timber.d("Interstitial shown after download complete")
                    onComplete()
                }
            }

            AdPlacement.FRAGMENT_TRANSITION -> {
                // Show ad during major navigation
                adManager.showInterstitialAd(activity) {
                    Timber.d("Interstitial shown during fragment transition")
                    onComplete()
                }
            }

            else -> {
                // For other placements, show with normal frequency control
                adManager.showInterstitialAd(activity, onComplete)
            }
        }
    }

    /**
     * Show rewarded ad for premium features
     */
    fun showRewardedForFeature(
        activity: Activity,
        placement: AdPlacement,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onComplete: () -> Unit
    ) {
        when (placement) {
            AdPlacement.UNLOCK_PREMIUM_FEATURE -> {
                adManager.showRewardedAd(
                    activity,
                    onUserEarnedReward = { amount ->
                        Timber.d("User earned reward for premium feature: $amount")
                        onRewardEarned(amount)
                    },
                    onAdClosed = onComplete
                )
            }

            AdPlacement.BONUS_DOWNLOAD_SPEED -> {
                adManager.showRewardedAd(
                    activity,
                    onUserEarnedReward = { amount ->
                        Timber.d("User earned reward for download speed boost: $amount")
                        onRewardEarned(amount)
                    },
                    onAdClosed = onComplete
                )
            }

            AdPlacement.REMOVE_ADS_TEMPORARILY -> {
                adManager.showRewardedAd(
                    activity,
                    onUserEarnedReward = { amount ->
                        Timber.d("User earned reward for temporary ad removal: $amount")
                        onRewardEarned(amount)
                    },
                    onAdClosed = onComplete
                )
            }

            else -> {
                adManager.showRewardedAd(activity, onRewardEarned, onComplete)
            }
        }
    }

    /**
     * Add banner ad to a container with proper sizing
     */
    fun addBannerToContainer(
        fragment: Fragment,
        container: ViewGroup,
        atTop: Boolean = false
    ): AdView? {
        return try {
            val bannerAd = adManager.createBannerAd(fragment.requireContext())

            if (atTop) {
                container.addView(bannerAd, 0)
            } else {
                container.addView(bannerAd)
            }

            Timber.d("Banner ad added to container")
            bannerAd
        } catch (e: Exception) {
            Timber.w(e, "Failed to add banner ad to container")
            null
        }
    }

    /**
     * Check if ads should be shown (for premium users, etc.)
     */
    fun shouldShowAds(): Boolean {
        // TODO: Implement premium user check
        // return !userRepository.isPremiumUser()
        return true
    }

    /**
     * Check if rewarded ad is available for a feature
     */
    fun isRewardedAdAvailableForFeature(placement: AdPlacement): Boolean {
        return when (placement) {
            AdPlacement.UNLOCK_PREMIUM_FEATURE,
            AdPlacement.BONUS_DOWNLOAD_SPEED,
            AdPlacement.REMOVE_ADS_TEMPORARILY -> adManager.isRewardedAdReady()
            else -> false
        }
    }
}
