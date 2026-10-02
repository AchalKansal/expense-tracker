package com.offline.expense;

import android.graphics.Color;

/**
 * User-selectable accent color, applied app-wide through ThemeHelper. Only the accent tokens
 * vary per theme — neutral backgrounds/text/borders (see ThemeHelper) stay the same regardless
 * of which one is picked, same as they already do between light/dark mode.
 */
enum AccentTheme {
    BLUE(
            Color.rgb(46, 118, 166), Color.rgb(127, 195, 232),
            Color.rgb(61, 141, 191), Color.rgb(158, 212, 239),
            Color.rgb(163, 209, 238), Color.rgb(191, 224, 245),
            Color.rgb(30, 74, 102)
    ),
    GREEN(
            Color.rgb(46, 140, 90), Color.rgb(120, 210, 160),
            Color.rgb(56, 163, 105), Color.rgb(150, 224, 180),
            Color.rgb(163, 224, 190), Color.rgb(196, 236, 214),
            Color.rgb(20, 90, 55)
    ),
    PURPLE(
            Color.rgb(110, 80, 170), Color.rgb(190, 170, 235),
            Color.rgb(130, 100, 190), Color.rgb(205, 185, 240),
            Color.rgb(205, 190, 235), Color.rgb(224, 212, 245),
            Color.rgb(60, 40, 110)
    ),
    ORANGE(
            Color.rgb(180, 100, 30), Color.rgb(235, 175, 120),
            Color.rgb(200, 120, 40), Color.rgb(240, 190, 140),
            Color.rgb(235, 195, 150), Color.rgb(245, 215, 180),
            Color.rgb(110, 60, 15)
    );

    private final int primaryLight;
    private final int primaryDark;
    private final int accentLight;
    private final int accentDark;
    private final int fillStart;
    private final int fillEnd;
    private final int onAccentFill;

    AccentTheme(int primaryLight, int primaryDark, int accentLight, int accentDark,
                int fillStart, int fillEnd, int onAccentFill) {
        this.primaryLight = primaryLight;
        this.primaryDark = primaryDark;
        this.accentLight = accentLight;
        this.accentDark = accentDark;
        this.fillStart = fillStart;
        this.fillEnd = fillEnd;
        this.onAccentFill = onAccentFill;
    }

    int primary(boolean darkMode) {
        return darkMode ? primaryDark : primaryLight;
    }

    int accent(boolean darkMode) {
        return darkMode ? accentDark : accentLight;
    }

    int fillStart() {
        return fillStart;
    }

    int fillEnd() {
        return fillEnd;
    }

    int onAccentFill() {
        return onAccentFill;
    }

    /** Representative solid color shown for this option in the theme picker itself. */
    int swatchColor() {
        return accentLight;
    }

    static AccentTheme fromPrefsValue(String value) {
        if (value == null) return BLUE;
        try {
            return AccentTheme.valueOf(value);
        } catch (IllegalArgumentException e) {
            return BLUE;
        }
    }
}
