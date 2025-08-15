package cloud.app.vvf.ads.providers

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.facebook.ads.*
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class FacebookAdProvider : AdProvider {

    override val providerType = AdProvider.ProviderType.FACEBOOK
    override val priority = 2 // Priority 2

    companion object {
        // Test Placement IDs - replace with real IDs in production
        const val BANNER_PLACEMENT_ID = "YOUR_PLACEMENT_ID"
        const val INTERSTITIAL_PLACEMENT_ID = "YOUR_PLACEMENT_ID"
        const val REWARDED_PLACEMENT_ID = "YOUR_PLACEMENT_ID"
    }

    private var interstitialAd: com.facebook.ads.InterstitialAd? = null
    private var rewardedVideoAd: RewardedVideoAd? = null
    private var isInitialized = false

    override suspend fun initialize(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Facebook AudienceNetwork does not require an initialization callback
            // Just call initialize
            AudienceNetworkAds.initialize(context)
            isInitialized = true
            Timber.d("Facebook Audience Network initialized")
            continuation.resume(true)
        } catch (e: Exception) {
            Timber.e(e, "Facebook Audience Network initialization failed")
            continuation.resume(false)
        }
    }

    override fun isAdReady(adType: AdProvider.AdType): Boolean {
        return when (adType) {
            AdProvider.AdType.BANNER -> isInitialized
            AdProvider.AdType.INTERSTITIAL -> interstitialAd?.isAdLoaded == true
            AdProvider.AdType.REWARDED -> rewardedVideoAd?.isAdLoaded == true
        }
    }

    override suspend fun preloadAd(context: Context, adType: AdProvider.AdType): Boolean {
        if (!isInitialized) return false

        return when (adType) {
            AdProvider.AdType.INTERSTITIAL -> preloadInterstitial(context)
            AdProvider.AdType.REWARDED -> preloadRewarded(context)
            AdProvider.AdType.BANNER -> true
        }
    }

    private suspend fun preloadInterstitial(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            interstitialAd = com.facebook.ads.InterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)

            val adListener = object : InterstitialAdListener {
                override fun onInterstitialDisplayed(ad: Ad?) {
                    Timber.d("Facebook interstitial displayed")
                }

                override fun onInterstitialDismissed(ad: Ad?) {
                    Timber.d("Facebook interstitial dismissed")
                }

                override fun onError(ad: Ad?, error: AdError?) {
                    Timber.w("Facebook interstitial load error: ${error?.errorMessage}")
                    continuation.resume(false)
                }

                override fun onAdLoaded(ad: Ad?) {
                    Timber.d("Facebook interstitial loaded")
                    continuation.resume(true)
                }

                override fun onAdClicked(ad: Ad?) {
                    Timber.d("Facebook interstitial clicked")
                }

                override fun onLoggingImpression(ad: Ad?) {}
            }

            interstitialAd?.loadAd(interstitialAd?.buildLoadAdConfig()?.withAdListener(adListener)?.build())
        } catch (e: Exception) {
            Timber.e(e, "Facebook interstitial preload error")
            continuation.resume(false)
        }
    }

    private suspend fun preloadRewarded(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            rewardedVideoAd = RewardedVideoAd(context, REWARDED_PLACEMENT_ID)

            val adListener = object : RewardedVideoAdListener {
                override fun onRewardedVideoCompleted() {
                    Timber.d("Facebook rewarded completed")
                }

                override fun onRewardedVideoClosed() {
                    Timber.d("Facebook rewarded closed")
                }

                override fun onError(ad: Ad?, error: AdError?) {
                    Timber.w("Facebook rewarded load error: ${error?.errorMessage}")
                    continuation.resume(false)
                }

                override fun onAdLoaded(ad: Ad?) {
                    Timber.d("Facebook rewarded loaded")
                    continuation.resume(true)
                }

                override fun onAdClicked(ad: Ad?) {
                    Timber.d("Facebook rewarded clicked")
                }

                override fun onLoggingImpression(ad: Ad?) {}
            }

            rewardedVideoAd?.loadAd(rewardedVideoAd?.buildLoadAdConfig()?.withAdListener(adListener)?.build())
        } catch (e: Exception) {
            Timber.e(e, "Facebook rewarded preload error")
            continuation.resume(false)
        }
    }

    override suspend fun showBannerAd(
        context: Context,
        container: ViewGroup,
        onAdLoaded: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean {
        return try {
            val adView = AdView(context, BANNER_PLACEMENT_ID, AdSize.BANNER_HEIGHT_50)

            val adListener = object : AdListener {
                override fun onError(ad: Ad?, error: AdError?) {
                    Timber.w("Facebook banner error: ${error?.errorMessage}")
                    onAdFailed(error?.errorMessage ?: "Unknown error")
                }

                override fun onAdLoaded(ad: Ad?) {
                    Timber.d("Facebook banner loaded")
                    onAdLoaded()
                }

                override fun onAdClicked(ad: Ad?) {}
                override fun onLoggingImpression(ad: Ad?) {}
            }

            container.addView(adView)
            adView.loadAd(adView.buildLoadAdConfig().withAdListener(adListener).build())
            true
        } catch (e: Exception) {
            Timber.e(e, "Facebook banner error")
            onAdFailed(e.message ?: "Unknown error")
            false
        }
    }

    override suspend fun showInterstitialAd(
        activity: Activity,
        onAdShown: () -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean {
        val ad = interstitialAd
        if (ad?.isAdLoaded != true) {
            onAdFailed("Facebook interstitial not ready")
            return false
        }

        return try {
            val showListener = object : InterstitialAdListener {
                override fun onInterstitialDisplayed(ad: Ad?) {
                    onAdShown()
                }

                override fun onInterstitialDismissed(ad: Ad?) {
                    onAdClosed()
                    interstitialAd = null
                }

                override fun onError(ad: Ad?, error: AdError?) {
                    onAdFailed(error?.errorMessage ?: "Show error")
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: Ad?) {}
                override fun onAdClicked(ad: Ad?) {}
                override fun onLoggingImpression(ad: Ad?) {}
            }

            ad.show()
            true
        } catch (e: Exception) {
            Timber.e(e, "Facebook interstitial show error")
            onAdFailed(e.message ?: "Unknown error")
            false
        }
    }

    override suspend fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (Int) -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean {
        val ad = rewardedVideoAd
        if (ad?.isAdLoaded != true) {
            onAdFailed("Facebook rewarded not ready")
            return false
        }

        return try {
            val showListener = object : RewardedVideoAdListener {
                override fun onRewardedVideoCompleted() {
                    onRewardEarned(1) // Facebook does not provide a specific reward amount
                }

                override fun onRewardedVideoClosed() {
                    onAdClosed()
                    rewardedVideoAd = null
                }

                override fun onError(ad: Ad?, error: AdError?) {
                    onAdFailed(error?.errorMessage ?: "Show error")
                    rewardedVideoAd = null
                }

                override fun onAdLoaded(ad: Ad?) {}
                override fun onAdClicked(ad: Ad?) {}
                override fun onLoggingImpression(ad: Ad?) {}
            }

            ad.show()
            true
        } catch (e: Exception) {
            Timber.e(e, "Facebook rewarded show error")
            onAdFailed(e.message ?: "Unknown error")
            false
        }
    }

    override fun cleanup() {
        interstitialAd?.destroy()
        rewardedVideoAd?.destroy()
        interstitialAd = null
        rewardedVideoAd = null
        isInitialized = false
    }
}
