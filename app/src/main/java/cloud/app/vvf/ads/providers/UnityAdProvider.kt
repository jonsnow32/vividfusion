package cloud.app.vvf.ads.providers

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.unity3d.ads.*
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class UnityAdProvider : AdProvider {

    override val providerType = AdProvider.ProviderType.UNITY
    override val priority = 3 // Ưu tiên thứ 3

    companion object {
        const val GAME_ID = "4374881" // Test Game ID - thay bằng ID thật
        const val BANNER_PLACEMENT_ID = "banner"
        const val INTERSTITIAL_PLACEMENT_ID = "video"
        const val REWARDED_PLACEMENT_ID = "rewardedVideo"
    }

    private var isInitialized = false

    override suspend fun initialize(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            UnityAds.initialize(context, GAME_ID, BuildConfig.DEBUG, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitialized = true
                    Timber.d("Unity Ads initialized successfully")
                    continuation.resume(true)
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                    Timber.w("Unity Ads initialization failed: $message")
                    continuation.resume(false)
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "Unity Ads initialization error")
            continuation.resume(false)
        }
    }

    override fun isAdReady(adType: AdProvider.AdType): Boolean {
        if (!isInitialized) return false

        return when (adType) {
            AdProvider.AdType.BANNER -> true // Banner luôn sẵn sàng nếu đã init
            AdProvider.AdType.INTERSTITIAL -> UnityAds.isInitialized() && UnityAds.getDebugMode() != null
            AdProvider.AdType.REWARDED -> UnityAds.isInitialized() && UnityAds.getDebugMode() != null
        }
    }

    override suspend fun preloadAd(context: Context, adType: AdProvider.AdType): Boolean {
        if (!isInitialized) return false

        return when (adType) {
            AdProvider.AdType.INTERSTITIAL -> {
                UnityAds.load(INTERSTITIAL_PLACEMENT_ID)
                true
            }
            AdProvider.AdType.REWARDED -> {
                UnityAds.load(REWARDED_PLACEMENT_ID)
                true
            }
            AdProvider.AdType.BANNER -> true
        }
    }

    override suspend fun showBannerAd(
        context: Context,
        container: ViewGroup,
        onAdLoaded: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean {
        return try {
            val bannerView = BannerView(context as Activity, BANNER_PLACEMENT_ID, UnityBannerSize(320, 50))

            bannerView.listener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: BannerView?) {
                    Timber.d("Unity banner loaded")
                    onAdLoaded()
                }

                override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {
                    Timber.w("Unity banner failed: ${errorInfo?.errorMessage}")
                    onAdFailed(errorInfo?.errorMessage ?: "Load failed")
                }

                override fun onBannerClick(bannerAdView: BannerView?) {
                    Timber.d("Unity banner clicked")
                }

                override fun onBannerShown(bannerAdView: BannerView?) {
                    Timber.d("Unity banner shown")
                }

                override fun onBannerLeftApplication(bannerAdView: BannerView?) {}
            }

            container.addView(bannerView)
            bannerView.load()
            true
        } catch (e: Exception) {
            Timber.e(e, "Unity banner error")
            onAdFailed(e.message ?: "Unknown error")
            false
        }
    }

    override suspend fun showInterstitialAd(
        activity: Activity,
        onAdShown: () -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        if (!UnityAds.isInitialized()) {
            onAdFailed("Unity interstitial not ready")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val showListener = object : IUnityAdsShowListener {
            override fun onUnityAdsShowStart(placementId: String?) {
                Timber.d("Unity interstitial started")
                onAdShown()
            }

            override fun onUnityAdsShowClick(placementId: String?) {
                Timber.d("Unity interstitial clicked")
            }

            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                Timber.d("Unity interstitial completed")
                onAdClosed()
                continuation.resume(true)
            }

            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                Timber.w("Unity interstitial show failed: $message")
                onAdFailed(message ?: "Show failed")
                continuation.resume(false)
            }
        }

        try {
            UnityAds.show(activity, INTERSTITIAL_PLACEMENT_ID, showListener)
        } catch (e: Exception) {
            Timber.e(e, "Unity interstitial show error")
            onAdFailed(e.message ?: "Unknown error")
            continuation.resume(false)
        }
    }

    override suspend fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (Int) -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        if (!UnityAds.isInitialized()) {
            onAdFailed("Unity rewarded not ready")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val showListener = object : IUnityAdsShowListener {
            override fun onUnityAdsShowStart(placementId: String?) {
                Timber.d("Unity rewarded started")
            }

            override fun onUnityAdsShowClick(placementId: String?) {
                Timber.d("Unity rewarded clicked")
            }

            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                Timber.d("Unity rewarded completed with state: $state")
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onRewardEarned(1) // Unity thường cho 1 reward
                }
                onAdClosed()
                continuation.resume(true)
            }

            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                Timber.w("Unity rewarded show failed: $message")
                onAdFailed(message ?: "Show failed")
                continuation.resume(false)
            }
        }

        try {
            UnityAds.show(activity, REWARDED_PLACEMENT_ID, showListener)
        } catch (e: Exception) {
            Timber.e(e, "Unity rewarded show error")
            onAdFailed(e.message ?: "Unknown error")
            continuation.resume(false)
        }
    }

    override fun cleanup() {
        isInitialized = false
    }
}
