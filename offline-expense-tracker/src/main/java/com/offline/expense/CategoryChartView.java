package com.offline.expense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoryChartView extends View {
    static final int MODE_DONUT = 0;
    static final int MODE_TREND = 1;

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mutedLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF donutRect = new RectF();
    private final Path trendPath = new Path();
    private final Path fillPath = new Path();
    private final NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
    private final SimpleDateFormat monthLabelFormat = new SimpleDateFormat("MMM", Locale.getDefault());
    private final List<CategoryTotal> data = new ArrayList<>();
    private final List<DayTotal> trendData = new ArrayList<>();
    private int accentColor = Color.rgb(37, 99, 235);
    private int labelColor = Color.rgb(32, 33, 36);
    private int mutedColor = Color.rgb(109, 113, 120);
    private int surfaceColor = Color.WHITE;
    private int mode = MODE_DONUT;
    private static final int DONUT_NAMED_SLICES = 5;
    // Validated categorical order (dataviz skill reference palette, slots 1-5) —
    // passes adjacent-pair CVD separation including the ring's wraparound pair.
    private final int[] sliceColors = {
            Color.rgb(0x2a, 0x78, 0xd6), // blue
            Color.rgb(0xeb, 0x68, 0x34), // orange
            Color.rgb(0x1b, 0xaf, 0x7a), // aqua
            Color.rgb(0xed, 0xa1, 0x00), // yellow
            Color.rgb(0xe8, 0x7b, 0xa4)  // magenta
    };

    public CategoryChartView(Context context) {
        super(context);
        init();
    }

    public CategoryChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        slicePaint.setStyle(Paint.Style.STROKE);
        slicePaint.setStrokeCap(Paint.Cap.ROUND);

        ringTrackPaint.setStyle(Paint.Style.STROKE);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));

        labelPaint.setTextSize(sp(12));
        labelPaint.setFakeBoldText(true);

        mutedLabelPaint.setTextSize(sp(11));

        centerTextPaint.setTextAlign(Paint.Align.CENTER);
        centerTextPaint.setFakeBoldText(true);

        emptyPaint.setTextSize(sp(14));
        emptyPaint.setTextAlign(Paint.Align.CENTER);

        dotPaint.setStyle(Paint.Style.FILL);

        setMinimumHeight(dp(210));
        applyColors();
    }

    void setThemeColors(int accentColor, int labelColor, int mutedColor, int surfaceColor) {
        this.accentColor = accentColor;
        this.labelColor = labelColor;
        this.mutedColor = mutedColor;
        this.surfaceColor = surfaceColor;
        applyColors();
        invalidate();
    }

    private void applyColors() {
        linePaint.setColor(accentColor);
        dotPaint.setColor(accentColor);
        labelPaint.setColor(labelColor);
        mutedLabelPaint.setColor(mutedColor);
        centerTextPaint.setColor(labelColor);
        emptyPaint.setColor(mutedColor);
        gridPaint.setColor(Color.argb(50, Color.red(mutedColor), Color.green(mutedColor), Color.blue(mutedColor)));
        ringTrackPaint.setColor(Color.argb(40, Color.red(mutedColor), Color.green(mutedColor), Color.blue(mutedColor)));

        int r = Color.red(accentColor), g = Color.green(accentColor), b = Color.blue(accentColor);
        fillPaint.setColor(Color.argb(50, r, g, b));
    }

    void setData(List<CategoryTotal> totals) {
        data.clear();
        data.addAll(totals);
        invalidate();
    }

    void setTrendData(List<DayTotal> totals) {
        trendData.clear();
        trendData.addAll(totals);
        invalidate();
    }

    void setMode(int mode) {
        this.mode = mode;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mode == MODE_TREND) {
            if (trendData.isEmpty()) {
                canvas.drawText("No data for this period", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            } else {
                drawTrendChart(canvas);
            }
        } else {
            if (data.isEmpty()) {
                canvas.drawText("No expense data for this period", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            } else {
                drawDonutChart(canvas);
            }
        }
    }

    // ── Donut chart ──────────────────────────────────────────────────────────

    private void drawDonutChart(Canvas canvas) {
        List<CategoryTotal> slices = buildDonutSlices();
        double totalAmount = 0.0;
        for (CategoryTotal ct : slices) totalAmount += ct.total;
        if (totalAmount <= 0.0) return;

        int n = slices.size();
        boolean hasOtherBucket = data.size() > DONUT_NAMED_SLICES;

        float size = Math.min(getWidth() * 0.44f, getHeight() - dp(24));
        float cx = dp(8) + size / 2f;
        float cy = getHeight() / 2f;
        float outerR = size / 2f;
        float innerR = outerR * 0.64f;
        float bandWidth = outerR - innerR;
        float ringR = (outerR + innerR) / 2f;

        donutRect.set(cx - ringR, cy - ringR, cx + ringR, cy + ringR);

        // Background track so the rounded-cap gaps read as an intentional ring, not gaps.
        ringTrackPaint.setStrokeWidth(bandWidth);
        canvas.drawCircle(cx, cy, ringR, ringTrackPaint);

        slicePaint.setStrokeWidth(bandWidth);
        float gapDeg = n > 1 ? 4f : 0f;
        float availableSweep = 360f - gapDeg * n;

        float startAngle = -90f;
        for (int i = 0; i < n; i++) {
            CategoryTotal ct = slices.get(i);
            float sweep = (float) (availableSweep * ct.total / totalAmount);
            boolean isOther = hasOtherBucket && i == n - 1;
            slicePaint.setColor(isOther ? mutedColor : sliceColors[i % sliceColors.length]);
            canvas.drawArc(donutRect, startAngle + gapDeg / 2f, sweep, false, slicePaint);
            startAngle += sweep + gapDeg;
        }

        // Center text: total
        centerTextPaint.setTextSize(sp(11));
        centerTextPaint.setColor(mutedColor);
        canvas.drawText("Total", cx, cy - dp(9), centerTextPaint);
        centerTextPaint.setTextSize(sp(12));
        centerTextPaint.setColor(labelColor);
        String totalStr = abbreviateMoney(totalAmount);
        canvas.drawText(totalStr, cx, cy + dp(7), centerTextPaint);

        // Legend
        float legendX = cx + outerR + dp(16);
        float legendWidth = getWidth() - legendX - dp(4);
        float rowH = Math.max(dp(28), (getHeight() - dp(12)) / (float) n);

        for (int i = 0; i < n; i++) {
            CategoryTotal ct = slices.get(i);
            boolean isOther = hasOtherBucket && i == n - 1;
            float rowTop = dp(8) + i * rowH;
            float baseline = rowTop + dp(14);

            // Color dot
            slicePaint.setStyle(Paint.Style.FILL);
            slicePaint.setColor(isOther ? mutedColor : sliceColors[i % sliceColors.length]);
            canvas.drawCircle(legendX + dp(5), baseline - dp(3), dp(5), slicePaint);
            slicePaint.setStyle(Paint.Style.STROKE);

            // Category name
            String label = isOther ? ct.category : CategoryIcons.getEmoji(ct.category) + " " + ct.category;
            String name = ellipsize(label, labelPaint, legendWidth - dp(14));
            canvas.drawText(name, legendX + dp(14), baseline, labelPaint);

            // Amount + share of total (muted, below name)
            int pct = (int) Math.round(100.0 * ct.total / totalAmount);
            mutedLabelPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(abbreviateMoney(ct.total) + " · " + pct + "%", legendX + dp(14), baseline + dp(14), mutedLabelPaint);
        }
    }

    /**
     * Top categories as individually-colored slices, with anything past the
     * validated palette's slot count folded into a single neutral "Other" slice —
     * this also fixes the total (and every percentage) previously being computed
     * from only the visible slices instead of every category.
     */
    private List<CategoryTotal> buildDonutSlices() {
        List<CategoryTotal> slices = new ArrayList<>();
        double otherTotal = 0.0;
        for (int i = 0; i < data.size(); i++) {
            if (i < DONUT_NAMED_SLICES) {
                slices.add(data.get(i));
            } else {
                otherTotal += data.get(i).total;
            }
        }
        if (otherTotal > 0.0) {
            slices.add(new CategoryTotal("Everything else", otherTotal));
        }
        return slices;
    }

    // ── Trend / Line chart ────────────────────────────────────────────────────

    private void drawTrendChart(Canvas canvas) {
        int n = trendData.size();
        float chartLeft = dp(58);
        float chartRight = getWidth() - dp(12);
        float chartTop = dp(14);
        float chartBottom = getHeight() - dp(26);
        float chartW = chartRight - chartLeft;
        float chartH = chartBottom - chartTop;

        double maxVal = 0.0;
        for (DayTotal d : trendData) maxVal = Math.max(maxVal, d.total);
        if (maxVal == 0.0) maxVal = 1.0;

        // Horizontal grid lines (4 levels)
        int gridLines = 4;
        mutedLabelPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= gridLines; i++) {
            float y = chartBottom - (i / (float) gridLines) * chartH;
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
            if (i > 0) {
                double val = maxVal * i / gridLines;
                canvas.drawText(abbreviateMoney(val), chartLeft - dp(4), y + dp(4), mutedLabelPaint);
            }
        }

        // Compute point coordinates
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = n == 1 ? chartLeft + chartW / 2f : chartLeft + (i / (float) (n - 1)) * chartW;
            ys[i] = chartBottom - (float) (trendData.get(i).total / maxVal) * chartH;
        }

        // Fill gradient under the line
        fillPath.reset();
        fillPath.moveTo(xs[0], chartBottom);
        fillPath.lineTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) fillPath.lineTo(xs[i], ys[i]);
        fillPath.lineTo(xs[n - 1], chartBottom);
        fillPath.close();

        // Apply vertical gradient to fill
        LinearGradient gradient = new LinearGradient(
                0, chartTop, 0, chartBottom,
                Color.argb(80, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                Color.argb(8, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                Shader.TileMode.CLAMP
        );
        fillPaint.setShader(gradient);
        canvas.drawPath(fillPath, fillPaint);
        fillPaint.setShader(null);

        // Line
        trendPath.reset();
        trendPath.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) trendPath.lineTo(xs[i], ys[i]);
        canvas.drawPath(trendPath, linePaint);

        // Dots at each point
        for (int i = 0; i < n; i++) {
            dotPaint.setColor(surfaceColor);
            canvas.drawCircle(xs[i], ys[i], dp(4), dotPaint);
            dotPaint.setColor(accentColor);
            canvas.drawCircle(xs[i], ys[i], dp(3), dotPaint);
        }

        // X-axis labels — show a subset to avoid crowding
        mutedLabelPaint.setTextAlign(Paint.Align.CENTER);
        int labelEvery = Math.max(1, (int) Math.ceil(n / 6.0));
        for (int i = 0; i < n; i++) {
            if (i % labelEvery == 0 || i == n - 1) {
                String label = n <= 12
                        ? monthLabelFormat.format(new Date(trendData.get(i).dayMillis))
                        : dayFormat.format(new Date(trendData.get(i).dayMillis));
                canvas.drawText(label, xs[i], chartBottom + dp(18), mutedLabelPaint);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String abbreviateMoney(double amount) {
        if (amount >= 100_000) return String.format(Locale.getDefault(), "₹%.0fL", amount / 100_000);
        if (amount >= 1_000) return String.format(Locale.getDefault(), "₹%.0fK", amount / 1_000);
        return String.format(Locale.getDefault(), "₹%.0f", amount);
    }

    private String ellipsize(String text, Paint paint, float width) {
        return TextUtils.ellipsize(text, new android.text.TextPaint(paint), width,
                TextUtils.TruncateAt.END).toString();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
