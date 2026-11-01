package dev.ayaz.november30;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.US);
    private static final String[] TAB_LABELS = {"today", "month", "stats", "plan"};
    private static final int[] TAB_ICONS = {R.drawable.ic_tab_today, R.drawable.ic_tab_month,
            R.drawable.ic_tab_stats, R.drawable.ic_tab_plan};

    private Store store;
    private int day;
    private int tab;

    private LinearLayout root;
    private TextView prompt;
    private ScrollView scroll;
    private LinearLayout content;
    private BottomNavigationView nav;

    // Live-updating Today views.
    private CircularProgressIndicator ring;
    private TextView ringText, statusLine, barLine, habitCount;

    @Override protected void onCreate(Bundle saved) {
        store = new Store(this);
        String accent = store.accent();
        if (Ui.YOU.equals(accent) && DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivityIfAvailable(this);
        } else if (Ui.overlayFor(accent) != 0) {
            getTheme().applyStyle(Ui.overlayFor(accent), true);
        }
        super.onCreate(saved);
        Ui.init(this, accent);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        day = saved != null ? saved.getInt("day", Challenge.todayIndex()) : Challenge.todayIndex();
        tab = saved != null ? saved.getInt("tab", 0) : 0;
        buildShell();
        render();

        Reminders.createChannel(this);
        Reminders.scheduleAll(this);
        askForNotifications();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("day", day);
        out.putInt("tab", tab);
    }

    // ------------------------------------------------------------------ shell

    private void buildShell() {
        root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout header = Ui.vertical(this);
        Ui.pad(header, this, 20, 14, 20, 6);
        LinearLayout promptRow = Ui.row(this);
        prompt = Ui.text(this, "", 13, Ui.TEXT, false);
        promptRow.addView(prompt);
        TextView cursor = Ui.text(this, "█", 13, Ui.ACCENT, false);
        promptRow.addView(cursor);
        ObjectAnimator blink = ObjectAnimator.ofFloat(cursor, View.ALPHA, 1f, 0f);
        blink.setDuration(530);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setInterpolator(t -> t < 0.5f ? 0f : 1f);
        blink.start();
        header.addView(promptRow);
        header.addView(Ui.text(this, "// dum ho tabhi “accept” karna", 12, Ui.MUTED, false));
        root.addView(header);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = Ui.vertical(this);
        Ui.pad(content, this, 16, 6, 16, 28);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        nav = new BottomNavigationView(this);
        Menu menu = nav.getMenu();
        for (int i = 0; i < TAB_LABELS.length; i++) menu.add(Menu.NONE, i + 1, i, TAB_LABELS[i]).setIcon(TAB_ICONS[i]);
        nav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        nav.setBackgroundColor(Ui.SURFACE);
        nav.setItemIconTintList(Ui.checked(Ui.ACCENT, Ui.MUTED));
        nav.setItemTextColor(Ui.checked(Ui.ACCENT, Ui.MUTED));
        nav.setItemActiveIndicatorColor(android.content.res.ColorStateList.valueOf(Ui.ACCENT_DIM));
        nav.setSelectedItemId(tab + 1);
        nav.setOnItemSelectedListener(item -> {
            tab = item.getItemId() - 1;
            render();
            scroll.scrollTo(0, 0);
            return true;
        });
        nav.setOnItemReselectedListener(item -> scroll.smoothScrollTo(0, 0));
        root.addView(nav);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        setContentView(root);
    }

    private void goTab(int t) {
        if (nav.getSelectedItemId() == t + 1) { tab = t; render(); }
        else nav.setSelectedItemId(t + 1);
        scroll.scrollTo(0, 0);
    }

    private void setPrompt(String cmd) {
        SpannableStringBuilder s = new SpannableStringBuilder();
        append(s, "you@november", Ui.ACCENT);
        append(s, ":~$ ", Ui.MUTED);
        append(s, cmd, Ui.TEXT);
        prompt.setText(s);
    }

    private static void append(SpannableStringBuilder s, String text, int color) {
        int start = s.length();
        s.append(text);
        s.setSpan(new ForegroundColorSpan(color), start, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void render() {
        content.removeAllViews();
        ring = null;
        switch (tab) {
            case 0: setPrompt("./challenge --day " + Ui.two(day) + " "); today(); break;
            case 1: setPrompt("cal nov 2026 "); month(); break;
            case 2: setPrompt("stats --all "); stats(); break;
            default: setPrompt("cat plan.md "); plan(); break;
        }
    }

    // ------------------------------------------------------------------ today

    private void today() {
        boolean future = Challenge.isFuture(day);
        LocalDate date = Challenge.date(day);

        LinearLayout dayNav = Ui.row(this);
        MaterialButton prev = Ui.button(this, "<", Ui.TONAL);
        MaterialButton next = Ui.button(this, ">", Ui.TONAL);
        prev.setEnabled(day > 1);
        next.setEnabled(day < Challenge.DAYS);
        prev.setAlpha(day > 1 ? 1f : 0.35f);
        next.setAlpha(day < Challenge.DAYS ? 1f : 0.35f);
        prev.setOnClickListener(v -> { day--; render(); });
        next.setOnClickListener(v -> { day++; render(); });
        LinearLayout mid = Ui.vertical(this);
        mid.setGravity(Gravity.CENTER);
        TextView big = Ui.text(this, "DAY " + Ui.two(day) + "/" + Challenge.DAYS, 22, Ui.TEXT, true);
        big.setGravity(Gravity.CENTER);
        mid.addView(big);
        boolean isToday = date.equals(LocalDate.now());
        TextView sub = Ui.text(this, date.format(DAY_FMT).toLowerCase(Locale.US) + (isToday ? " · today" : ""),
                12, isToday ? Ui.ACCENT : Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        mid.addView(sub);
        dayNav.addView(prev, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 48)));
        dayNav.addView(mid, Ui.weight(1));
        dayNav.addView(next, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 48)));
        Ui.add(content, dayNav, 12);

        // Summary card: progress ring + status output.
        Ui.Card sum = Ui.card(this);
        LinearLayout row = Ui.row(this);
        FrameLayout ringBox = new FrameLayout(this);
        ring = new CircularProgressIndicator(this);
        ring.setIndicatorSize(Ui.dp(this, 92));
        ring.setTrackThickness(Ui.dp(this, 9));
        ring.setTrackCornerRadius(Ui.dp(this, 5));
        ring.setIndicatorColor(Ui.ACCENT);
        ring.setTrackColor(Ui.SURFACE_HI);
        ring.setMax(Challenge.count());
        ringBox.addView(ring, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        ringText = Ui.text(this, "", 20, Ui.TEXT, true);
        ringText.setGravity(Gravity.CENTER);
        ringBox.addView(ringText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        row.addView(ringBox, new LinearLayout.LayoutParams(Ui.dp(this, 96), Ui.dp(this, 96)));

        LinearLayout info = Ui.vertical(this);
        Ui.pad(info, this, 16, 0, 0, 0);
        statusLine = Ui.text(this, "", 13, Ui.ACCENT, true);
        info.addView(statusLine);
        info.addView(Ui.line(this, "> phase: " + Challenge.phase(day), Ui.MUTED));
        int workouts = store.workoutsInWeek(day);
        info.addView(Ui.line(this, "> workouts: " + workouts + "/" + Challenge.WORKOUTS_PER_WEEK
                + (workouts >= Challenge.WORKOUTS_PER_WEEK ? " ✓" : ""), Ui.MUTED));
        barLine = Ui.text(this, "", 12, Ui.ACCENT, false);
        Ui.pad(barLine, this, 0, 4, 0, 0);
        info.addView(barLine);
        row.addView(info, Ui.weight(1));
        sum.body.addView(row);
        sum.style(true);
        Ui.add(content, sum.view, 4);

        if (future) {
            TextView note = Ui.line(this, "# locked: day hasn't started — preview only", Ui.MUTED);
            Ui.pad(note, this, 4, 10, 4, 0);
            content.addView(note);
        }

        LinearLayout sec = Ui.section(this, "habits", "");
        habitCount = (TextView) sec.getChildAt(sec.getChildCount() - 1);
        content.addView(sec);
        for (Habit h : Challenge.HABITS) Ui.add(content, habitCard(h, future), 10);

        TextView foot = Ui.line(this, "# strong_day >= " + Challenge.STRONG_DAY + "/13 · never miss twice", Ui.MUTED);
        foot.setGravity(Gravity.CENTER);
        Ui.pad(foot, this, 0, 8, 0, 0);
        content.addView(foot);
        updateSummary();
    }

    private void updateSummary() {
        if (ring == null) return;
        int score = store.score(day);
        int max = Challenge.count();
        ring.setProgressCompat(score, true);
        ringText.setText(Ui.two(score) + "\n/" + max);
        if (score == max) statusLine.setText("> status: PERFECT_DAY ✓");
        else if (score >= Challenge.STRONG_DAY) statusLine.setText("> status: STRONG_DAY (+" + (max - score) + ")");
        else statusLine.setText("> status: " + (Challenge.STRONG_DAY - score) + " to strong_day");
        barLine.setText(Ui.asciiBar(score, max, 13) + " " + Math.round(score * 100f / max) + "%");
        if (habitCount != null) habitCount.setText("[" + Ui.two(score) + "/" + max + "]");
    }

    private String ruleFor(Habit h) {
        boolean ramp = store.ramp();
        switch (h.id) {
            case Challenge.STEPS:
                return "target " + Ui.number(Challenge.target(h.id, day, ramp)) + " steps"
                        + (ramp && Challenge.week(day) < 3 ? " (ramp)" : "");
            case Challenge.SKILL:
                return store.skillName() + " · " + Ui.number(Challenge.target(h.id, day, ramp)) + " min";
            case Challenge.MEDITATE:
                return "target " + Ui.number(Challenge.target(h.id, day, ramp)) + " min";
            default:
                return h.rule.toLowerCase(Locale.US);
        }
    }

    private View habitCard(Habit h, boolean future) {
        Ui.Card card = Ui.card(this);
        Ui.pad(card.body, this, 14, 10, 6, 12);
        boolean done = store.done(day, h.id);
        card.style(done);

        LinearLayout top = Ui.row(this);
        TextView idx = Ui.text(this, Ui.two(h.id + 1), 12, done ? Ui.ACCENT : Ui.MUTED, true);
        Ui.pad(idx, this, 0, 0, 12, 0);
        top.addView(idx);
        LinearLayout labels = Ui.vertical(this);
        labels.addView(Ui.text(this, h.emoji + " " + h.title, 15, Ui.TEXT, true));
        labels.addView(Ui.text(this, "// " + ruleFor(h), 12, Ui.MUTED, false));
        top.addView(labels, Ui.weight(1));
        MaterialCheckBox cb = Ui.checkbox(this);
        cb.setChecked(done);
        cb.setEnabled(!future);
        cb.setContentDescription(h.title);
        top.addView(cb);
        card.body.addView(top);

        cb.setOnCheckedChangeListener((b, checked) -> {
            store.setDone(day, h.id, checked);
            card.style(checked);
            idx.setTextColor(checked ? Ui.ACCENT : Ui.MUTED);
            updateSummary();
        });
        if (h.kind == Habit.Kind.CHECK && !future) card.view.setOnClickListener(v -> cb.toggle());

        switch (h.kind) {
            case NUMBER: card.body.addView(numberInput(h, cb, future)); break;
            case TEXT: card.body.addView(textInput(h, cb, future)); break;
            case WORKOUT: card.body.addView(workoutChips(cb, future)); break;
            default: break;
        }
        return card.view;
    }

    private View numberInput(Habit h, MaterialCheckBox cb, boolean future) {
        boolean expense = h.id == Challenge.EXPENSE;
        TextInputLayout til = Ui.field(this, expense ? "spent_today" : "log", false);
        if (expense) til.setPrefixText("₹ ");
        else til.setSuffixText(h.unit);
        EditText e = til.getEditText();
        e.setInputType(h.decimal ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                : InputType.TYPE_CLASS_NUMBER);
        String v = store.value(day, h.id);
        if (v != null) e.setText(v);
        til.setEnabled(!future);
        e.addTextChangedListener(new SimpleWatcher(s -> {
            store.setValue(day, h.id, s);
            if (!store.hasNumber(day, h.id)) return;
            boolean met = store.number(day, h.id) >= Challenge.target(h.id, day, store.ramp());
            if (cb.isChecked() != met) cb.setChecked(met);
        }));
        return indent(til);
    }

    private View textInput(Habit h, MaterialCheckBox cb, boolean future) {
        TextInputLayout til = Ui.field(this, h.id == Challenge.GOALS ? "goals.txt" : "gratitude.log", true);
        til.setPlaceholderText(h.id == Challenge.GOALS ? "1. \n2. \n3. " : "grateful for…");
        EditText e = til.getEditText();
        String v = store.value(day, h.id);
        if (v != null) e.setText(v);
        til.setEnabled(!future);
        e.addTextChangedListener(new SimpleWatcher(s -> {
            store.setValue(day, h.id, s);
            boolean written = !s.trim().isEmpty();
            if (cb.isChecked() != written) cb.setChecked(written);
        }));
        return indent(til);
    }

    private View indent(View v) {
        LinearLayout wrap = Ui.vertical(this);
        Ui.pad(wrap, this, 26, 8, 10, 0);
        wrap.addView(v);
        return wrap;
    }

    private View workoutChips(MaterialCheckBox cb, boolean future) {
        LinearLayout row = Ui.row(this);
        Chip train = Ui.chip(this, "trained");
        Chip rest = Ui.chip(this, "rest_day");
        Runnable sync = () -> {
            String v = store.value(day, Challenge.WORKOUT);
            train.setChecked(Store.WORKOUT.equals(v));
            rest.setChecked(Store.REST.equals(v));
        };
        sync.run();
        train.setEnabled(!future);
        rest.setEnabled(!future);
        View.OnClickListener pick = v -> {
            String choice = v == train ? Store.WORKOUT : Store.REST;
            String current = store.value(day, Challenge.WORKOUT);
            if (choice.equals(current)) {
                store.setValue(day, Challenge.WORKOUT, null);
                cb.setChecked(false);
            } else if (Store.REST.equals(choice)
                    && store.restDaysInWeek(day, day) >= Challenge.maxRestDays(day)) {
                snack("rest_day limit reached this week — time to train");
            } else {
                store.setValue(day, Challenge.WORKOUT, choice);
                cb.setChecked(true);
            }
            sync.run();
        };
        train.setOnClickListener(pick);
        rest.setOnClickListener(pick);
        row.addView(train);
        row.addView(Ui.gap(this, 8));
        row.addView(rest);
        // The checkbox can't say "workout vs rest", so keep it consistent with the chips.
        cb.setOnClickListener(v -> {
            if (cb.isChecked() && store.value(day, Challenge.WORKOUT) == null) {
                store.setValue(day, Challenge.WORKOUT, Store.WORKOUT);
            } else if (!cb.isChecked()) {
                store.setValue(day, Challenge.WORKOUT, null);
            }
            sync.run();
        });
        return indent(row);
    }

    // ------------------------------------------------------------------ month

    private void month() {
        content.addView(Ui.section(this, "november_2026", "tap a day"));
        Ui.Card card = Ui.card(this);
        Ui.pad(card.body, this, 8, 12, 8, 12);
        LinearLayout head = Ui.row(this);
        for (String n : new String[]{"su", "mo", "tu", "we", "th", "fr", "sa"}) {
            TextView t = Ui.text(this, n, 11, Ui.MUTED, true);
            t.setGravity(Gravity.CENTER);
            head.addView(t, Ui.weight(1));
        }
        Ui.add(card.body, head, 6);

        int offset = Challenge.START.getDayOfWeek().getValue() % 7; // Sunday = 0
        int today = Challenge.isActiveToday() ? Challenge.todayIndex() : -1;
        int rows = (offset + Challenge.DAYS + 6) / 7;
        for (int r = 0; r < rows; r++) {
            LinearLayout week = Ui.row(this);
            for (int c = 0; c < 7; c++) {
                int d = r * 7 + c - offset + 1;
                TextView cell = Ui.text(this, "", 13, Ui.TEXT, true);
                cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1);
                int m = Ui.dp(this, 3);
                lp.setMargins(m, m, m, m);
                if (d >= 1 && d <= Challenge.DAYS) {
                    int score = store.score(d);
                    boolean fut = Challenge.isFuture(d);
                    int fill = fut ? Ui.BG : heat(score);
                    cell.setText(Ui.two(d));
                    cell.setTextColor(fut ? Ui.MUTED : score == Challenge.count() ? Ui.ON_ACCENT : Ui.TEXT);
                    if (d == today) cell.setBackground(Ui.outlined(this, fill, Ui.ACCENT, 12, 2));
                    else if (fut) cell.setBackground(Ui.outlined(this, fill, Ui.OUTLINE, 12, 1));
                    else cell.setBackground(Ui.round(this, fill, 12));
                    final int pick = d;
                    cell.setOnClickListener(v -> { day = pick; goTab(0); });
                }
                week.addView(cell, lp);
            }
            card.body.addView(week);
        }
        Ui.add(content, card.view, 8);

        LinearLayout legend = Ui.row(this);
        legend.setGravity(Gravity.CENTER);
        legend.addView(Ui.text(this, "less ", 11, Ui.MUTED, false));
        for (int s : new int[]{0, 4, 8, 11, 13}) {
            View sw = new View(this);
            sw.setBackground(Ui.round(this, heat(s), 3));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 14), Ui.dp(this, 14));
            lp.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
            legend.addView(sw, lp);
        }
        legend.addView(Ui.text(this, " more", 11, Ui.MUTED, false));
        Ui.add(content, legend, 8);

        content.addView(Ui.section(this, "weekly_review", null));
        for (int start = 1; start <= Challenge.DAYS; start += 7) {
            int end = Challenge.weekEnd(start);
            int len = end - start + 1;
            int pts = 0, strong = 0;
            for (int d = start; d <= end; d++) {
                int s = store.score(d);
                pts += s;
                if (s >= Challenge.STRONG_DAY) strong++;
            }
            int max = len * Challenge.count();
            int workouts = store.workoutsInWeek(start);
            int need = len >= 7 ? Challenge.WORKOUTS_PER_WEEK : 1;

            Ui.Card w = Ui.card(this);
            LinearLayout top = Ui.row(this);
            top.addView(Ui.text(this, "wk" + Challenge.week(start), 14, Ui.TEXT, true));
            top.addView(Ui.text(this, "  nov " + Ui.two(start) + "–" + Ui.two(end), 12, Ui.MUTED, false), Ui.weight(1));
            top.addView(Ui.text(this, pts + "/" + max, 14, Ui.ACCENT, true));
            Ui.add(w.body, top, 10);
            Ui.add(w.body, Ui.progress(this, pts, max, Ui.ACCENT, 6), 10);
            w.body.addView(Ui.text(this, "strong " + strong + "/" + len + "  ·  workouts " + workouts + "/" + need
                    + (workouts >= need ? " ✓" : ""), 12, workouts >= need ? Ui.ACCENT : Ui.MUTED, false));
            Ui.add(content, w.view, 10);
        }
    }

    private int heat(int score) {
        if (score == 0) return Ui.SURFACE_HI;
        if (score == Challenge.count()) return Ui.ACCENT;
        if (score >= Challenge.STRONG_DAY) return Ui.blend(Ui.ACCENT, Ui.SURFACE, 0.45f);
        if (score >= 7) return Ui.blend(Ui.ACCENT, Ui.SURFACE, 0.65f);
        return Ui.blend(Ui.ACCENT, Ui.SURFACE, 0.82f);
    }

    // ------------------------------------------------------------------ stats

    private void stats() {
        int elapsed = Challenge.elapsedDays();
        int total = 0, strong = 0, perfect = 0, best = 0, run = 0;
        for (int d = 1; d <= Challenge.DAYS; d++) {
            int s = store.score(d);
            total += s;
            if (s == Challenge.count()) perfect++;
            if (s >= Challenge.STRONG_DAY) { strong++; run++; best = Math.max(best, run); } else run = 0;
        }
        int maxPts = Challenge.maxPoints();
        int goal = Math.round(maxPts * 0.8f);
        int pct = Math.round(total * 100f / maxPts);

        Ui.Card hero = Ui.card(this);
        hero.style(true);
        hero.body.addView(Ui.text(this, "$ score --total", 12, Ui.MUTED, false));
        TextView score = Ui.text(this, total + "/" + maxPts, 36, Ui.ACCENT, true);
        Ui.pad(score, this, 0, 4, 0, 0);
        hero.body.addView(score);
        Ui.add(hero.body, Ui.text(this, pct + "% · win at 80% = " + goal + " pts"
                + (total >= goal ? " ✓" : " · " + (goal - total) + " to go"), 12, Ui.MUTED, false), 12);
        Ui.add(hero.body, Ui.progress(this, total, maxPts, Ui.ACCENT, 10), 0);
        Ui.add(content, hero.view, 10);

        LinearLayout r1 = Ui.row(this), r2 = Ui.row(this);
        tile(r1, String.valueOf(currentStreak()), "streak");
        tile(r1, String.valueOf(best), "best_streak");
        tile(r2, String.valueOf(strong), "strong_days");
        tile(r2, String.valueOf(perfect), "perfect_days");
        Ui.add(content, r1, 8);
        Ui.add(content, r2, 4);

        double steps = 0, sleep = 0, pages = 0, skill = 0, meditate = 0, spend = 0;
        int sleepDays = 0, spendDays = 0, workouts = 0;
        for (int d = 1; d <= Challenge.DAYS; d++) {
            steps += store.number(d, Challenge.STEPS);
            if (store.hasNumber(d, Challenge.SLEEP)) { sleep += store.number(d, Challenge.SLEEP); sleepDays++; }
            pages += store.number(d, Challenge.READ);
            skill += store.number(d, Challenge.SKILL);
            meditate += store.number(d, Challenge.MEDITATE);
            if (store.hasNumber(d, Challenge.EXPENSE)) { spend += store.number(d, Challenge.EXPENSE); spendDays++; }
            if (Store.WORKOUT.equals(store.value(d, Challenge.WORKOUT))) workouts++;
        }
        content.addView(Ui.section(this, "totals", null));
        Ui.Card totals = Ui.card(this);
        kv(totals.body, "steps", Ui.number(steps) + " (~" + Ui.number(Math.round(steps * 0.00075)) + " km)");
        kv(totals.body, "avg_sleep", sleepDays == 0 ? "--" : String.format(Locale.US, "%.1f h", sleep / sleepDays));
        kv(totals.body, "workouts", String.valueOf(workouts));
        kv(totals.body, "pages", Ui.number(pages));
        kv(totals.body, "skill", String.format(Locale.US, "%.1f h", skill / 60));
        kv(totals.body, "meditation", Ui.number(meditate) + " min");
        kv(totals.body, "spent", "₹" + Ui.number(spend)
                + (spendDays == 0 ? "" : " (₹" + Ui.number(Math.round(spend / spendDays)) + "/d)"));
        Ui.add(content, totals.view, 4);

        content.addView(Ui.section(this, "consistency", elapsed + " days"));
        Ui.Card per = Ui.card(this);
        for (Habit h : Challenge.HABITS) {
            int n = 0;
            for (int d = 1; d <= Challenge.DAYS; d++) if (store.done(d, h.id)) n++;
            boolean good = elapsed > 0 && n >= 0.8f * elapsed;
            LinearLayout r = Ui.row(this);
            r.addView(Ui.text(this, Ui.two(h.id + 1) + " " + h.title, 12, Ui.TEXT, false), Ui.weight(1));
            r.addView(Ui.text(this, n + "/" + elapsed, 12, good ? Ui.ACCENT : Ui.MUTED, true));
            Ui.add(per.body, r, 6);
            Ui.add(per.body, Ui.progress(this, n, Math.max(1, elapsed),
                    good ? Ui.ACCENT : Ui.blend(Ui.ACCENT, Ui.MUTED, 0.5f), 4), 12);
        }
        Ui.add(content, per.view, 16);

        MaterialButton share = Ui.button(this, "$ share --progress", Ui.FILLED);
        final int totalF = total, strongF = strong;
        share.setOnClickListener(v -> {
            String msg = "november_challenge — day " + Challenge.todayIndex() + "/30\n"
                    + "score: " + totalF + "/" + maxPts + " (" + pct + "%)\n"
                    + "strong_days: " + strongF + " · streak: " + currentStreak() + "\n"
                    + "dum ho tabhi accept karna 💪";
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(send, "Share progress"));
        });
        Ui.add(content, share, 0);
    }

    /** Consecutive strong days ending today (today may still be in progress). */
    private int currentStreak() {
        int d = Challenge.elapsedDays();
        if (d == 0) return 0;
        if (store.score(d) < Challenge.STRONG_DAY) d--;
        int s = 0;
        while (d >= 1 && store.score(d) >= Challenge.STRONG_DAY) { s++; d--; }
        return s;
    }

    private void tile(LinearLayout parent, String value, String label) {
        Ui.Card t = Ui.card(this);
        t.body.addView(Ui.text(this, value, 26, Ui.TEXT, true));
        t.body.addView(Ui.text(this, label, 11, Ui.MUTED, false));
        LinearLayout.LayoutParams lp = Ui.weight(1);
        int m = Ui.dp(this, 4);
        lp.setMargins(parent.getChildCount() == 0 ? 0 : m, 0, parent.getChildCount() == 0 ? m : 0, 0);
        parent.addView(t.view, lp);
    }

    private void kv(LinearLayout parent, String key, String value) {
        LinearLayout r = Ui.row(this);
        Ui.pad(r, this, 0, 5, 0, 5);
        r.addView(Ui.text(this, key, 13, Ui.MUTED, false), Ui.weight(1));
        r.addView(Ui.text(this, value, 13, Ui.TEXT, true));
        parent.addView(r);
    }

    // ------------------------------------------------------------------ plan & settings

    private void plan() {
        content.addView(Ui.section(this, "rules", null));
        Ui.Card rules = Ui.card(this);
        bullets(rules.body,
                "30 days × 13 habits = 390 pts",
                "win at 80% = 312 pts",
                "strong_day = 10+ habits; perfect = 13",
                "workout 4–5×/wk, max 3 rest_days",
                "never miss twice in a row",
                "data stays on this device");
        Ui.add(content, rules.view, 4);

        content.addView(Ui.section(this, "ramp", store.ramp() ? "on" : "off"));
        Ui.Card ramp = Ui.card(this);
        ramp.body.addView(Ui.line(this, "wk  dates   steps  med  skill", Ui.MUTED));
        ramp.body.addView(Ui.line(this, "1   01–07   7.0k  10m  30m", Ui.TEXT));
        ramp.body.addView(Ui.line(this, "2   08–14   8.5k  15m  45m", Ui.TEXT));
        ramp.body.addView(Ui.line(this, "3   15–21  10.0k  15m  45m", Ui.TEXT));
        ramp.body.addView(Ui.line(this, "4+  22–30  10.0k  15m  45m", Ui.ACCENT));
        Ui.add(content, ramp.view, 4);

        content.addView(Ui.section(this, "daily_blueprint", null));
        Ui.Card bp = Ui.card(this);
        schedule(bp.body, "06:30", "wake · phone on charger 60 min");
        schedule(bp.body, "06:40", "water → meditate → 3 goals");
        schedule(bp.body, "07:00", "workout / 30-min brisk walk");
        schedule(bp.body, "meals", "home food · phone away");
        schedule(bp.body, "post ", "15-min walks after meals");
        schedule(bp.body, "18:30", "45 min skill → 10-min tidy");
        schedule(bp.body, "21:30", "log ₹ · read 10 pg · gratitude");
        schedule(bp.body, "22:30", "lights out (7–8 h)");
        Ui.add(content, bp.view, 4);

        content.addView(Ui.section(this, "config", null));
        Ui.Card s = Ui.card(this);

        TextInputLayout skill = Ui.field(this, "skill_name", false);
        skill.getEditText().setText(store.skillName());
        skill.getEditText().addTextChangedListener(new SimpleWatcher(t -> {
            if (!t.trim().isEmpty()) store.setSkillName(t.trim());
        }));
        Ui.add(s.body, skill, 10);

        MaterialSwitch rampSw = Ui.toggle(this, "ramp_up_targets", store.ramp());
        rampSw.setOnCheckedChangeListener((b, on) -> store.setRamp(on));
        Ui.add(s.body, rampSw, 2);

        MaterialSwitch remSw = Ui.toggle(this, "daily_reminders", store.remindersOn());
        Ui.add(s.body, remSw, 8);
        MaterialButton morning = Ui.button(this, "am " + Ui.time(store.morningReminder()), Ui.TONAL);
        MaterialButton evening = Ui.button(this, "pm " + Ui.time(store.eveningReminder()), Ui.TONAL);
        remSw.setOnCheckedChangeListener((b, on) -> {
            store.setRemindersOn(on);
            Reminders.scheduleAll(this);
            if (on) askForNotifications();
        });
        morning.setOnClickListener(v -> pickTime("morning reminder", store.morningReminder(), m -> {
            store.setMorningReminder(m);
            morning.setText("am " + Ui.time(m));
        }));
        evening.setOnClickListener(v -> pickTime("evening reminder", store.eveningReminder(), m -> {
            store.setEveningReminder(m);
            evening.setText("pm " + Ui.time(m));
        }));
        LinearLayout times = Ui.row(this);
        times.addView(morning, Ui.weight(1));
        times.addView(Ui.gap(this, 8));
        times.addView(evening, Ui.weight(1));
        Ui.add(s.body, times, 14);

        s.body.addView(Ui.text(this, "accent", 12, Ui.MUTED, false));
        LinearLayout accents = Ui.row(this);
        Ui.pad(accents, this, 0, 4, 0, 0);
        String current = store.accent();
        String[][] options = {{Ui.GREEN, "green"}, {Ui.AMBER, "amber"}, {Ui.CYAN, "cyan"}, {Ui.YOU, "you"}};
        for (String[] o : options) {
            if (Ui.YOU.equals(o[0]) && !Ui.dynamicAvailable()) continue;
            Chip chip = Ui.chip(this, o[1]);
            chip.setChecked(o[0].equals(current));
            chip.setOnClickListener(v -> {
                if (o[0].equals(store.accent())) { chip.setChecked(true); return; }
                store.setAccent(o[0]);
                recreate();
            });
            accents.addView(chip);
            accents.addView(Ui.gap(this, 6));
        }
        hScroll(s.body, accents);
        Ui.add(content, s.view, 14);

        MaterialButton reset = Ui.button(this, "rm -rf progress/", Ui.OUTLINED);
        reset.setTextColor(Ui.ERROR);
        reset.setStrokeColor(android.content.res.ColorStateList.valueOf(Ui.blend(Ui.ERROR, Ui.SURFACE, 0.5f)));
        reset.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Reset all progress?")
                .setMessage("Erases every logged habit, number and note. Settings are kept. Can't be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (d, w) -> { store.resetProgress(); render(); snack("progress cleared"); })
                .show());
        Ui.add(content, reset, 0);
    }

    private void hScroll(LinearLayout parent, View child) {
        android.widget.HorizontalScrollView h = new android.widget.HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(false);
        h.addView(child);
        parent.addView(h);
    }

    private void bullets(LinearLayout parent, String... lines) {
        for (String l : lines) parent.addView(Ui.line(this, "> " + l, Ui.TEXT));
    }

    private void schedule(LinearLayout parent, String time, String what) {
        LinearLayout r = Ui.row(this);
        Ui.pad(r, this, 0, 3, 0, 3);
        r.addView(Ui.text(this, time + "  ", 13, Ui.ACCENT, true));
        r.addView(Ui.text(this, what, 13, Ui.TEXT, false), Ui.weight(1));
        parent.addView(r);
    }

    private interface MinutePicked { void on(int minutes); }

    private void pickTime(String title, int current, MinutePicked cb) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(current / 60)
                .setMinute(current % 60)
                .setTitleText(title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            cb.on(picker.getHour() * 60 + picker.getMinute());
            Reminders.scheduleAll(this);
        });
        picker.show(getSupportFragmentManager(), "time");
    }

    private void askForNotifications() {
        if (Build.VERSION.SDK_INT < 33 || !store.remindersOn()) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        if (store.askedNotifications() && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) return;
        store.setAskedNotifications();
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
    }

    private void snack(String s) {
        Snackbar.make(root, s, Snackbar.LENGTH_SHORT).setAnchorView(nav).show();
    }

    /** TextWatcher that only cares about the final text. */
    private static final class SimpleWatcher implements TextWatcher {
        interface Changed { void on(String text); }
        private final Changed changed;
        SimpleWatcher(Changed changed) { this.changed = changed; }
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) { changed.on(s.toString()); }
    }
}
