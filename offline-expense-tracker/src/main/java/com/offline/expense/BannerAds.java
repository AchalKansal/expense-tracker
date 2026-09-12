package com.offline.expense;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

final class BannerAds {
    static final String BANNER_AD_UNIT_ID = "ca-app-pub-1377363250840020/6830180927";

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_BASE_DELAY_MS = 15_000;

    private BannerAds() {}

    static AdView attach(Activity activity, FrameLayout container) {
        AdView adView = new AdView(activity);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);
        adView.setAdSize(resolveAdaptiveBannerSize(activity, container));
        container.addView(adView);

        Handler retryHandler = new Handler(Looper.getMainLooper());
        adView.setAdListener(new AdListener() {
            private int retryCount = 0;

            @Override
            public void onAdLoaded() {
                retryCount = 0;
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                // A no-fill/network error is often transient; retry a few times with backoff
                // instead of leaving the slot blank for the rest of the screen's lifetime.
                if (retryCount >= MAX_RETRIES) return;
                retryCount++;
                long delay = (long) RETRY_BASE_DELAY_MS * retryCount;
                retryHandler.postDelayed(() -> {
                    if (adView.isAttachedToWindow()) {
                        adView.loadAd(new AdRequest.Builder().build());
                    }
                }, delay);
            }
        });

        MobileAds.initialize(activity, status -> adView.loadAd(new AdRequest.Builder().build()));
        return adView;
    }

    /** Adaptive banners fill significantly more often than the fixed 320x50 AdSize.BANNER. */
    private static AdSize resolveAdaptiveBannerSize(Activity activity, FrameLayout container) {
        DisplayMetrics outMetrics = activity.getResources().getDisplayMetrics();
        int widthPixels = container.getWidth() > 0 ? container.getWidth() : outMetrics.widthPixels;
        int adWidth = (int) (widthPixels / outMetrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }
}
