package com.offline.coders;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

final class FlowLayout extends ViewGroup {
    private final int hGap;
    private final int vGap;

    FlowLayout(Context context) {
        super(context);
        float density = context.getResources().getDisplayMetrics().density;
        hGap = Math.round(8 * density);
        vGap = Math.round(8 * density);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        hGap = Math.round(8 * density);
        vGap = Math.round(8 * density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int x = 0, y = 0, rowH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();
            if (x > 0 && x + cw > maxWidth) {
                x = 0;
                y += rowH + vGap;
                rowH = 0;
            }
            x += cw + hGap;
            rowH = Math.max(rowH, ch);
        }
        setMeasuredDimension(maxWidth, y + rowH);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxWidth = r - l;
        int x = 0, y = 0, rowH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();
            if (x > 0 && x + cw > maxWidth) {
                x = 0;
                y += rowH + vGap;
                rowH = 0;
            }
            child.layout(x, y, x + cw, y + ch);
            x += cw + hGap;
            rowH = Math.max(rowH, ch);
        }
    }
}
