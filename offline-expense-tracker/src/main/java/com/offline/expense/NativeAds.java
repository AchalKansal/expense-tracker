package com.offline.expense;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.Locale;

/**
 * Two policy-compliant native ad layouts (row + card) built programmatically to match this
 * app's existing hand-drawn view style. Both always show the "Ad" attribution label required
 * by AdMob's native ads policy; the SDK separately auto-places its own AdChoices privacy icon
 * since neither layout registers a custom one.
 */
final class NativeAds {
    static final String NATIVE_AD_UNIT_ID = "ca-app-pub-1377363250840020/7206762475";

    interface Callback {
        void onLoaded(NativeAdView adView);
    }

    private NativeAds() {}

    static void loadRow(Activity activity, ThemeHelper theme, Callback callback) {
        load(activity, nativeAd -> callback.onLoaded(buildRowView(activity, theme, nativeAd)));
    }

    static void loadCard(Activity activity, ThemeHelper theme, Callback callback) {
        load(activity, nativeAd -> callback.onLoaded(buildCardView(activity, theme, nativeAd)));
    }

    /** Call from onDestroy for any NativeAdView returned above to release its resources. */
    static void destroy(View adView) {
        if (adView == null) return;
        Object tag = adView.getTag();
        if (tag instanceof NativeAd) {
            ((NativeAd) tag).destroy();
        }
    }

