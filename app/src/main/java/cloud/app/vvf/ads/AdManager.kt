package cloud.app.vvf.ads

import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor() : DefaultLifecycleObserver {

    companion object {
        // Test Ad Unit IDs - Replace with your actual IDs in production
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        private const val AD_FREQUENCY_LIMIT = 3 // Show interstitial every 3 actions
        private const val MIN_TIME_BETWEEN_ADS = 30_000L // 30 seconds
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false

    // Ad frequency control
    private var actionCount = 0
    private var lastAdShownTime = 0L

    fun initialize(context: Context) {
        MobileAds.initialize(context) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Timber.d("AdMob adapter: $adapterClass, status: ${status?.initializationState}, description: ${status?.description}")
            }
            Timber.i("AdMob initialized successfully")
        }

        // Preload ads
        loadInterstitialAd(context)
        loadRewardedAd(context)
    }

    /**
     * Create banner ad for use in layouts
     */
    fun createBannerAd(context: Context): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Timber.d("Banner ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Banner ad failed to load: ${error.message}")
                }

                override fun onAdClicked() {
                    Timber.d("Banner ad clicked")
                }
            }

            loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * Load interstitial ad
     */
    private fun loadInterstitialAd(context: Context) {
        if (isLoadingInterstitial || interstitialAd != null) return

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Timber.d("Interstitial ad loaded successfully")
                interstitialAd = ad
                isLoadingInterstitial = false

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Timber.d("Interstitial ad dismissed")
                        interstitialAd = null
                        loadInterstitialAd(context) // Preload next ad
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Timber.w("Interstitial ad failed to show: ${error.message}")
                        interstitialAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Timber.d("Interstitial ad showed")
                        lastAdShownTime = System.currentTimeMillis()
                    }
                }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Timber.w("Interstitial ad failed to load: ${error.message}")
                isLoadingInterstitial = false
                interstitialAd = null
            }
        })
    }

    /**
     * Show interstitial ad with frequency control
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

        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Timber.d("Interstitial ad dismissed")
                    interstitialAd = null
                    actionCount = 0 // Reset counter
                    loadInterstitialAd(activity) // Preload next ad
                    onAdClosed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.w("Interstitial ad failed to show: ${error.message}")
                    interstitialAd = null
                    onAdClosed?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.d("Interstitial ad showed")
                    lastAdShownTime = System.currentTimeMillis()
                }
            }
            ad.show(activity)
        } ?: run {
            Timber.d("Interstitial ad not ready, loading new one")
            loadInterstitialAd(activity)
            onAdClosed?.invoke()
        }
    }

    /**
     * Load rewarded ad
     */
    private fun loadRewardedAd(context: Context) {
        if (isLoadingRewarded || rewardedAd != null) return

        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                Timber.d("Rewarded ad loaded successfully")
                rewardedAd = ad
                isLoadingRewarded = false

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Timber.d("Rewarded ad dismissed")
                        rewardedAd = null
                        loadRewardedAd(context) // Preload next ad
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Timber.w("Rewarded ad failed to show: ${error.message}")
                        rewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Timber.d("Rewarded ad showed")
                    }
                }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Timber.w("Rewarded ad failed to load: ${error.message}")
                isLoadingRewarded = false
                rewardedAd = null
            }
        })
    }

    /**
     * Show rewarded ad
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (rewardAmount: Int) -> Unit,
        onAdClosed: (() -> Unit)? = null
    ) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Timber.d("Rewarded ad dismissed")
                    rewardedAd = null
                    loadRewardedAd(activity) // Preload next ad
                    onAdClosed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.w("Rewarded ad failed to show: ${error.message}")
                    rewardedAd = null
                    onAdClosed?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.d("Rewarded ad showed")
                }
            }

            ad.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                Timber.d("User earned reward: $rewardAmount")
                onUserEarnedReward(rewardAmount)
            }
        } ?: run {
            Timber.d("Rewarded ad not ready, loading new one")
            loadRewardedAd(activity)
            onAdClosed?.invoke()
        }
    }

    /**
     * Check if rewarded ad is available
     */
    fun isRewardedAdReady(): Boolean = rewardedAd != null

    /**
     * Check if interstitial ad is available
     */
    fun isInterstitialAdReady(): Boolean = interstitialAd != null

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        interstitialAd = null
        rewardedAd = null
    }
}
