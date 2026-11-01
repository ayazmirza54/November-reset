package dev.ayaz.november30;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String[] TABS = {"Today", "Month", "Stats", "Plan"};
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US);

    private Store store;
    private int day;
    private int tab;

    private ScrollView scroll;
    private LinearLayout content;
    private LinearLayout nav;

    // Today-screen views that update live without a full rebuild.
    private ProgressRing ring;
    private TextView scoreLine;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        store = new Store(this);
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

    @Override protected void onResume() {
        super.onResume();
        // Date may have rolled over while the app was in the background.
        if (tab != 0) render();
    }

    // ------------------------------------------------------------------ shell

    private void buildShell() {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout header = Ui.vertical(this);
        Ui.pad(header, this, 20, 16, 20, 8);
        TextView title = Ui.text(this, "NOVEMBER CHALLENGE", 22, Ui.GOLD, true);
        title.setLetterSpacing(0.04f);
        header.addView(title);
        header.addView(Ui.text(this, "Dum ho tabhi “accept” karna", 13, Ui.MUTED, false));
        root.addView(header);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = Ui.vertical(this);
        Ui.pad(content, this, 16, 8, 16, 24);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        nav = Ui.horizontal(this);
        nav.setBackgroundColor(Ui.CARD);
        Ui.pad(nav, this, 8, 6, 8, 6);
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            TextView t = Ui.text(this, TABS[i], 14, Ui.MUTED, true);
            t.setGravity(Gravity.CENTER);
            Ui.pad(t, this, 0, 12, 0, 12);
            t.setOnClickListener(v -> { tab = index; render(); scroll.scrollTo(0, 0); });
            nav.addView(t, Ui.weight(1));
        }
        root.addView(nav);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                left = i.left; top = i.top; right = i.right; bottom = i.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top, right, bottom);
            return insets;
        });
        setContentView(root);
    }

    private void render() {
        for (int i = 0; i < nav.getChildCount(); i++) {
            TextView t = (TextView) nav.getChildAt(i);
            boolean on = i == tab;
            t.setTextColor(on ? Ui.BG : Ui.MUTED);
            t.setBackground(on ? Ui.round(this, Ui.GOLD, 12) : null);
        }
        content.removeAllViews();
        ring = null;
        scoreLine = null;
        switch (tab) {
            case 0: today(); break;
            case 1: month(); break;
            case 2: stats(); break;
            default: plan(); break;
        }
    }

    // ------------------------------------------------------------------ today

    private void today() {
        boolean future = Challenge.isFuture(day);
        LocalDate date = Challenge.date(day);

        LinearLayout dayNav = Ui.horizontal(this);
        TextView prev = Ui.button(this, "‹", false);
        TextView next = Ui.button(this, "›", false);
        prev.setAlpha(day > 1 ? 1f : 0.3f);
        next.setAlpha(day < Challenge.DAYS ? 1f : 0.3f);
        prev.setOnClickListener(v -> { if (day > 1) { day--; render(); } });
        next.setOnClickListener(v -> { if (day < Challenge.DAYS) { day++; render(); } });
        LinearLayout mid = Ui.vertical(this);
        mid.setGravity(Gravity.CENTER);
        mid.addView(Ui.text(this, "DAY " + day + " / " + Challenge.DAYS, 18, Ui.WHITE, true));
        String sub = date.format(DAY_FMT) + (date.equals(LocalDate.now()) ? " · today" : "");
        mid.addView(Ui.text(this, sub, 12, Ui.MUTED, false));
        for (int i = 0; i < mid.getChildCount(); i++) ((TextView) mid.getChildAt(i)).setGravity(Gravity.CENTER);
        dayNav.addView(prev);
        dayNav.addView(mid, Ui.weight(1));
        dayNav.addView(next);
        Ui.add(content, dayNav, 14);

        LinearLayout summary = Ui.card(this);
        LinearLayout row = Ui.horizontal(this);
        ring = new ProgressRing(this);
        int size = Ui.dp(this, 104);
        row.addView(ring, new LinearLayout.LayoutParams(size, size));
        LinearLayout info = Ui.vertical(this);
        Ui.pad(info, this, 16, 0, 0, 0);
        scoreLine = Ui.text(this, "", 16, Ui.WHITE, true);
        info.addView(scoreLine);
        TextView phase = Ui.text(this, Challenge.phase(day), 12, Ui.GOLD, false);
        Ui.pad(phase, this, 0, 6, 0, 0);
        info.addView(phase);
        int workouts = store.workoutsInWeek(day);
        TextView weekLine = Ui.text(this, "Workouts this week: " + workouts + " / " + Challenge.WORKOUTS_PER_WEEK,
                12, workouts >= Challenge.WORKOUTS_PER_WEEK ? Ui.GREEN : Ui.MUTED, false);
        Ui.pad(weekLine, this, 0, 4, 0, 0);
        info.addView(weekLine);
        row.addView(info, Ui.weight(1));
        summary.addView(row);
        Ui.add(content, summary, 14);
        updateSummary();

        if (future) {
            TextView note = Ui.text(this, "This day hasn't started yet. Preview only — come back on "
                    + date.format(DAY_FMT) + ".", 13, Ui.MUTED, false);
            Ui.pad(note, this, 4, 0, 4, 10);
            content.addView(note);
        }

        for (Habit h : Challenge.HABITS) Ui.add(content, habitCard(h, future), 10);

        TextView foot = Ui.text(this, "Strong day = " + Challenge.STRONG_DAY + "+ habits. Miss a day? Never miss two.",
                12, Ui.MUTED, false);
        foot.setGravity(Gravity.CENTER);
        Ui.pad(foot, this, 0, 8, 0, 0);
        content.addView(foot);
    }

    private void updateSummary() {
        if (ring == null) return;
        int score = store.score(day);
        ring.set(score, Challenge.count());
        String msg;
        if (score == Challenge.count()) msg = "Perfect day 🔥";
        else if (score >= Challenge.STRONG_DAY) msg = "Strong day — " + (Challenge.count() - score) + " to perfect";
        else msg = (Challenge.STRONG_DAY - score) + " more for a strong day";
        scoreLine.setText(msg);
    }

    private String ruleFor(Habit h) {
        boolean ramp = store.ramp();
        switch (h.id) {
            case Challenge.STEPS:
                return "Target: " + Ui.number(Challenge.target(h.id, day, ramp)) + " steps"
                        + (ramp && Challenge.week(day) < 3 ? " (ramp-up)" : "");
            case Challenge.SKILL:
                return store.skillName() + " · target " + Ui.number(Challenge.target(h.id, day, ramp)) + " min";
            case Challenge.MEDITATE:
                return "Target: " + Ui.number(Challenge.target(h.id, day, ramp)) + " min";
            default:
                return h.rule;
        }
    }

    private View habitCard(Habit h, boolean future) {
        LinearLayout card = Ui.card(this);
        boolean done = store.done(day, h.id);
        card.setBackground(done ? Ui.outlined(this, Ui.CARD, 0xff2f6b56, 16) : Ui.round(this, Ui.CARD, 16));
        Ui.pad(card, this, 14, 10, 8, 10);

        LinearLayout top = Ui.horizontal(this);
        TextView emoji = Ui.text(this, h.emoji, 22, Ui.WHITE, false);
        Ui.pad(emoji, this, 0, 0, 12, 0);
        top.addView(emoji);
        LinearLayout labels = Ui.vertical(this);
        labels.addView(Ui.text(this, h.title, 15, Ui.WHITE, true));
        labels.addView(Ui.text(this, ruleFor(h), 12, Ui.MUTED, false));
        top.addView(labels, Ui.weight(1));
        CheckBox cb = new CheckBox(this);
        cb.setChecked(done);
        cb.setEnabled(!future);
        cb.setContentDescription(h.title);
        top.addView(cb);
        card.addView(top);

        CompoundButton.OnCheckedChangeListener onCheck = (b, checked) -> {
            store.setDone(day, h.id, checked);
            card.setBackground(checked ? Ui.outlined(this, Ui.CARD, 0xff2f6b56, 16) : Ui.round(this, Ui.CARD, 16));
            Ui.pad(card, this, 14, 10, 8, 10);
            updateSummary();
        };
        cb.setOnCheckedChangeListener(onCheck);
        if (h.kind == Habit.Kind.CHECK && !future) top.setOnClickListener(v -> cb.toggle());

        switch (h.kind) {
            case NUMBER: card.addView(numberInput(h, cb, future)); break;
            case TEXT: card.addView(textInput(h, cb, future)); break;
            case WORKOUT: card.addView(workoutChips(cb, future)); break;
            default: break;
        }
        return card;
    }

    private View numberInput(Habit h, CheckBox cb, boolean future) {
        LinearLayout row = Ui.horizontal(this);
        Ui.pad(row, this, 34, 0, 8, 0);
        if (h.id == Challenge.EXPENSE) row.addView(Ui.text(this, "₹ ", 15, Ui.MUTED, true));
        EditText e = Ui.input(this, h.id == Challenge.EXPENSE ? "Amount spent" : "0", false);
        e.setInputType(h.decimal ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                : InputType.TYPE_CLASS_NUMBER);
        String v = store.value(day, h.id);
        if (v != null) e.setText(v);
        e.setEnabled(!future);
        row.addView(e, Ui.weight(1));
        if (h.id != Challenge.EXPENSE) {
            TextView unit = Ui.text(this, " " + h.unit, 13, Ui.MUTED, false);
            row.addView(unit);
        }
        e.addTextChangedListener(new SimpleWatcher(s -> {
            store.setValue(day, h.id, s);
            if (!store.hasNumber(day, h.id)) return;
            double target = Challenge.target(h.id, day, store.ramp());
            boolean met = store.number(day, h.id) >= target;
            if (cb.isChecked() != met) cb.setChecked(met);
        }));
        return row;
    }

    private View textInput(Habit h, CheckBox cb, boolean future) {
        EditText e = Ui.input(this, h.id == Challenge.GOALS ? "1.\n2.\n3." : "I'm grateful for…", true);
        String v = store.value(day, h.id);
        if (v != null) e.setText(v);
        e.setEnabled(!future);
        e.addTextChangedListener(new SimpleWatcher(s -> {
            store.setValue(day, h.id, s);
            boolean written = !s.trim().isEmpty();
            if (cb.isChecked() != written) cb.setChecked(written);
        }));
        LinearLayout wrap = Ui.vertical(this);
        Ui.pad(wrap, this, 34, 0, 8, 0);
        wrap.addView(e);
        return wrap;
    }

    private View workoutChips(CheckBox cb, boolean future) {
        LinearLayout row = Ui.horizontal(this);
        Ui.pad(row, this, 34, 6, 8, 2);
        TextView train = Ui.chip(this, "🏋️ Trained");
        TextView rest = Ui.chip(this, "😴 Rest day");
        Runnable restyle = () -> {
            String v = store.value(day, Challenge.WORKOUT);
            Ui.styleChip(train, Store.WORKOUT.equals(v));
            Ui.styleChip(rest, Store.REST.equals(v));
        };
        restyle.run();
        View.OnClickListener pick = v -> {
            if (future) return;
            String choice = v == train ? Store.WORKOUT : Store.REST;
            String current = store.value(day, Challenge.WORKOUT);
            if (choice.equals(current)) {
                store.setValue(day, Challenge.WORKOUT, null);
                cb.setChecked(false);
            } else {
                if (Store.REST.equals(choice)
                        && store.restDaysInWeek(day, day) >= Challenge.maxRestDays(day)) {
                    toast("Rest-day limit reached for this week — time to train!");
                    return;
                }
                store.setValue(day, Challenge.WORKOUT, choice);
                cb.setChecked(true);
            }
            restyle.run();
        };
        train.setOnClickListener(pick);
        rest.setOnClickListener(pick);
        row.addView(train);
        View gap = new View(this);
        row.addView(gap, new LinearLayout.LayoutParams(Ui.dp(this, 8), 1));
        row.addView(rest);
        // The checkbox alone can't encode workout vs rest, so route it through the chips.
        cb.setOnClickListener(v -> {
            if (cb.isChecked() && store.value(day, Challenge.WORKOUT) == null) {
                store.setValue(day, Challenge.WORKOUT, Store.WORKOUT);
            } else if (!cb.isChecked()) {
                store.setValue(day, Challenge.WORKOUT, null);
            }
            restyle.run();
        });
        return row;
    }

    // ------------------------------------------------------------------ month

    private void month() {
        content.addView(Ui.section(this, "NOVEMBER 2026"));
        LinearLayout card = Ui.card(this);
        Ui.pad(card, this, 8, 12, 8, 12);
        LinearLayout head = Ui.horizontal(this);
        for (String n : new String[]{"S", "M", "T", "W", "T", "F", "S"}) {
            TextView t = Ui.text(this, n, 12, Ui.MUTED, true);
            t.setGravity(Gravity.CENTER);
            head.addView(t, Ui.weight(1));
        }
        Ui.add(card, head, 6);

        int offset = Challenge.START.getDayOfWeek().getValue() % 7; // Sunday = 0
        int today = Challenge.isActiveToday() ? Challenge.todayIndex() : -1;
        int cells = offset + Challenge.DAYS;
        int rows = (cells + 6) / 7;
        for (int r = 0; r < rows; r++) {
            LinearLayout week = Ui.horizontal(this);
            for (int c = 0; c < 7; c++) {
                int d = r * 7 + c - offset + 1;
                TextView cell = Ui.text(this, "", 14, Ui.WHITE, false);
                cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1);
                lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3));
                if (d >= 1 && d <= Challenge.DAYS) {
                    int score = store.score(d);
                    boolean fut = Challenge.isFuture(d);
                    cell.setText(d + (fut || score == 0 ? "" : "\n" + score));
                    cell.setTextSize(fut || score == 0 ? 14 : 12);
                    int color = fut ? Ui.BG : heat(score);
                    cell.setBackground(d == today ? Ui.outlined(this, color, Ui.GOLD, 10) : Ui.round(this, color, 10));
                    cell.setTextColor(fut ? Ui.MUTED : Ui.WHITE);
                    final int pick = d;
                    cell.setOnClickListener(v -> { day = pick; tab = 0; render(); scroll.scrollTo(0, 0); });
                }
                week.addView(cell, lp);
            }
            card.addView(week);
        }
        Ui.add(content, card, 8);

        LinearLayout legend = Ui.horizontal(this);
        legend.setGravity(Gravity.CENTER);
        legendItem(legend, heat(0), "0");
        legendItem(legend, heat(5), "1–6");
        legendItem(legend, heat(8), "7–9");
        legendItem(legend, heat(11), "10–12");
        legendItem(legend, heat(13), "13");
        Ui.add(content, legend, 16);

        content.addView(Ui.section(this, "WEEKLY REVIEW"));
        for (int start = 1; start <= Challenge.DAYS; start += 7) {
            int end = Challenge.weekEnd(start);
            int pts = 0, max = 0, strong = 0;
            for (int d = start; d <= end; d++) {
                pts += store.score(d);
                max += Challenge.count();
                if (store.score(d) >= Challenge.STRONG_DAY) strong++;
            }
            LinearLayout w = Ui.card(this);
            LinearLayout top = Ui.horizontal(this);
            top.addView(Ui.text(this, "Week " + Challenge.week(start) + "  ·  Nov " + start + "–" + end,
                    14, Ui.WHITE, true), Ui.weight(1));
            top.addView(Ui.text(this, pts + " / " + max, 14, Ui.GOLD, true));
            Ui.add(w, top, 8);
            Ui.add(w, Ui.bar(this, max == 0 ? 0 : pts / (float) max, Ui.GOLD), 8);
            int workouts = store.workoutsInWeek(start);
            int need = end - start + 1 >= 7 ? Challenge.WORKOUTS_PER_WEEK : 1;
            w.addView(Ui.text(this, "Workouts " + workouts + "/" + need + "  ·  Strong days " + strong
                    + "/" + (end - start + 1), 12, workouts >= need ? Ui.GREEN : Ui.MUTED, false));
            Ui.add(content, w, 10);
        }
    }

    private int heat(int score) {
        if (score == 0) return Ui.CARD_2;
        if (score == Challenge.count()) return Ui.GREEN;
        if (score >= Challenge.STRONG_DAY) return 0xff2f6b56;
        if (score >= 7) return 0xff5c5426;
        return 0xff4a3a2a;
    }

    private void legendItem(LinearLayout parent, int color, String label) {
        View sw = new View(this);
        sw.setBackground(Ui.round(this, color, 3));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 12), Ui.dp(this, 12));
        lp.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 4), 0);
        parent.addView(sw, lp);
        parent.addView(Ui.text(this, label, 11, Ui.MUTED, false));
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
        int pct = Math.round(total * 100f / Challenge.maxPoints());

        LinearLayout hero = Ui.card(this);
        hero.addView(Ui.text(this, "CHALLENGE SCORE", 12, Ui.GOLD, true));
        hero.addView(Ui.text(this, total + " / " + Challenge.maxPoints(), 34, Ui.WHITE, true));
        Ui.add(hero, Ui.text(this, pct + "% · goal is 80% (" + Math.round(Challenge.maxPoints() * 0.8)
                + " pts)", 13, pct >= 80 ? Ui.GREEN : Ui.MUTED, false), 10);
        Ui.add(hero, Ui.bar(this, total / (float) Challenge.maxPoints(), pct >= 80 ? Ui.GREEN : Ui.GOLD), 0);
        Ui.add(content, hero, 12);

        LinearLayout tiles = Ui.horizontal(this);
        tile(tiles, String.valueOf(currentStreak()), "current streak");
        tile(tiles, String.valueOf(best), "best streak");
        tile(tiles, strong + "", "strong days");
        tile(tiles, perfect + "", "perfect days");
        Ui.add(content, tiles, 16);

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
        content.addView(Ui.section(this, "TOTALS"));
        LinearLayout totals = Ui.card(this);
        statRow(totals, "🚶 Steps walked", Ui.number(steps) + "  (~" + Ui.number(Math.round(steps * 0.00075)) + " km)");
        statRow(totals, "🌙 Average sleep", sleepDays == 0 ? "—" : String.format(Locale.US, "%.1f hrs", sleep / sleepDays));
        statRow(totals, "💪 Workouts", workouts + " sessions");
        statRow(totals, "📖 Pages read", Ui.number(pages) + "  (~" + Math.max(0, Math.round(pages / 250)) + " books)");
        statRow(totals, "🧠 " + store.skillName(), String.format(Locale.US, "%.1f hrs", skill / 60));
        statRow(totals, "🧘 Meditation", Ui.number(meditate) + " min");
        statRow(totals, "💸 Spent", "₹" + Ui.number(spend)
                + (spendDays == 0 ? "" : "  (₹" + Ui.number(Math.round(spend / spendDays)) + "/day)"));
        Ui.add(content, totals, 16);

        content.addView(Ui.section(this, "HABIT CONSISTENCY"));
        LinearLayout per = Ui.card(this);
        for (Habit h : Challenge.HABITS) {
            int n = 0;
            for (int d = 1; d <= Challenge.DAYS; d++) if (store.done(d, h.id)) n++;
            float f = elapsed == 0 ? 0 : n / (float) elapsed;
            LinearLayout r = Ui.horizontal(this);
            r.addView(Ui.text(this, h.emoji + "  " + h.title, 13, Ui.WHITE, false), Ui.weight(1));
            r.addView(Ui.text(this, n + "/" + elapsed, 13, f >= 0.8f ? Ui.GREEN : Ui.MUTED, true));
            Ui.add(per, r, 4);
            Ui.add(per, Ui.bar(this, f, f >= 0.8f ? Ui.GREEN : Ui.GOLD), 12);
        }
        Ui.add(content, per, 16);

        TextView share = Ui.button(this, "SHARE MY PROGRESS", true);
        final int totalF = total, strongF = strong;
        share.setOnClickListener(v -> {
            String msg = "November Challenge — Day " + Challenge.todayIndex() + "/30\n"
                    + "Score: " + totalF + "/" + Challenge.maxPoints() + " (" + pct + "%)\n"
                    + "Strong days: " + strongF + " · Current streak: " + currentStreak() + "\n"
                    + "Dum ho tabhi accept karna 💪";
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
        LinearLayout t = Ui.vertical(this);
        t.setBackground(Ui.round(this, Ui.CARD, 14));
        t.setGravity(Gravity.CENTER);
        Ui.pad(t, this, 4, 12, 4, 12);
        TextView v = Ui.text(this, value, 22, Ui.WHITE, true);
        v.setGravity(Gravity.CENTER);
        TextView l = Ui.text(this, label, 10, Ui.MUTED, false);
        l.setGravity(Gravity.CENTER);
        t.addView(v);
        t.addView(l);
        LinearLayout.LayoutParams lp = Ui.weight(1);
        lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        parent.addView(t, lp);
    }

    private void statRow(LinearLayout parent, String label, String value) {
        LinearLayout r = Ui.horizontal(this);
        Ui.pad(r, this, 0, 6, 0, 6);
        r.addView(Ui.text(this, label, 14, Ui.WHITE, false), Ui.weight(1));
        r.addView(Ui.text(this, value, 14, Ui.GOLD, true));
        parent.addView(r);
    }

    // ------------------------------------------------------------------ plan & settings

    private void plan() {
        content.addView(Ui.section(this, "THE RULES"));
        LinearLayout rules = Ui.card(this);
        bullet(rules, "30 days, 13 habits, 390 points. Hit 80% (312) and you've won.");
        bullet(rules, "A strong day is 10+ habits. Perfect days are a bonus, not the bar.");
        bullet(rules, "Workout 4–5× a week; the other days are planned rest (max 3/week).");
        bullet(rules, "Never miss twice. One bad day is noise, two is a new habit.");
        bullet(rules, "Log honestly. Only you see this data — it never leaves your phone.");
        Ui.add(content, rules, 12);

        content.addView(Ui.section(this, "4-WEEK RAMP"));
        LinearLayout ramp = Ui.card(this);
        bullet(ramp, "Week 1 (Nov 1–7) — Foundation: 7K steps, 10 min meditation, 30 min skill.");
        bullet(ramp, "Week 2 (Nov 8–14) — Build: 8.5K steps, 15 min meditation, 45 min skill.");
        bullet(ramp, "Week 3 (Nov 15–21) — Push: full 10K steps, every habit at full target.");
        bullet(ramp, "Week 4+ (Nov 22–30) — Lock in: same targets, aim for 11+/13 daily.");
        Ui.add(content, ramp, 12);

        content.addView(Ui.section(this, "DAILY BLUEPRINT"));
        LinearLayout bp = Ui.card(this);
        bullet(bp, "6:30 — Wake. Phone stays on the charger for 60 min.");
        bullet(bp, "6:40 — Water, 10–15 min meditation, write 3 goals.");
        bullet(bp, "7:00 — Workout or a brisk 30-min walk (≈ 4K steps).");
        bullet(bp, "Meals — home food, no phone at the table.");
        bullet(bp, "Lunch/evening — walk after meals to close the step gap.");
        bullet(bp, "Evening — 45 min deep skill practice, then 10-min room/desk reset.");
        bullet(bp, "21:30 — Log expenses, read 10 pages, write 3 gratitudes.");
        bullet(bp, "22:30 — Lights out for 7–8 hours of sleep.");
        Ui.add(content, bp, 16);

        content.addView(Ui.section(this, "SETTINGS"));
        LinearLayout s = Ui.card(this);

        s.addView(Ui.text(this, "Skill you're mastering", 13, Ui.MUTED, false));
        EditText skill = Ui.input(this, "e.g. Terraform, guitar, Spanish", false);
        skill.setText(store.skillName());
        skill.addTextChangedListener(new SimpleWatcher(t -> {
            if (!t.trim().isEmpty()) store.setSkillName(t.trim());
        }));
        Ui.add(s, skill, 12);

        Switch rampSw = settingSwitch(s, "Ramp-up targets (weeks 1–2)", store.ramp());
        rampSw.setOnCheckedChangeListener((b, on) -> store.setRamp(on));

        Switch remSw = settingSwitch(s, "Daily reminders", store.remindersOn());
        TextView morning = Ui.button(this, "Morning · " + Ui.time(store.morningReminder()), false);
        TextView evening = Ui.button(this, "Evening · " + Ui.time(store.eveningReminder()), false);
        remSw.setOnCheckedChangeListener((b, on) -> {
            store.setRemindersOn(on);
            Reminders.scheduleAll(this);
            if (on) askForNotifications();
        });
        morning.setOnClickListener(v -> pickTime(store.morningReminder(), m -> {
            store.setMorningReminder(m);
            morning.setText("Morning · " + Ui.time(m));
        }));
        evening.setOnClickListener(v -> pickTime(store.eveningReminder(), m -> {
            store.setEveningReminder(m);
            evening.setText("Evening · " + Ui.time(m));
        }));
        LinearLayout times = Ui.horizontal(this);
        times.addView(morning, Ui.weight(1));
        View gap = new View(this);
        times.addView(gap, new LinearLayout.LayoutParams(Ui.dp(this, 8), 1));
        times.addView(evening, Ui.weight(1));
        Ui.add(s, times, 4);
        Ui.add(content, s, 16);

        TextView reset = Ui.button(this, "Reset all progress", false);
        reset.setTextColor(Ui.RED);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Reset all progress?")
                .setMessage("This erases every logged habit, number and note. Settings are kept. This can't be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (d, w) -> { store.resetProgress(); toast("Progress cleared"); render(); })
                .show());
        Ui.add(content, reset, 0);
    }

    private void bullet(LinearLayout parent, String s) {
        TextView t = Ui.text(this, "•  " + s, 13, Ui.WHITE, false);
        t.setLineSpacing(0, 1.15f);
        Ui.pad(t, this, 0, 3, 0, 3);
        parent.addView(t);
    }

    private Switch settingSwitch(LinearLayout parent, String label, boolean on) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextColor(Ui.WHITE);
        sw.setTextSize(14);
        sw.setChecked(on);
        Ui.pad(sw, this, 0, 6, 0, 6);
        Ui.add(parent, sw, 8);
        return sw;
    }

    private interface MinutePicked { void on(int minutes); }

    private void pickTime(int current, MinutePicked cb) {
        new TimePickerDialog(this, (view, h, m) -> {
            cb.on(h * 60 + m);
            Reminders.scheduleAll(this);
        }, current / 60, current % 60, false).show();
    }

    private void askForNotifications() {
        if (Build.VERSION.SDK_INT < 33 || !store.remindersOn()) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        if (store.askedNotifications() && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) return;
        store.setAskedNotifications();
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

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
