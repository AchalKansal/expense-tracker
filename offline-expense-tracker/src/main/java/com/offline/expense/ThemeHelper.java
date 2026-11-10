package com.offline.expense;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
        if (isLiquidGlass()) return darkMode ? Color.rgb(31, 34, 43) : Color.rgb(206, 210, 219);
        return darkMode ? Color.rgb(23, 22, 19) : Color.rgb(244, 248, 251);
    }

    int colorSurface() {
        return darkMode ? Color.rgb(33, 31, 27) : Color.WHITE;
    }

    int colorInput() {
        return darkMode ? Color.rgb(42, 40, 34) : Color.WHITE;
    }

    int colorInk() {
        if (isLiquidGlass()) return darkMode ? Color.WHITE : Color.rgb(24, 30, 40);
        return darkMode ? Color.rgb(243, 241, 234) : Color.rgb(30, 27, 22);
    }

    int colorMuted() {
        if (isLiquidGlass()) return darkMode ? Color.rgb(232, 240, 252) : Color.rgb(77, 88, 104);
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

    Drawable makeCardDrawable() {
        if (isLiquidGlass()) {
            return makeContentSurfaceDrawable();
        }
        GradientDrawable d = new GradientDrawable();
        d.setColor(colorSurface());
        d.setCornerRadius(dp(12));
        d.setStroke(dp(1), colorBorder());
        return d;
    }

    /** Calm content layer; glass is reserved for controls and navigation, never nested in cards. */
    Drawable makeContentSurfaceDrawable() {
        if (!isLiquidGlass()) return makeInputDrawable();
        return new GlassDrawable(
                darkMode ? Color.argb(78, 240, 244, 255) : Color.argb(112, 255, 255, 255),
                darkMode ? Color.argb(31, 155, 164, 182) : Color.argb(38, 191, 196, 210),
                darkMode ? Color.argb(172, 244, 247, 255) : Color.argb(214, 255, 255, 255),
                darkMode ? Color.argb(82, 255, 255, 255) : Color.argb(152, 255, 255, 255), dp(24));
    }

    /** A single glass container for segmented navigation controls. */
    Drawable makeGlassBarDrawable() {
        if (!isLiquidGlass()) return makeToggleDrawable();
        return new GlassDrawable(
                darkMode ? Color.argb(126, 70, 78, 100) : Color.argb(130, 255, 255, 255),
                darkMode ? Color.argb(72, 39, 44, 58) : Color.argb(76, 218, 224, 235),
                darkMode ? Color.argb(118, 215, 225, 240) : Color.argb(220, 255, 255, 255),
                darkMode ? Color.argb(38, 255, 255, 255) : Color.argb(120, 255, 255, 255), dp(18));
    }

    Drawable makeClearControlDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.TRANSPARENT);
        d.setCornerRadius(dp(14));
        return d;
    }

    Drawable makeInputDrawable() {
        GradientDrawable d = new GradientDrawable();
        if (isLiquidGlass()) {
            return new GlassDrawable(
                    darkMode ? Color.argb(102, 32, 52, 91) : Color.argb(145, 255, 255, 255),
                    darkMode ? Color.argb(57, 18, 33, 66) : Color.argb(84, 204, 229, 255),
                    darkMode ? Color.argb(122, 187, 220, 255) : Color.argb(220, 255, 255, 255),
                    darkMode ? Color.argb(38, 255, 255, 255) : Color.argb(115, 255, 255, 255), dp(16));
        }
        d.setColor(colorInput());
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1), colorBorder());
        return d;
    }

    Drawable makeToggleDrawable() {
        GradientDrawable d = new GradientDrawable();
        if (isLiquidGlass()) {
            return new GlassDrawable(
                    darkMode ? Color.argb(108, 49, 72, 112) : Color.argb(155, 255, 255, 255),
                    darkMode ? Color.argb(66, 28, 44, 79) : Color.argb(96, 201, 226, 255),
                    darkMode ? Color.argb(138, 193, 224, 255) : Color.argb(226, 255, 255, 255),
                    darkMode ? Color.argb(44, 255, 255, 255) : Color.argb(128, 255, 255, 255), dp(16));
        }
        d.setColor(colorSurface());
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1), colorBorder());
        return d;
    }

    Drawable makeActiveToggleDrawable() {
        if (isLiquidGlass()) {
            return new GlassDrawable(
                    darkMode ? Color.argb(174, 111, 157, 234) : Color.argb(182, 206, 231, 255),
                    darkMode ? Color.argb(126, 65, 88, 183) : Color.argb(126, 147, 181, 255),
                    darkMode ? Color.argb(210, 218, 239, 255) : Color.argb(235, 255, 255, 255),
                    darkMode ? Color.argb(58, 255, 255, 255) : Color.argb(132, 255, 255, 255), dp(16));
        }
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorAccentFillEnd(), colorAccentFillStart()}
        );
        d.setCornerRadius(dp(8));
        return d;
    }

    Drawable makePremiumButtonDrawable() {
        if (isLiquidGlass()) {
            return new GlassDrawable(
                    darkMode ? Color.argb(190, 144, 184, 255) : Color.argb(194, 205, 229, 255),
                    darkMode ? Color.argb(144, 83, 104, 209) : Color.argb(143, 143, 175, 255),
                    darkMode ? Color.argb(225, 230, 244, 255) : Color.argb(245, 255, 255, 255),
                    darkMode ? Color.argb(65, 255, 255, 255) : Color.argb(145, 255, 255, 255), dp(18));
        }
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorAccentFillStart(), colorAccentFillEnd()}
        );
        d.setCornerRadius(dp(8));
        return d;
    }

    Drawable makeBackgroundDrawable() {
        if (!isLiquidGlass()) {
            GradientDrawable d = new GradientDrawable();
            d.setColor(colorBackground());
            return d;
        }
        return new LiquidBackgroundDrawable(darkMode);
    }

    boolean isLiquidGlass() {
        return accentTheme.isLiquidGlass();
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
