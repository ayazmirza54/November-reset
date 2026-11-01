package dev.ayaz.november30;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Locale;

/** Terminal-flavoured Material 3 palette and component builders. */
final class Ui {
    private Ui() {}

    static final String GREEN = "green", AMBER = "amber", CYAN = "cyan", YOU = "you";

    // Fixed terminal neutrals.
    static final int BG = 0xff0b0f0c, SURFACE = 0xff111712, SURFACE_HI = 0xff1a231c, OUTLINE = 0xff2a362c,
            TEXT = 0xffd8e6d9, MUTED = 0xff83978a, ERROR = 0xffff7a7a;
    // Accent, set from the chosen theme.
    static int ACCENT = 0xff7cf29a, ON_ACCENT = 0xff04210e, ACCENT_DIM = 0xff15331f;

    static final Typeface MONO = Typeface.MONOSPACE;
    static final Typeface MONO_BOLD = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);

    static void init(Context c, String accent) {
        ACCENT = accentColor(c, accent);
        ON_ACCENT = blend(ACCENT, Color.BLACK, 0.85f);
        ACCENT_DIM = blend(ACCENT, SURFACE, 0.84f);
    }

    static boolean dynamicAvailable() { return Build.VERSION.SDK_INT >= 31; }

    static int accentColor(Context c, String accent) {
        switch (accent) {
            case AMBER: return 0xffffc660;
            case CYAN: return 0xff6ddcff;
            case YOU:
                if (dynamicAvailable()) return c.getColor(android.R.color.system_accent1_200);
                return 0xff7cf29a;
            default: return 0xff7cf29a;
        }
    }

    static int overlayFor(String accent) {
        switch (accent) {
            case AMBER: return R.style.ThemeOverlay_Term_Amber;
            case CYAN: return R.style.ThemeOverlay_Term_Cyan;
            default: return 0;
        }
    }

    /** Linear blend: t=0 gives a, t=1 gives b. */
    static int blend(int a, int b, float t) {
        float s = 1 - t;
        return Color.argb(255,
                Math.round(Color.red(a) * s + Color.red(b) * t),
                Math.round(Color.green(a) * s + Color.green(b) * t),
                Math.round(Color.blue(a) * s + Color.blue(b) * t));
    }

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static ColorStateList checked(int on, int off) {
        return new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{on, off});
    }

    static GradientDrawable round(Context c, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    static GradientDrawable outlined(Context c, int fill, int stroke, float radiusDp, float strokeDp) {
        GradientDrawable d = round(c, fill, radiusDp);
        d.setStroke(dp(c, strokeDp), stroke);
        return d;
    }

    // ---------------------------------------------------------------- layout

    static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(bold ? MONO_BOLD : MONO);
        return t;
    }

    static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static void pad(View v, Context c, int l, int t, int r, int b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    static void add(LinearLayout parent, View child, int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(parent.getContext(), bottomDp);
        parent.addView(child, lp);
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    static View gap(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, dp), dp(c, dp)));
        return v;
    }

    /** "## title" header with optional right-aligned meta text. */
    static LinearLayout section(Context c, String title, String meta) {
        LinearLayout r = row(c);
        pad(r, c, 4, 14, 4, 8);
        TextView hash = text(c, "## ", 14, MUTED, true);
        r.addView(hash);
        r.addView(text(c, title, 14, ACCENT, true), weight(1));
        if (meta != null) r.addView(text(c, meta, 12, MUTED, false));
        return r;
    }

    /** A line like "> key: value" for terminal output. */
    static TextView line(Context c, String s, int color) {
        TextView t = text(c, s, 13, color, false);
        t.setLineSpacing(0, 1.2f);
        pad(t, c, 0, 2, 0, 2);
        return t;
    }

    // ---------------------------------------------------------------- material components

    static final class Card {
        final MaterialCardView view;
        final LinearLayout body;
        Card(MaterialCardView view, LinearLayout body) { this.view = view; this.body = body; }
        void style(boolean active) {
            view.setCardBackgroundColor(active ? ACCENT_DIM : SURFACE);
            view.setStrokeColor(active ? blend(ACCENT, SURFACE, 0.35f) : OUTLINE);
        }
    }

    static Card card(Context c) {
        MaterialCardView v = new MaterialCardView(c);
        v.setRadius(dp(c, 20));
        v.setCardElevation(0);
        v.setStrokeWidth(dp(c, 1));
        LinearLayout body = vertical(c);
        pad(body, c, 16, 14, 16, 14);
        v.addView(body, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        Card card = new Card(v, body);
        card.style(false);
        return card;
    }

    static final int FILLED = 0, TONAL = 1, OUTLINED = 2;

    static MaterialButton button(Context c, String label, int style) {
        MaterialButton b = new MaterialButton(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTypeface(MONO_BOLD);
        b.setCornerRadius(dp(c, 16));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setMinHeight(dp(c, 48));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        switch (style) {
            case FILLED:
                b.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
                b.setTextColor(ON_ACCENT);
                break;
            case TONAL:
                b.setBackgroundTintList(ColorStateList.valueOf(ACCENT_DIM));
                b.setTextColor(ACCENT);
                break;
            default:
                b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                b.setStrokeColor(ColorStateList.valueOf(OUTLINE));
                b.setStrokeWidth(dp(c, 1));
                b.setTextColor(TEXT);
                break;
        }
        return b;
    }

    static MaterialCheckBox checkbox(Context c) {
        MaterialCheckBox cb = new MaterialCheckBox(c);
        cb.setButtonTintList(checked(ACCENT, MUTED));
        cb.setButtonIconTintList(ColorStateList.valueOf(ON_ACCENT));
        return cb;
    }

    static Chip chip(Context c, String label) {
        Chip chip = new Chip(c);
        chip.setText(label);
        chip.setTypeface(MONO_BOLD);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        chip.setCheckedIconTint(ColorStateList.valueOf(ACCENT));
        chip.setChipBackgroundColor(checked(ACCENT_DIM, SURFACE));
        chip.setChipStrokeColor(checked(ACCENT, OUTLINE));
        chip.setChipStrokeWidth(dp(c, 1));
        chip.setTextColor(checked(ACCENT, TEXT));
        return chip;
    }

    static MaterialSwitch toggle(Context c, String label, boolean on) {
        MaterialSwitch s = new MaterialSwitch(c);
        s.setText(label);
        s.setTypeface(MONO);
        s.setTextSize(14);
        s.setTextColor(TEXT);
        s.setThumbTintList(checked(ON_ACCENT, MUTED));
        s.setTrackTintList(checked(ACCENT, SURFACE_HI));
        s.setTrackDecorationTintList(checked(Color.TRANSPARENT, OUTLINE));
        s.setChecked(on);
        pad(s, c, 0, 4, 0, 4);
        return s;
    }

    static TextInputLayout field(Context c, String hint, boolean multiline) {
        TextInputLayout til = new TextInputLayout(c);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        float r = dp(c, 14);
        til.setBoxCornerRadii(r, r, r, r);
        til.setBoxStrokeWidth(dp(c, 1));
        til.setBoxStrokeWidthFocused(dp(c, 2));
        til.setBoxStrokeColorStateList(new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{ACCENT, OUTLINE}));
        til.setHintTextColor(ColorStateList.valueOf(ACCENT));
        til.setDefaultHintTextColor(ColorStateList.valueOf(MUTED));
        til.setPrefixTextColor(ColorStateList.valueOf(MUTED));
        til.setSuffixTextColor(ColorStateList.valueOf(MUTED));
        til.setTypeface(MONO);
        til.setHint(hint);

        TextInputEditText e = new TextInputEditText(til.getContext());
        e.setTextColor(TEXT);
        e.setTypeface(MONO);
        e.setTextSize(15);
        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setSingleLine(true);
        }
        til.addView(e, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return til;
    }

    static LinearProgressIndicator progress(Context c, int value, int max, int color, int thicknessDp) {
        LinearProgressIndicator p = new LinearProgressIndicator(c);
        p.setMax(Math.max(1, max));
        p.setProgressCompat(Math.min(value, Math.max(1, max)), false);
        p.setIndicatorColor(color);
        p.setTrackColor(SURFACE_HI);
        p.setTrackThickness(dp(c, thicknessDp));
        p.setTrackCornerRadius(dp(c, thicknessDp / 2f));
        return p;
    }

    // ---------------------------------------------------------------- formatting

    static String asciiBar(int value, int max, int width) {
        int filled = max <= 0 ? 0 : Math.round(width * Math.min(1f, value / (float) max));
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < width; i++) b.append(i < filled ? '█' : '░');
        return b.append(']').toString();
    }

    static String time(int minutes) {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    static String number(double v) {
        if (v == Math.rint(v)) return String.format(Locale.US, "%,d", (long) v);
        return String.format(Locale.US, "%,.1f", v);
    }

    static String two(int v) { return String.format(Locale.US, "%02d", v); }
}
