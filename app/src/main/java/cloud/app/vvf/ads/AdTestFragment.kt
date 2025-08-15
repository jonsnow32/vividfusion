package cloud.app.vvf.ads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.R
import cloud.app.vvf.ads.providers.AdProvider
import cloud.app.vvf.databinding.FragmentAdTestBinding
import cloud.app.vvf.utils.autoCleared
import com.google.android.gms.ads.AdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AdTestFragment : Fragment() {

    private var binding by autoCleared<FragmentAdTestBinding>()

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var adPlacementHelper: AdPlacementHelper

    private var bannerAd: AdView? = null
    private var actionCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBannerAd()
        setupClickListeners()
        startStatusUpdates()
        updateActionCount()
    }

    private fun setupBannerAd() {
        lifecycleScope.launch {
            try {
                val success = adManager.createBannerAd(requireContext(), binding.bannerAdContainer)
                binding.tvBannerStatus.text = if (success) {
                    "Banner: Waterfall Success ✅"
                } else {
                    "Banner: All Providers Failed ❌"
                }
                Timber.d("Banner waterfall result: $success")
            } catch (e: Exception) {
                binding.tvBannerStatus.text = "Banner: Error - ${e.message}"
                Timber.e(e, "Failed to create waterfall banner ad")
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnShowInterstitial.setOnClickListener {
            incrementActionAndShowInterstitial()
        }

        binding.btnShowRewarded.setOnClickListener {
            showRewardedAd()
        }

        binding.btnTestDownloadComplete.setOnClickListener {
            testDownloadCompleteAd()
        }

        binding.btnTestPlayerExit.setOnClickListener {
            testPlayerExitAd()
        }

        binding.btnIncrementAction.setOnClickListener {
            incrementAction()
        }

        binding.btnResetCounter.setOnClickListener {
            resetActionCounter()
        }
    }

    private fun incrementActionAndShowInterstitial() {
        incrementAction()

        adManager.showInterstitialAd(requireActivity()) {
            showToast("Interstitial ad completed")
        }
    }

    private fun showRewardedAd() {
        if (adManager.isRewardedAdReady()) {
            adManager.showRewardedAd(
                requireActivity(),
                onUserEarnedReward = { rewardAmount ->
                    showToast("Reward earned: $rewardAmount coins!")
                    Timber.d("User earned reward: $rewardAmount")
                },
                onAdClosed = {
                    showToast("Rewarded ad closed")
                }
            )
        } else {
            showToast("Rewarded ad not ready yet")
        }
    }

    private fun testDownloadCompleteAd() {
        incrementAction()

        adPlacementHelper.showInterstitialForPlacement(
            requireActivity(),
            AdPlacementHelper.AdPlacement.AFTER_DOWNLOAD_COMPLETE
        ) {
            showToast("Download complete ad finished")
        }
    }

    private fun testPlayerExitAd() {
        incrementAction()

        adPlacementHelper.showInterstitialForPlacement(
            requireActivity(),
            AdPlacementHelper.AdPlacement.BACK_FROM_PLAYER
        ) {
            showToast("Player exit ad finished")
        }
    }

    private fun incrementAction() {
        actionCount++
        updateActionCount()
    }

    private fun resetActionCounter() {
        actionCount = 0
        updateActionCount()
        showToast("Action counter reset")
    }

    private fun updateActionCount() {
        binding.tvActionCount.text = "Action Count: $actionCount"

        // Update button text to show when next ad will show
        val actionsUntilNextAd = 3 - (actionCount % 3)
        binding.btnIncrementAction.text = if (actionsUntilNextAd == 3) "+ Action (Next ad: now)" else "+ Action ($actionsUntilNextAd until ad)"
    }

    private fun startStatusUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (isAdded) {
                updateAdStatus()
                delay(2000) // Update every 2 seconds
            }
        }
    }

    private fun updateAdStatus() {
        binding.tvInterstitialStatus.text = if (adManager.isInterstitialAdReady()) {
            val provider = adManager.getReadyProvider(AdProvider.AdType.INTERSTITIAL)
            "Interstitial: Ready ✅ (${provider?.providerType ?: "Unknown"})"
        } else {
            "Interstitial: Loading... ⏳"
        }

        binding.tvRewardedStatus.text = if (adManager.isRewardedAdReady()) {
            val provider = adManager.getReadyProvider(AdProvider.AdType.REWARDED)
            "Rewarded: Ready ✅ (${provider?.providerType ?: "Unknown"})"
        } else {
            "Rewarded: Loading... ⏳"
        }

        binding.tvBannerStatus.text = if (bannerAd != null) {
            "Banner: Loaded ✅"
        } else {
            "Banner: Failed ❌"
        }

        // Display provider statistics
        displayProviderStats()
    }

    private fun displayProviderStats() {
        val stats = adManager.getProviderStats()
        val statsText = StringBuilder("\n📊 Provider Performance:\n")

        stats.entries.sortedBy { it.key.ordinal }.forEach { (providerType, stat) ->
            val successRate = (stat.successRate * 100).toInt()
            val fillRate = (stat.fillRate * 100).toInt()
            statsText.append("${providerType}: ${successRate}% success, ${fillRate}% fill\n")
        }

        binding.tvActionCount.text = "${binding.tvActionCount.text}${statsText}"
    }

    private fun showToast(message: String) {
        // Using a simple approach - you can replace with your app's toast utility
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
        Timber.d("AdTest: $message")
    }

    override fun onDestroyView() {
        bannerAd = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = AdTestFragment()
    }
}
