package com.offline.expense;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
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

    // Google's AdMob Ad Refresh policy sets a hard minimum of 30s between automatic banner
    // refreshes; refreshing faster than this is a policy violation tied to invalid traffic.
    private static final int REFRESH_INTERVAL_MS = 30_000;

    private BannerAds() {}

    static AdView attach(Activity activity, FrameLayout container) {
        AdView adView = new AdView(activity);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);
        adView.setAdSize(resolveAdaptiveBannerSize(activity, container));
        container.addView(adView);

        Handler handler = new Handler(Looper.getMainLooper());
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
                handler.postDelayed(() -> {
                    if (adView.isAttachedToWindow()) {
                        adView.loadAd(new AdRequest.Builder().build());
                    }
                }, delay);
            }
        });

        Runnable refreshLoop = new Runnable() {
            @Override
            public void run() {
                if (adView.isAttachedToWindow()) {
                    adView.loadAd(new AdRequest.Builder().build());
                }
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };

        // Start the refresh loop once the view is actually on screen, and stop it the moment
        // this screen is torn down — so a screen you've navigated away from doesn't keep
        // silently spending ad requests in the background.
        adView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                handler.postDelayed(refreshLoop, REFRESH_INTERVAL_MS);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                handler.removeCallbacksAndMessages(null);
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
