package cloud.app.vvf.ads.providers

import android.app.Activity
import android.content.Context
import android.view.ViewGroup

/**
 * Interface chung cho tất cả ad providers
 */
interface AdProvider {

    enum class AdType {
        BANNER, INTERSTITIAL, REWARDED
    }

    enum class ProviderType {
        ADMOB, FACEBOOK, UNITY, IRONSOURCE, APPLOVIN, VUNGLE
    }

    val providerType: ProviderType
    val priority: Int // Thứ tự ưu tiên (1 = cao nhất)

    /**
     * Khởi tạo ad provider
     */
    suspend fun initialize(context: Context): Boolean

    /**
     * Kiểm tra ad có sẵn hay không
     */
    fun isAdReady(adType: AdType): Boolean

    /**
     * Load ad trước
     */
    suspend fun preloadAd(context: Context, adType: AdType): Boolean

    /**
     * Hiển thị banner ad
     */
    suspend fun showBannerAd(
        context: Context,
        container: ViewGroup,
        onAdLoaded: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ): Boolean

    /**
     * Hiển thị interstitial ad
     */
    suspend fun showInterstitialAd(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdClosed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ): Boolean

    /**
     * Hiển thị rewarded ad
     */
    suspend fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (Int) -> Unit = {},
        onAdClosed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ): Boolean

    /**
     * Cleanup resources
     */
    fun cleanup()
}
