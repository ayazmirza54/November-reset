package dev.ayaz.november30;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the UI in code. */
final class Ui {
    private Ui() {}

    static final int BG = 0xff0b0e13, CARD = 0xff171c25, CARD_2 = 0xff222a37, GOLD = 0xffffd43b,
            WHITE = 0xfff5f6fa, MUTED = 0xffa0a7b5, GREEN = 0xff50d6a2, RED = 0xffff6b6b, TRACK = 0xff2a3140;

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static GradientDrawable round(Context c, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    static GradientDrawable outlined(Context c, int fill, int stroke, float radiusDp) {
        GradientDrawable d = round(c, fill, radiusDp);
        d.setStroke(dp(c, 2), stroke);
        return d;
    }

    static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout horizontal(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = vertical(c);
        l.setBackground(round(c, CARD, 18));
        pad(l, c, 16, 14, 16, 14);
        return l;
    }

    static void pad(View v, Context c, int l, int t, int r, int b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    /** Adds a full-width child with a bottom margin. */
    static void add(LinearLayout parent, View child, int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(parent.getContext(), bottomDp);
        parent.addView(child, lp);
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    static TextView section(Context c, String title) {
        TextView t = text(c, title, 13, GOLD, true);
        t.setLetterSpacing(0.12f);
        pad(t, c, 2, 12, 0, 2);
        return t;
    }

    static TextView button(Context c, String label, boolean primary) {
        TextView b = text(c, label, 14, primary ? BG : WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(c, primary ? GOLD : CARD_2, 12));
        pad(b, c, 16, 12, 16, 12);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    static TextView chip(Context c, String label) {
        TextView t = text(c, label, 13, WHITE, true);
        t.setGravity(Gravity.CENTER);
        pad(t, c, 12, 8, 12, 8);
        t.setClickable(true);
        return t;
    }

    static void styleChip(TextView chip, boolean on) {
        Context c = chip.getContext();
        chip.setBackground(on ? round(c, GOLD, 20) : outlined(c, CARD, TRACK, 20));
        chip.setTextColor(on ? BG : WHITE);
    }

    static EditText input(Context c, String hint, boolean multiline) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextColor(WHITE);
        e.setHintTextColor(MUTED);
        e.setTextSize(15);
        e.setBackgroundTintList(ColorStateList.valueOf(TRACK));
        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setSingleLine(true);
        }
        return e;
    }

    /** A thin horizontal bar showing fraction 0..1. */
    static View bar(Context c, float fraction, int color) {
        LinearLayout track = horizontal(c);
        track.setBackground(round(c, TRACK, 4));
        View fill = new View(c);
        fill.setBackground(round(c, color, 4));
        float f = Math.max(0f, Math.min(1f, fraction));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(c, 8), f));
        View rest = new View(c);
        track.addView(rest, new LinearLayout.LayoutParams(0, dp(c, 8), 1f - f));
        track.setWeightSum(1f);
        return track;
    }

    static String time(int minutes) {
        int h = minutes / 60, m = minutes % 60;
        String ampm = h < 12 ? "AM" : "PM";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format(java.util.Locale.US, "%d:%02d %s", h12, m, ampm);
    }

    static String number(double v) {
        if (v == Math.rint(v)) return String.format(java.util.Locale.US, "%,d", (long) v);
        return String.format(java.util.Locale.US, "%,.1f", v);
    }
}
