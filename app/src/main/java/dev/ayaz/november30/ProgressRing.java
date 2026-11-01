package dev.ayaz.november30;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Circular score indicator: "9 / 13". */
final class ProgressRing extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private int value, max = 1;

    ProgressRing(Context c) {
        super(c);
        float stroke = Ui.dp(c, 10);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(Ui.TRACK);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        big.setColor(Ui.WHITE);
        big.setTextAlign(Paint.Align.CENTER);
        big.setTypeface(Typeface.DEFAULT_BOLD);
        small.setColor(Ui.MUTED);
        small.setTextAlign(Paint.Align.CENTER);
    }

    void set(int value, int max) {
        this.value = value;
        this.max = Math.max(1, max);
        setContentDescription(value + " of " + max + " habits done");
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float half = track.getStrokeWidth() / 2f;
        float left = (getWidth() - size) / 2f + half, top = (getHeight() - size) / 2f + half;
        oval.set(left, top, left + size - 2 * half, top + size - 2 * half);
        canvas.drawArc(oval, 0, 360, false, track);
        arc.setColor(value >= Challenge.STRONG_DAY ? Ui.GREEN : Ui.GOLD);
        if (value > 0) canvas.drawArc(oval, -90, 360f * value / max, false, arc);
        big.setTextSize(size * 0.28f);
        small.setTextSize(size * 0.11f);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        canvas.drawText(String.valueOf(value), cx, cy + size * 0.06f, big);
        canvas.drawText("of " + max, cx, cy + size * 0.22f, small);
    }
}
