package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.DEFAULT_ADMOB_REWARDED_UNIT_ID
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdMobManager(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _adImpressionsCount = MutableStateFlow(0)
    val adImpressionsCount: StateFlow<Int> = _adImpressionsCount.asStateFlow()

    private val _lastAdEventStatus = MutableStateFlow<String?>(null)
    val lastAdEventStatus: StateFlow<String?> = _lastAdEventStatus.asStateFlow()

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var loadedRewardedUnitId: String = DEFAULT_ADMOB_REWARDED_UNIT_ID
    @Volatile
    private var initStarted = false
    @Volatile
    private var sdkInitialized = false
    @Volatile
    private var pendingRewardedUnitId: String? = null

    fun initialize(rewardedUnitId: String = DEFAULT_ADMOB_REWARDED_UNIT_ID) {
        val cleanUnit = rewardedUnitId.trim().ifBlank { DEFAULT_ADMOB_REWARDED_UNIT_ID }
        pendingRewardedUnitId = cleanUnit
        if (!initStarted) {
            initStarted = true
            runCatching {
                MobileAds.initialize(appContext) {
                    sdkInitialized = true
                    Log.d(TAG, "Google Mobile Ads SDK initialized")
                    val queuedUnit = pendingRewardedUnitId ?: cleanUnit
                    preloadRewardedAd(queuedUnit)
                }
            }.onFailure { error ->
                initStarted = false
                Log.w(TAG, "Google Mobile Ads init unavailable in this environment", error)
                _lastAdEventStatus.value = "AdMob SDK standby (${error.localizedMessage ?: "no Play Services"})"
            }
        } else if (sdkInitialized) {
            preloadRewardedAd(cleanUnit)
        }
    }

    fun preloadRewardedAd(unitId: String = DEFAULT_ADMOB_REWARDED_UNIT_ID) {
        val cleanUnit = unitId.trim().ifBlank { DEFAULT_ADMOB_REWARDED_UNIT_ID }
        if (!sdkInitialized) {
            pendingRewardedUnitId = cleanUnit
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { preloadRewardedAd(cleanUnit) }
            return
        }
        loadedRewardedUnitId = cleanUnit
        runCatching {
            val request = AdRequest.Builder().build()
            RewardedAd.load(
                appContext,
                cleanUnit,
                request,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        _isRewardedAdReady.value = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        _isRewardedAdReady.value = false
                        Log.w(TAG, "Rewarded ad failed to load (${error.code}): ${error.message}")
                    }
                }
            )
        }.onFailure { error ->
            Log.w(TAG, "Rewarded ad load unavailable", error)
            _isRewardedAdReady.value = false
        }
    }

    fun recordBannerImpression(valueMicros: Long, currencyCode: String) {
        _adImpressionsCount.value += 1
        _lastAdEventStatus.value = if (valueMicros > 0L) {
            "Google Banner Ad paid $valueMicros micros ($currencyCode) — split 70% Admin / 30% Creators"
        } else {
            "AdMob Test Ad served (0 micros) — set live AdMob Unit IDs in Admin to earn real USD/NGN"
        }
    }

    fun showRewardedAd(
        activity: Activity?,
        unitId: String,
        creatorId: String,
        creatorName: String,
        postId: String,
        onPaidMicros: (
            valueMicros: Long,
            currencyCode: String,
            adUnitId: String,
            creatorId: String,
            creatorName: String,
            postId: String
        ) -> Unit,
        onRewardEarned: () -> Unit,
        onUnavailable: (String) -> Unit
    ) {
        val cleanUnit = unitId.trim().ifBlank { DEFAULT_ADMOB_REWARDED_UNIT_ID }
        if (activity == null) {
            onUnavailable("Open on an Android screen to watch a Google Rewarded Ad.")
            return
        }
        val currentAd = rewardedAd
        if (currentAd == null || loadedRewardedUnitId != cleanUnit) {
            preloadRewardedAd(cleanUnit)
            onUnavailable("Loading Google AdMob Rewarded Ad… please tap again in a moment.")
            return
        }

        currentAd.onPaidEventListener = OnPaidEventListener { adValue ->
            _adImpressionsCount.value += 1
            if (adValue.valueMicros > 0L) {
                _lastAdEventStatus.value =
                    "Rewarded Ad paid ${adValue.valueMicros} micros (${adValue.currencyCode}) — 70% Admin / 30% $creatorName"
            } else {
                _lastAdEventStatus.value =
                    "AdMob Test Ad served (0 micros) — set live AdMob Unit IDs in Admin to earn real USD/NGN"
            }
            onPaidMicros(
                adValue.valueMicros,
                adValue.currencyCode.ifBlank { "USD" },
                cleanUnit,
                creatorId,
                creatorName,
                postId
            )
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedAdReady.value = false
                preloadRewardedAd(cleanUnit)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _isRewardedAdReady.value = false
                onUnavailable("AdMob couldn't show right now: ${adError.message}")
                preloadRewardedAd(cleanUnit)
            }
        }

        runCatching {
            currentAd.show(activity) {
                _adImpressionsCount.value = _adImpressionsCount.value.coerceAtLeast(1)
                onRewardEarned()
            }
        }.onFailure { error ->
            rewardedAd = null
            _isRewardedAdReady.value = false
            onUnavailable(error.localizedMessage ?: "Couldn't launch Google Rewarded Ad")
        }
    }

    private companion object {
        const val TAG = "BintafaceAdMob"
    }
}
