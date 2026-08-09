package com.offline.expense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

final class PaceBarView extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint projectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint actualPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final Path clipPath = new Path();

    private int trackColor = Color.rgb(226, 232, 240);
    private int tickColor = Color.rgb(15, 23, 42);
    private int barColor = Color.rgb(37, 99, 235);
    private float spentFraction = 0f;
    private float projectedFraction = 0f;
    private float tickFraction = -1f;

    public PaceBarView(Context context) {
        super(context);
        init();
    }

    public PaceBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.FILL);
        projectedPaint.setStyle(Paint.Style.FILL);
        actualPaint.setStyle(Paint.Style.FILL);
        tickPaint.setStyle(Paint.Style.FILL);
        applyColors();
    }

    void setThemeColors(int trackColor, int tickColor) {
        this.trackColor = trackColor;
        this.tickColor = tickColor;
        applyColors();
        invalidate();
    }

    /**
     * @param tickFraction pass a negative value to hide the reference tick (e.g. no history yet).
     */
    void setData(float spentFraction, float projectedFraction, float tickFraction, int barColor) {
        this.spentFraction = clamp(spentFraction);
        this.projectedFraction = clamp(projectedFraction);
        this.tickFraction = tickFraction < 0f ? -1f : clamp(tickFraction);
        this.barColor = barColor;
        applyColors();
        invalidate();
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void applyColors() {
        trackPaint.setColor(trackColor);
        tickPaint.setColor(tickColor);
        actualPaint.setColor(barColor);
        projectedPaint.setColor(Color.argb(140, Color.red(barColor), Color.green(barColor), Color.blue(barColor)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float radius = h / 2f;
        trackRect.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(trackRect, radius, radius, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);
        canvas.drawRect(trackRect, trackPaint);
        if (projectedFraction > 0f) {
            canvas.drawRect(0, 0, w * projectedFraction, h, projectedPaint);
        }
        if (spentFraction > 0f) {
            canvas.drawRect(0, 0, w * spentFraction, h, actualPaint);
        }
        canvas.restore();

        if (tickFraction >= 0f) {
            float x = w * tickFraction;
            canvas.drawRect(x - dp(1), 0, x + dp(1), h, tickPaint);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
