package cloud.app.vvf.ads.providers

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

class AdMobProvider : AdProvider {

    override val providerType = AdProvider.ProviderType.ADMOB
    override val priority = 1 // Highest priority

    companion object {
        // Test Ad Unit IDs - replace with real IDs in production
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    override suspend fun initialize(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            MobileAds.initialize(context) { initializationStatus ->
                isInitialized = initializationStatus.adapterStatusMap.isNotEmpty()
                Timber.d("AdMob initialized: $isInitialized")
                continuation.resume(isInitialized)
            }
        } catch (e: Exception) {
            Timber.e(e, "AdMob initialization failed")
            continuation.resume(false)
        }
    }

    override fun isAdReady(adType: AdProvider.AdType): Boolean {
        return when (adType) {
            AdProvider.AdType.BANNER -> isInitialized
            AdProvider.AdType.INTERSTITIAL -> interstitialAd != null
            AdProvider.AdType.REWARDED -> rewardedAd != null
        }
    }

    override suspend fun preloadAd(context: Context, adType: AdProvider.AdType): Boolean {
        if (!isInitialized) return false

        return when (adType) {
            AdProvider.AdType.INTERSTITIAL -> withContext(Dispatchers.Main) { preloadInterstitial(context) }
            AdProvider.AdType.REWARDED -> withContext(Dispatchers.Main) { preloadRewarded(context) }
            AdProvider.AdType.BANNER -> true // Banner is loaded on demand
        }
    }

    private suspend fun preloadInterstitial(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        if (interstitialAd != null) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                Timber.d("AdMob interstitial loaded")
                continuation.resume(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Timber.w("AdMob interstitial failed to load: ${error.message}")
                continuation.resume(false)
            }
        })
    }

    private suspend fun preloadRewarded(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        if (rewardedAd != null) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                Timber.d("AdMob rewarded loaded")
                continuation.resume(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Timber.w("AdMob rewarded failed to load: ${error.message}")
                continuation.resume(false)
            }
        })
    }

    override suspend fun showBannerAd(
        context: Context,
        container: ViewGroup,
        onAdLoaded: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        try {
            // Remove any previous ad views
            container.removeAllViews()
            // Calculate the container width in dp for adaptive banner
            val displayMetrics = context.resources.displayMetrics
            val containerWidthPx = container.width
            val containerWidthDp = if (containerWidthPx > 0) {
                (containerWidthPx / displayMetrics.density).toInt()
            } else {
                // fallback to screen width if container width is not measured yet
                (displayMetrics.widthPixels / displayMetrics.density).toInt()
            }
            val adView = AdView(context).apply {
                adUnitId = BANNER_AD_UNIT_ID
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, containerWidthDp))
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Timber.d("AdMob adaptive banner loaded")
                        onAdLoaded()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.w("AdMob banner failed: ${error.message}")
                        onAdFailed(error.message)
                    }
                }
            }

            container.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
            true
        } catch (e: Exception) {
            Timber.e(e, "AdMob banner error")
            onAdFailed(e.message ?: "Unknown error")
            false
        }
    }

    override suspend fun showInterstitialAd(
        activity: Activity,
        onAdShown: () -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        val ad = interstitialAd
        if (ad == null) {
            onAdFailed("Interstitial not ready")
            return@withContext false
        }

        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Timber.d("AdMob interstitial shown")
                    onAdShown()
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.d("AdMob interstitial dismissed")
                    interstitialAd = null
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.w("AdMob interstitial show failed: ${error.message}")
                    interstitialAd = null
                    onAdFailed(error.message)
                }
            }

            ad.show(activity)
            true
        } catch (e: Exception) {
            Timber.e(e, "AdMob interstitial show error")
            onAdFailed(e.message ?: "Unknown error")
            false
        }
    }

    override suspend fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (Int) -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        val ad = rewardedAd
        if (ad == null) {
            onAdFailed("Rewarded ad not ready")
            return@withContext false
        }

        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Timber.d("AdMob rewarded shown")
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.d("AdMob rewarded dismissed")
                    rewardedAd = null
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.w("AdMob rewarded show failed: ${error.message}")
                    rewardedAd = null
                    onAdFailed(error.message)
                }
            }

            ad.show(activity) { rewardItem ->
                Timber.d("AdMob reward earned: ${rewardItem.amount}")
                onRewardEarned(rewardItem.amount)
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "AdMob rewarded show error")
            onAdFailed(e.message ?: "Unknown error")
            false
        }
    }

    override fun cleanup() {
        interstitialAd = null
        rewardedAd = null
        isInitialized = false
    }
}
