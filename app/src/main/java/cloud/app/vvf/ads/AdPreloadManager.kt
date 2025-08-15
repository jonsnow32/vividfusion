package cloud.app.vvf.ads

import android.content.Context
import android.content.SharedPreferences
import cloud.app.vvf.ads.providers.AdProvider
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdPreloadManager @Inject constructor(
    private val adWaterfallManager: AdWaterfallManager
) {

    private var context: Context? = null
    private var preferencesManager: SharedPreferences? = null
    private var preloadScope: CoroutineScope? = null

    // Preload configuration
    private var preloadEnabled = true
    private var preloadOnWifi = true
    private var preloadOnMobile = false
    private var maxDailyPreloads = 50
    private var currentDailyPreloads = 0

    // Timing configuration
    private val immediatePreloadDelay = 1000L // 1 second
    private val normalPreloadInterval = 60_000L // 1 minute
    private val backgroundPreloadInterval = 300_000L // 5 minutes

    companion object {
        private const val PREFS_NAME = "ad_preload_prefs"
        private const val KEY_PRELOAD_ENABLED = "preload_enabled"
        private const val KEY_PRELOAD_ON_WIFI = "preload_on_wifi"
        private const val KEY_PRELOAD_ON_MOBILE = "preload_on_mobile"
        private const val KEY_MAX_DAILY_PRELOADS = "max_daily_preloads"
        private const val KEY_DAILY_PRELOAD_COUNT = "daily_preload_count"
        private const val KEY_LAST_PRELOAD_DATE = "last_preload_date"
    }

    fun initialize(context: Context) {
        this.context = context
        this.preferencesManager = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadPreferences()
        resetDailyCountIfNeeded()
    }

    private fun loadPreferences() {
        preferencesManager?.let { prefs ->
            preloadEnabled = prefs.getBoolean(KEY_PRELOAD_ENABLED, true)
            preloadOnWifi = prefs.getBoolean(KEY_PRELOAD_ON_WIFI, true)
            preloadOnMobile = prefs.getBoolean(KEY_PRELOAD_ON_MOBILE, false)
            maxDailyPreloads = prefs.getInt(KEY_MAX_DAILY_PRELOADS, 50)
            currentDailyPreloads = prefs.getInt(KEY_DAILY_PRELOAD_COUNT, 0)
        }
    }

    private fun savePreferences() {
        preferencesManager?.edit()?.apply {
            putBoolean(KEY_PRELOAD_ENABLED, preloadEnabled)
            putBoolean(KEY_PRELOAD_ON_WIFI, preloadOnWifi)
            putBoolean(KEY_PRELOAD_ON_MOBILE, preloadOnMobile)
            putInt(KEY_MAX_DAILY_PRELOADS, maxDailyPreloads)
            putInt(KEY_DAILY_PRELOAD_COUNT, currentDailyPreloads)
            putLong(KEY_LAST_PRELOAD_DATE, System.currentTimeMillis())
            apply()
        }
    }

    private fun resetDailyCountIfNeeded() {
        preferencesManager?.let { prefs ->
            val lastPreloadDate = prefs.getLong(KEY_LAST_PRELOAD_DATE, 0L)
            val currentDate = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L

            if (currentDate - lastPreloadDate > oneDayMs) {
                currentDailyPreloads = 0
                savePreferences()
                Timber.d("Reset daily preload count")
            }
        }
    }

    /**
     * Start intelligent preload based on app state and network conditions
     */
    fun startIntelligentPreload(isAppInForeground: Boolean = true) {
        if (!preloadEnabled || !canPreloadBasedOnNetwork()) {
            Timber.d("Preload disabled or network conditions not met")
            return
        }

        if (currentDailyPreloads >= maxDailyPreloads) {
            Timber.d("Daily preload limit reached: $currentDailyPreloads/$maxDailyPreloads")
            return
        }

        context?.let { ctx ->
            preloadScope?.cancel()
            preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            preloadScope?.launch {
                // Start periodic preload in waterfall manager
                adWaterfallManager.startPeriodicPreload(ctx)

                // Immediate preload for critical ad types
                delay(immediatePreloadDelay)
                performImmediatePreload(ctx)

                // Schedule ongoing preloads based on app state
                val interval = if (isAppInForeground) normalPreloadInterval else backgroundPreloadInterval

                while (isActive) {
                    try {
                        delay(interval)

                        if (currentDailyPreloads < maxDailyPreloads && canPreloadBasedOnNetwork()) {
                            performScheduledPreload(ctx)
                        } else {
                            Timber.d("Skipping preload - daily limit or network constraints")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error in intelligent preload loop")
                        delay(30_000L) // Wait 30 seconds before retry
                    }
                }
            }
        }
    }

    /**
     * Perform immediate preload for high-priority ad types
     */
    private suspend fun performImmediatePreload(context: Context) {
        if (!canPreload()) return

        try {
            Timber.d("Performing immediate preload")

            // Prioritize interstitial ads as they're most commonly used
            adWaterfallManager.forcePreloadAdType(context, AdProvider.AdType.INTERSTITIAL)
            incrementDailyCount()

            delay(2000L) // Small delay between preloads

            // Then preload rewarded ads
            if (canPreload()) {
                adWaterfallManager.forcePreloadAdType(context, AdProvider.AdType.REWARDED)
                incrementDailyCount()
            }

        } catch (e: Exception) {
            Timber.e(e, "Error in immediate preload")
        }
    }

    /**
     * Perform scheduled preload for all ad types
     */
    private suspend fun performScheduledPreload(context: Context) {
        if (!canPreload()) return

        try {
            Timber.d("Performing scheduled preload")

            // Preload all ads types in background
            adWaterfallManager.preloadAllAds(context)
            incrementDailyCount()

        } catch (e: Exception) {
            Timber.e(e, "Error in scheduled preload")
        }
    }

    /**
     * Preload ads on demand (e.g., before showing an ad)
     */
    suspend fun preloadOnDemand(context: Context, adType: AdProvider.AdType) {
        if (!canPreload()) {
            Timber.d("Cannot preload on demand - conditions not met")
            return
        }

        try {
            Timber.d("Preloading on demand: $adType")
            adWaterfallManager.forcePreloadAdType(context, adType)
            incrementDailyCount()
        } catch (e: Exception) {
            Timber.e(e, "Error in on-demand preload")
        }
    }

    /**
     * Check if preload is allowed based on current conditions
     */
    private fun canPreload(): Boolean {
        return preloadEnabled &&
               canPreloadBasedOnNetwork() &&
               currentDailyPreloads < maxDailyPreloads
    }

    /**
     * Check network conditions for preload
     */
    private fun canPreloadBasedOnNetwork(): Boolean {
        context?.let { ctx ->
            val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager ?: return false

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

                return when {
                    networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> preloadOnWifi
                    networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> preloadOnMobile
                    else -> false
                }
            } else {
                @Suppress("DEPRECATION")
                val activeNetwork = connectivityManager.activeNetworkInfo ?: return false

                @Suppress("DEPRECATION")
                return when (activeNetwork.type) {
                    android.net.ConnectivityManager.TYPE_WIFI -> preloadOnWifi
                    android.net.ConnectivityManager.TYPE_MOBILE -> preloadOnMobile
                    else -> false
                }
            }
        }
        return false
    }

    private fun incrementDailyCount() {
        currentDailyPreloads++
        savePreferences()
    }

    /**
     * Update preload settings
     */
    fun updateSettings(
        enabled: Boolean = preloadEnabled,
        onWifi: Boolean = preloadOnWifi,
        onMobile: Boolean = preloadOnMobile,
        maxDaily: Int = maxDailyPreloads
    ) {
        preloadEnabled = enabled
        preloadOnWifi = onWifi
        preloadOnMobile = onMobile
        maxDailyPreloads = maxDaily
        savePreferences()

        Timber.i("Preload settings updated - Enabled: $enabled, WiFi: $onWifi, Mobile: $onMobile, MaxDaily: $maxDaily")
    }

    /**
     * Get current preload statistics
     */
    fun getPreloadStats(): Map<String, Any> {
        val waterfallStats = adWaterfallManager.getPreloadStats()
        val managerStats = mutableMapOf<String, Any>()

        managerStats["preload_enabled"] = preloadEnabled
        managerStats["preload_on_wifi"] = preloadOnWifi
        managerStats["preload_on_mobile"] = preloadOnMobile
        managerStats["max_daily_preloads"] = maxDailyPreloads
        managerStats["current_daily_preloads"] = currentDailyPreloads
        managerStats["daily_preload_remaining"] = maxDailyPreloads - currentDailyPreloads
        managerStats["can_preload_now"] = canPreload()
        managerStats["network_allows_preload"] = canPreloadBasedOnNetwork()

        return managerStats + waterfallStats
    }

    /**
     * Stop all preload activities
     */
    fun stopPreload() {
        preloadScope?.cancel()
        preloadScope = null
        adWaterfallManager.stopPeriodicPreload()
        Timber.i("Stopped intelligent preload")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopPreload()
        context = null
        preferencesManager = null
    }
}
