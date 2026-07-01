package com.offline.expense;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

final class ThemeHelper {
    private final boolean darkMode;
    private final float density;

    ThemeHelper(boolean darkMode, float density) {
        this.darkMode = darkMode;
        this.density = density;
    }

    int colorBackground() {
        return darkMode ? Color.rgb(2, 6, 23) : Color.rgb(248, 250, 252);
    }

    int colorSurface() {
        return darkMode ? Color.rgb(15, 23, 42) : Color.WHITE;
    }

    int colorInput() {
        return darkMode ? Color.rgb(30, 41, 59) : Color.WHITE;
    }

    int colorInk() {
        return darkMode ? Color.rgb(248, 250, 252) : Color.rgb(15, 23, 42);
    }

    int colorMuted() {
        return darkMode ? Color.rgb(148, 163, 184) : Color.rgb(100, 116, 139);
    }

    int colorBorder() {
        return darkMode ? Color.rgb(51, 65, 85) : Color.rgb(226, 232, 240);
    }

    int colorPrimary() {
        return darkMode ? Color.rgb(59, 130, 246) : Color.rgb(37, 99, 235);
    }

    int colorAccent() {
        return darkMode ? Color.rgb(56, 189, 248) : Color.rgb(14, 165, 233);
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
                new int[]{colorAccent(), colorPrimary()}
        );
        d.setCornerRadius(dp(8));
        return d;
    }

    GradientDrawable makePremiumButtonDrawable() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorPrimary(), colorAccent()}
        );
        d.setCornerRadius(dp(8));
        return d;
    }

    int colorDanger() {
        return Color.rgb(239, 68, 68);
    }

    GradientDrawable makeDangerButtonDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(darkMode ? Color.argb(50, 239, 68, 68) : Color.argb(20, 239, 68, 68));
        d.setCornerRadius(dp(8));
        d.setStroke(dp(1), colorDanger());
        return d;
    }

    int dp(int value) {
        return Math.round(value * density);
    }
}
