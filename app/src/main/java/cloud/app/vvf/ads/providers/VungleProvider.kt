package cloud.app.vvf.ads.providers

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.vungle.ads.AdConfig
import com.vungle.ads.BannerAdListener
import com.vungle.ads.BaseAd
import com.vungle.ads.InitializationListener
import com.vungle.ads.InterstitialAd
import com.vungle.ads.InterstitialAdListener
import com.vungle.ads.RewardedAd
import com.vungle.ads.RewardedAdListener
import com.vungle.ads.VungleAdSize
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleBannerView
import com.vungle.ads.VungleError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

class VungleProvider : AdProvider {
    override val providerType = AdProvider.ProviderType.VUNGLE
    override val priority = 2

    companion object {
        // Replace with your real Vungle placement IDs
        const val APP_ID = "68ab2d5a61"
        const val INTERSTITIAL_PLACEMENT_ID = "DE0431"
        const val REWARDED_PLACEMENT_ID = "REWA9788"
        // Banner support is limited; add if needed
        const val BANNER_PLACEMENT_ID = "BAN3467"
    }

    private var isInitialized = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    // Banner not implemented

    override suspend fun initialize(context: Context): Boolean = suspendCancellableCoroutine { continuation ->

      VungleAds.init(context, APP_ID, object : InitializationListener {
        override fun onSuccess() {
          isInitialized = true
          Timber.d("Vungle initialized")
          continuation.resume(true)
        }
        override fun onError(vungleError: VungleError) {
          isInitialized = false
          Timber.e("Vungle init error: ${vungleError.errorMessage}")
          continuation.resume(false)
        }
      })

    }

    override fun isAdReady(adType: AdProvider.AdType): Boolean {
        return when (adType) {
            AdProvider.AdType.BANNER -> isInitialized
            AdProvider.AdType.INTERSTITIAL -> interstitial?.canPlayAd() == true
            AdProvider.AdType.REWARDED -> rewarded?.canPlayAd() == true
        }
    }

    override suspend fun preloadAd(context: Context, adType: AdProvider.AdType): Boolean {
        if (!isInitialized) return false
        return when (adType) {
            AdProvider.AdType.INTERSTITIAL -> withContext(Dispatchers.Main) { preloadInterstitial(context) }
            AdProvider.AdType.REWARDED -> withContext(Dispatchers.Main) { preloadRewarded(context) }
            AdProvider.AdType.BANNER -> true // Not implemented
        }
    }

    // Common listener for InterstitialAd
    class CommonInterstitialAdListener(
        private val onLoaded: (() -> Unit)? = null,
        private val onFailedToLoad: ((VungleError) -> Unit)? = null,
        private val onShown: (() -> Unit)? = null,
        private val onClosed: (() -> Unit)? = null,
        private val onFailedToShow: ((VungleError) -> Unit)? = null
    ) : InterstitialAdListener {
        override fun onAdLoaded(baseAd: BaseAd) { onLoaded?.invoke() }
        override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) { onFailedToLoad?.invoke(adError) }
        override fun onAdStart(baseAd: BaseAd) { onShown?.invoke() }
        override fun onAdEnd(baseAd: BaseAd) { onClosed?.invoke() }
        override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) { onFailedToShow?.invoke(adError) }
        override fun onAdClicked(baseAd: BaseAd) {}
        override fun onAdImpression(baseAd: BaseAd) {}
        override fun onAdLeftApplication(baseAd: BaseAd) {}
    }

    private suspend fun preloadInterstitial(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        val ad = InterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)
        ad.adListener = CommonInterstitialAdListener(
            onLoaded = {
                interstitial = ad
                Timber.d("Vungle interstitial loaded")
                continuation.resume(true)
            },
            onFailedToLoad = { error ->
                interstitial = null
                Timber.w("Vungle interstitial failed: ${error.errorMessage}")
                continuation.resume(false)
            }
        )
        ad.load()
    }

    private suspend fun preloadRewarded(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        val ad = RewardedAd(context,REWARDED_PLACEMENT_ID).apply {
          adListener = object : RewardedAdListener {
            override fun onAdClicked(baseAd: BaseAd) {
            }
            override fun onAdEnd(baseAd: BaseAd) {
            }

            override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
              rewarded = null
              Timber.w("Vungle rewarded failed: ${adError.errorMessage}")
              continuation.resume(false)
            }
            override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
            }
            override fun onAdImpression(baseAd: BaseAd) {
            }
            override fun onAdLeftApplication(baseAd: BaseAd) {
            }
            override fun onAdLoaded(baseAd: BaseAd) {
              rewarded = baseAd as RewardedAd?
              Timber.d("Vungle rewarded loaded")
              continuation.resume(true)
            }
            override fun onAdStart(baseAd: BaseAd) {
            }

            override fun onAdRewarded(baseAd: BaseAd) {
            }
          }
          load()
        }
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
        val ad = VungleBannerView(context, BANNER_PLACEMENT_ID, VungleAdSize.BANNER).apply {
          adListener = object : BannerAdListener {

            override fun onAdClicked(baseAd: BaseAd) {
            }

            override fun onAdEnd(baseAd: BaseAd) {
            }

            override fun onAdFailedToLoad(
              baseAd: BaseAd,
              adError: VungleError
            ) {
              Timber.w("AdMob banner failed: ${adError.message}")
              onAdFailed(adError.message ?: "Load failed")
            }

            override fun onAdFailedToPlay(
              baseAd: BaseAd,
              adError: VungleError
            ) {
            }

            override fun onAdImpression(baseAd: BaseAd) {
            }

            override fun onAdLeftApplication(baseAd: BaseAd) {
            }

            override fun onAdLoaded(baseAd: BaseAd) {
              Timber.d("Vungle banner loaded")
              onAdLoaded()
            }

            override fun onAdStart(baseAd: BaseAd) {
            }
          }
          load()
        }.also {
          container.addView(it)
        }
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
        val ad = interstitial
        if (ad == null || !ad.canPlayAd()) {
            onAdFailed("Vungle interstitial not ready")
            return@withContext false
        }
        ad.adListener = CommonInterstitialAdListener(
            onShown = { onAdShown() },
            onClosed = {
                interstitial = null
                onAdClosed()
            },
            onFailedToShow = { error ->
                interstitial = null
                onAdFailed(error.errorMessage)
            }
        )
        ad.play(activity)
        true
    }

    override suspend fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (Int) -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        val ad = rewarded
        if (ad == null || !ad.canPlayAd()) {
            onAdFailed("Vungle rewarded not ready")
            return@withContext false
        }
        ad.adListener = object : RewardedAdListener {
            override fun onAdLoaded(baseAd: BaseAd) {}
            override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {}
            override fun onAdStart(baseAd: BaseAd) {
                Timber.d("Vungle rewarded shown")
            }
            override fun onAdEnd(baseAd: BaseAd) {
                Timber.d("Vungle rewarded closed")
                rewarded = null
                onAdClosed()
            }
            override fun onAdRewarded(baseAd: BaseAd) {
                Timber.d("Vungle reward earned")
                onRewardEarned(1)
            }
            override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
                Timber.w("Vungle rewarded show failed: ${adError.errorMessage}")
                rewarded = null
                onAdFailed(adError.errorMessage)
            }
            override fun onAdClicked(baseAd: BaseAd) {}
            override fun onAdImpression(baseAd: BaseAd) {}
            override fun onAdLeftApplication(baseAd: BaseAd) {}
        }
        ad.play(activity)
        true
    }

    override fun cleanup() {
        interstitial = null
        rewarded = null
        isInitialized = false
    }
}
