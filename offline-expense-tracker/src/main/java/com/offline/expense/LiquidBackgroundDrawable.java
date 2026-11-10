package com.offline.expense;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Ambient, translucent color beneath glass surfaces. It gives the glass something to refract. */
final class LiquidBackgroundDrawable extends Drawable {
    private final boolean darkMode;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    LiquidBackgroundDrawable(boolean darkMode) {
        this.darkMode = darkMode;
    }

    @Override
    public void draw(Canvas canvas) {
        bounds.set(getBounds());
        if (bounds.isEmpty()) return;
        // Neutral dimensional field: the material stays clear while selection supplies color.
        paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                darkMode ? new int[]{Color.rgb(79, 82, 92), Color.rgb(19, 22, 30)}
                        : new int[]{Color.rgb(226, 228, 234), Color.rgb(130, 136, 148)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds, paint);
        paint.setShader(null);

        float size = Math.max(bounds.width(), bounds.height());
        drawBloom(canvas, bounds.left + bounds.width() * .35f, bounds.top + bounds.height() * .20f,
                size * .54f, darkMode ? 0x32FFFFFF : 0x4AFFFFFF);
        drawBloom(canvas, bounds.right - bounds.width() * .06f, bounds.bottom - bounds.height() * .16f,
                size * .48f, darkMode ? 0x29000000 : 0x36000000);
    }

    private void drawBloom(Canvas canvas, float x, float y, float radius, int color) {
        paint.setShader(new RadialGradient(x, y, radius,
                new int[]{color, color & 0x00FFFFFF}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius, paint);
        paint.setShader(null);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
