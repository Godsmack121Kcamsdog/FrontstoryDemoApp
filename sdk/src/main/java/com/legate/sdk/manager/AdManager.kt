package com.legate.sdk.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.legate.sdk.SdkNativeAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdManager : AdProvider {

    companion object {
        private const val TAG = "AdManager"
        private const val NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110" // Test ID
        private const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712" // Test ID
    }

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val _interstitialAdFlow = MutableStateFlow<InterstitialAd?>(null)
    private val interstitialAdFlow = _interstitialAdFlow.asStateFlow()

    private val _actionsFlow = MutableStateFlow<AdAction>(AdAction.Idle)
    private val actionsFlow = _actionsFlow.asStateFlow()

    private val _nativeAdsFlow = MutableStateFlow<List<SdkNativeAd>>(emptyList())
    private val nativeAdsFlow = _nativeAdsFlow.asStateFlow()

    private var nativeAdsId: String = ""

    override fun initialize(context: Context) {
        MobileAds.initialize(context) {
            scope.launch {
                _actionsFlow.emit(AdAction.Action(null, "AdMob was initialized"))
            }
        }
    }

    override fun loadNativeAds(context: Context, adsId: String?) {
        nativeAdsId = adsId ?: NATIVE_AD_ID
        val adLoader = AdLoader.Builder(context, nativeAdsId)
            .forNativeAd { ad: NativeAd ->
                // Show the ad.
                scope.launch {
                    val list = _nativeAdsFlow.value.toMutableList()
                    list.add(SdkNativeAd(ad.headline, ad.body, ad.icon?.drawable))
                    _nativeAdsFlow.emit(list)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Handle the failure.
                    scope.launch {
                        _actionsFlow.emit(AdAction.Error("NativeAd failed to load: ${adError.message}"))
                    }
                    Log.e(TAG, "onAdFailedToLoad: ${adError.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    // Methods in the NativeAdOptions.Builder class can be
                    // used here to specify individual options settings.
                    .build()
            )
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), 3)
    }

    override fun loadInterstitialAd(context: Context, adsId: String?) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adsId ?: INTERSTITIAL_AD_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    scope.launch {
                        _interstitialAdFlow.emit(ad)
                        _actionsFlow.emit(
                            AdAction.Action(
                                data = ad.adUnitId,
                                msg = "Interstitial ad loaded: ${ad.adUnitId}"
                            )
                        )
                    }
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    scope.launch {
                        _actionsFlow.emit(
                            AdAction.Error(
                                msg = "Interstitial ad failed to load: ${error.message}"
                            )
                        )
                    }
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            })
    }

    override fun showInterstitialAd(activity: Activity) {
        scope.launch {
            interstitialAdFlow.collect { ad ->
                withContext(Dispatchers.Main) {
                    ad?.show(activity)
                }
                _interstitialAdFlow.emit(null) // Clean instance after showing
            }
        }
    }

    override fun getActionsFlow(): StateFlow<AdAction> {
        return actionsFlow
    }

    override fun getNativeAdsFlow(): StateFlow<List<SdkNativeAd>> {
        return nativeAdsFlow
    }

    override fun clear(activity: Activity) {
        AdLoader.Builder(activity, nativeAdsId)
            .forNativeAd { nativeAd ->
                // If this callback occurs after the activity is destroyed, you
                // must call destroy and return or you may get a memory leak.
                // Note `isDestroyed` is a method on Activity.
                if (activity.isDestroyed) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
            }.build()
        job.cancel()
        scope.cancel()
    }

}
