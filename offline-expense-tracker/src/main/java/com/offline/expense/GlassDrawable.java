package com.offline.expense;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** A frosted surface with an internal light reflection, used only by the Liquid Glass theme. */
final class GlassDrawable extends Drawable {
    private final int topColor;
    private final int bottomColor;
    private final int borderColor;
    private final int highlightColor;
    private final float cornerRadius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    GlassDrawable(int topColor, int bottomColor, int borderColor, int highlightColor, float cornerRadius) {
        this.topColor = topColor;
        this.bottomColor = bottomColor;
        this.borderColor = borderColor;
        this.highlightColor = highlightColor;
        this.cornerRadius = cornerRadius;
    }

    @Override
    public void draw(Canvas canvas) {
        bounds.set(getBounds());
        if (bounds.isEmpty()) return;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                topColor, bottomColor, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);

        // The short highlight fade makes the top edge read as a reflective glass surface,
        // rather than as a flat translucent card.
        float highlightEnd = bounds.top + Math.max(cornerRadius * 2.5f, bounds.height() * .36f);
        paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.left, highlightEnd,
                highlightColor, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);

        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(borderColor);
        RectF strokeBounds = new RectF(bounds);
        strokeBounds.inset(.75f, .75f);
        canvas.drawRoundRect(strokeBounds, cornerRadius, cornerRadius, paint);


    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
