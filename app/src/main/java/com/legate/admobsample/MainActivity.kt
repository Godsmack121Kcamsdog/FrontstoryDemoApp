package com.legate.admobsample

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.legate.admobsample.ui.model.NativeAdView
import com.legate.admobsample.ui.theme.AdMobSampleTheme
import com.legate.sdk.SdkNativeAd
import com.legate.sdk.manager.AdAction
import com.legate.sdk.manager.AdManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdManager.initialize(this)

        setContent {
            AdMobSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(innerPadding)
                }
            }
        }

        lifecycleScope.launch {
            DataStorage.incrementAppOpenCount(this@MainActivity) //increment every app opening time

            //tracking AdManager actions
            AdManager.actionsFlow.collect {
                when (it) {
                    is AdAction.Action -> {
                        Toast.makeText(
                            this@MainActivity,
                            it.msg,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    is AdAction.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            it.msg,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    AdAction.Idle -> {}
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AdManager.clear(this)
    }
}

@Composable
fun MainScreen(padding: PaddingValues) {
    val interstitialAdLimit = 2
    val context = LocalContext.current
    val nativeAds by AdManager.nativeAdsFlow.collectAsState()

    // Uploading interstitial ads
    LaunchedEffect(key1 = "LOAD_INTERSTITIAL_AD") {
        AdManager.loadInterstitialAd(context)
    }

    // show InterstitialAd after second app loading
    LaunchedEffect(key1 = "SHOW_INTERSTITIAL_AD") {
        val openCount = DataStorage.getAppOpenCount(context).first()
        if (openCount >= interstitialAdLimit) {
            AdManager.showInterstitialAd(context as Activity)
        }
    }

    // Uploading native ads
    LaunchedEffect(key1 = "LOAD_NATIVE_ADS") {
        AdManager.loadNativeAds(context)
    }

    val items = mutableListOf<Any>("Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6")

    // Enter ads into 1 and 3 positions, if they exist
    if (nativeAds.size >= 2) {
        items[1] = nativeAds[0]
        items[3] = nativeAds[1]
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(items) { index, item ->
            if (item is SdkNativeAd) {
                NativeAdView(ad = item)
            } else {
                Text(
                    text = item.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

