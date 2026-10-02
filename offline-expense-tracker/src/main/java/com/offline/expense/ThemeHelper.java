package com.offline.expense;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

final class ThemeHelper {
    private final boolean darkMode;
    private final AccentTheme accentTheme;
    private final float density;

    ThemeHelper(boolean darkMode, AccentTheme accentTheme, float density) {
        this.darkMode = darkMode;
        this.accentTheme = accentTheme;
        this.density = density;
    }

    int colorBackground() {
        return darkMode ? Color.rgb(23, 22, 19) : Color.rgb(244, 248, 251);
    }

    int colorSurface() {
        return darkMode ? Color.rgb(33, 31, 27) : Color.WHITE;
    }

    int colorInput() {
        return darkMode ? Color.rgb(42, 40, 34) : Color.WHITE;
    }

    int colorInk() {
        return darkMode ? Color.rgb(243, 241, 234) : Color.rgb(30, 27, 22);
    }

    int colorMuted() {
        return darkMode ? Color.rgb(162, 156, 142) : Color.rgb(111, 106, 94);
    }

    int colorBorder() {
        return darkMode ? Color.rgb(53, 50, 43) : Color.rgb(225, 237, 245);
    }

    // Readable mid-tone accent for plain text/icons/links sitting directly on the
    // background or a card (not on a filled button) - needs real contrast, so it's a
    // deeper shade of the pastel fill below, lightened for dark mode. Both shift with the
    // user's chosen accent theme.
    int colorPrimary() {
        return accentTheme.primary(darkMode);
    }

    int colorAccent() {
        return accentTheme.accent(darkMode);
    }

    // The pastel fill used for buttons, active tabs/toggles, and selected chips - stays
    // this light in both themes, since it's meant to pop as a light chip either way.
    int colorAccentFillStart() {
        return accentTheme.fillStart();
    }

    int colorAccentFillEnd() {
        return accentTheme.fillEnd();
    }

    // Text/icon color for content drawn on top of the pastel fill above - dark navy,
    // not white, since the fill itself is too light for white text to read on.
    int colorOnAccentFill() {
        return accentTheme.onAccentFill();
    }

    GradientDrawable makeCardDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(colorSurface());
        d.setCornerRadius(dp(12));
        d.setStroke(dp(1), colorBorder());
        return d;
    }

    GradientDrawable makeInputDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(colorInput());
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1), colorBorder());
        return d;
    }

    GradientDrawable makeToggleDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(colorSurface());
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1), colorBorder());
        return d;
    }

    GradientDrawable makeActiveToggleDrawable() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorAccentFillEnd(), colorAccentFillStart()}
        );
        d.setCornerRadius(dp(8));
        return d;
    }

    GradientDrawable makePremiumButtonDrawable() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorAccentFillStart(), colorAccentFillEnd()}
        );
        d.setCornerRadius(dp(8));
        return d;
    }

    int colorDanger() {
        return Color.rgb(239, 68, 68);
    }

    GradientDrawable makeBadgeDrawable(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(999));
        return d;
    }

    GradientDrawable makeDangerButtonDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(darkMode ? Color.argb(50, 239, 68, 68) : Color.argb(20, 239, 68, 68));
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1), colorDanger());
        return d;
    }

    GradientDrawable makeSwatchDrawable(int fillColor, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fillColor);
        if (selected) d.setStroke(dp(2), colorInk());
        return d;
    }

    int dp(int value) {
        return Math.round(value * density);
    }
}
