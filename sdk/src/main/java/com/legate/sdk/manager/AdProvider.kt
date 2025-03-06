package com.legate.sdk.manager

import android.app.Activity
import android.content.Context
import com.legate.sdk.SdkNativeAd
import kotlinx.coroutines.flow.StateFlow

interface AdProvider {
    fun initialize(context: Context)
    fun loadNativeAds(context: Context, adsId: String?)
    fun loadInterstitialAd(context: Context, adsId: String?)
    fun showInterstitialAd(activity: Activity)
    fun getActionsFlow(): StateFlow<AdAction>
    fun getNativeAdsFlow(): StateFlow<List<SdkNativeAd>>
    fun clear(activity: Activity)
}