    private static void load(Activity activity, NativeAd.OnNativeAdLoadedListener onLoaded) {
        AdLoader adLoader = new AdLoader.Builder(activity, NATIVE_AD_UNIT_ID)
                .forNativeAd(onLoaded)
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        // No-fill/error: this placement is an optional enhancement, not
                        // guaranteed layout space, so we simply skip it rather than retry.
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private static TextView makeAdBadge(Activity activity, ThemeHelper theme) {
        TextView badge = new TextView(activity);
        badge.setText("Ad");
        badge.setTextSize(10);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setTextColor(theme.colorPrimary());
        badge.setBackground(theme.makeToggleDrawable());
        badge.setPadding(theme.dp(6), theme.dp(2), theme.dp(6), theme.dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = theme.dp(6);
        badge.setLayoutParams(params);
        return badge;
    }

    private static NativeAdView buildRowView(Activity activity, ThemeHelper theme, NativeAd nativeAd) {
        NativeAdView adView = new NativeAdView(activity);
        adView.setBackground(theme.makeCardDrawable());
        adView.setPadding(theme.dp(12), theme.dp(10), theme.dp(12), theme.dp(10));
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rootParams.bottomMargin = theme.dp(10);
        adView.setLayoutParams(rootParams);

        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView badge = makeAdBadge(activity, theme);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(activity);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(theme.dp(40), theme.dp(40));
        iconParams.setMarginEnd(theme.dp(10));
        icon.setLayoutParams(iconParams);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout textColumn = new LinearLayout(activity);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView headline = new TextView(activity);
        headline.setTextColor(theme.colorInk());
        headline.setTextSize(14.5f);
        headline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headline.setSingleLine(true);
        headline.setEllipsize(TextUtils.TruncateAt.END);

        TextView body = new TextView(activity);
        body.setTextColor(theme.colorMuted());
        body.setTextSize(12);
        body.setSingleLine(true);
        body.setEllipsize(TextUtils.TruncateAt.END);
        body.setPadding(0, theme.dp(2), 0, 0);

        textColumn.addView(headline);
        textColumn.addView(body);

        Button cta = new Button(activity);
        cta.setAllCaps(false);
        cta.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        cta.setTextColor(theme.colorOnAccentFill());
        cta.setBackground(theme.makePremiumButtonDrawable());
        cta.setMinWidth(0);
        cta.setMinHeight(0);
        cta.setTextSize(12.5f);
        cta.setPadding(theme.dp(14), theme.dp(8), theme.dp(14), theme.dp(8));
        LinearLayout.LayoutParams ctaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ctaParams.setMarginStart(theme.dp(8));
        cta.setLayoutParams(ctaParams);

        row.addView(icon);
        row.addView(textColumn);
        row.addView(cta);

        column.addView(badge);
        column.addView(row);
        adView.addView(column);

        bindOptional(icon, nativeAd.getIcon() != null ? nativeAd.getIcon().getDrawable() : null);
        headline.setText(nativeAd.getHeadline());
        bindOptionalText(body, nativeAd.getBody());
        bindOptionalText(cta, nativeAd.getCallToAction());

        adView.setIconView(icon);
        adView.setHeadlineView(headline);
        adView.setBodyView(body);
        adView.setCallToActionView(cta);
        adView.setNativeAd(nativeAd);
        adView.setTag(nativeAd);
        return adView;
    }

    private static NativeAdView buildCardView(Activity activity, ThemeHelper theme, NativeAd nativeAd) {
        NativeAdView adView = new NativeAdView(activity);
        adView.setBackground(theme.makeCardDrawable());
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rootParams.topMargin = theme.dp(14);
        adView.setLayoutParams(rootParams);
        adView.setPadding(theme.dp(14), theme.dp(12), theme.dp(14), theme.dp(14));

        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView badge = makeAdBadge(activity, theme);

        MediaView mediaView = new MediaView(activity);
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, theme.dp(140));
        mediaParams.bottomMargin = theme.dp(10);
        mediaView.setLayoutParams(mediaParams);

        LinearLayout headRow = new LinearLayout(activity);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(activity);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(theme.dp(30), theme.dp(30));
        iconParams.setMarginEnd(theme.dp(9));
        icon.setLayoutParams(iconParams);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout textColumn = new LinearLayout(activity);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView headline = new TextView(activity);
        headline.setTextColor(theme.colorInk());
        headline.setTextSize(14.5f);
        headline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView stars = new TextView(activity);
        stars.setTextColor(theme.colorMuted());
        stars.setTextSize(11.5f);
        stars.setPadding(0, theme.dp(2), 0, 0);

        textColumn.addView(headline);
        textColumn.addView(stars);
        headRow.addView(icon);
        headRow.addView(textColumn);

        TextView body = new TextView(activity);
        body.setTextColor(theme.colorMuted());
        body.setTextSize(12.5f);
        body.setLineSpacing(theme.dp(2), 1f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = theme.dp(8);
        body.setLayoutParams(bodyParams);

        Button cta = new Button(activity);
        cta.setAllCaps(false);
        cta.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        cta.setTextColor(theme.colorOnAccentFill());
        cta.setBackground(theme.makePremiumButtonDrawable());
        LinearLayout.LayoutParams ctaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, theme.dp(44));
        ctaParams.topMargin = theme.dp(12);
        cta.setLayoutParams(ctaParams);

        column.addView(badge);
        column.addView(mediaView);
        column.addView(headRow);
        column.addView(body);
        column.addView(cta);
        adView.addView(column);

        bindOptional(icon, nativeAd.getIcon() != null ? nativeAd.getIcon().getDrawable() : null);
        headline.setText(nativeAd.getHeadline());
        if (nativeAd.getStarRating() != null) {
            stars.setText(String.format(Locale.getDefault(), "%.1f ★ rating", nativeAd.getStarRating()));
            stars.setVisibility(View.VISIBLE);
        } else {
            stars.setVisibility(View.GONE);
        }
        bindOptionalText(body, nativeAd.getBody());
        bindOptionalText(cta, nativeAd.getCallToAction());

        adView.setMediaView(mediaView);
        adView.setIconView(icon);
        adView.setHeadlineView(headline);
        adView.setStarRatingView(stars);
        adView.setBodyView(body);
        adView.setCallToActionView(cta);
        adView.setNativeAd(nativeAd);
        adView.setTag(nativeAd);
        return adView;
    }

    private static void bindOptional(ImageView view, android.graphics.drawable.Drawable drawable) {
        if (drawable == null) {
            view.setVisibility(View.GONE);
        } else {
            view.setImageDrawable(drawable);
            view.setVisibility(View.VISIBLE);
        }
    }

    private static void bindOptionalText(TextView view, CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }
    }
}
