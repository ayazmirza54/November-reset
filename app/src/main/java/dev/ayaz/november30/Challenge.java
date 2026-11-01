package dev.ayaz.november30;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Fixed rules of the November 2026 challenge. */
final class Challenge {
    private Challenge() {}

    static final LocalDate START = LocalDate.of(2026, 11, 1);
    static final int DAYS = 30;
    /** A "strong day" is 10 of 13 habits, roughly the 80% success line. */
    static final int STRONG_DAY = 10;
    static final int WORKOUTS_PER_WEEK = 4;

    static final int STEPS = 0, SLEEP = 1, WORKOUT = 2, JUNK = 3, WAKE_PHONE = 4, READ = 5,
            EXPENSE = 6, SKILL = 7, MEAL_PHONE = 8, MEDITATE = 9, CLEAN = 10, GOALS = 11, GRATITUDE = 12;

    static final Habit[] HABITS = {
            Habit.number(STEPS, "🚶", "10K walk", "Log today's step count", "steps", false),
            Habit.number(SLEEP, "🌙", "7–8 hrs of sleep", "Hours slept last night (7+)", "hrs", true),
            new Habit(WORKOUT, "💪", "Workout 4–5× a week", "Train today, or take a planned rest day", Habit.Kind.WORKOUT, null, false),
            Habit.check(JUNK, "🥗", "Avoid junk food", "No fried food, sweets, chips or sugary drinks"),
            Habit.check(WAKE_PHONE, "📵", "No phone after waking", "First 60 minutes of the day are phone-free"),
            Habit.number(READ, "📖", "Read 10 pages", "Pages read today", "pages", false),
            Habit.number(EXPENSE, "💸", "Track daily expenses", "Log today's total spend (0 counts)", "₹", false),
            Habit.number(SKILL, "🧠", "Master a skill", "Focused practice minutes", "min", false),
            Habit.check(MEAL_PHONE, "🍽️", "No phone during meals", "Every meal today, screen-free"),
            Habit.number(MEDITATE, "🧘", "Meditate", "Minutes of meditation", "min", false),
            Habit.check(CLEAN, "🧹", "Clean your workspace", "10-minute reset of desk & room"),
            Habit.text(GOALS, "🎯", "Write your daily goals", "Your top 3 goals for today"),
            Habit.text(GRATITUDE, "🙏", "Show gratitude", "3 things you're grateful for"),
    };

    static int count() { return HABITS.length; }

    static int maxPoints() { return DAYS * HABITS.length; }

    static LocalDate date(int day) { return START.plusDays(day - 1); }

    /** Challenge day for today, clamped to 1..30. */
    static int todayIndex() {
        long d = ChronoUnit.DAYS.between(START, LocalDate.now()) + 1;
        return (int) Math.max(1, Math.min(DAYS, d));
    }

    /** Number of challenge days that have started (0 before Nov 1, 30 after Nov 30). */
    static int elapsedDays() {
        long d = ChronoUnit.DAYS.between(START, LocalDate.now()) + 1;
        return (int) Math.max(0, Math.min(DAYS, d));
    }

    static boolean isFuture(int day) { return date(day).isAfter(LocalDate.now()); }

    static boolean isActiveToday() {
        LocalDate now = LocalDate.now();
        return !now.isBefore(START) && !now.isAfter(date(DAYS));
    }

    static int week(int day) { return (day - 1) / 7 + 1; }

    static int weekStart(int day) { return (week(day) - 1) * 7 + 1; }

    static int weekEnd(int day) { return Math.min(DAYS, weekStart(day) + 6); }

    static int maxRestDays(int day) { return weekEnd(day) - weekStart(day) + 1 >= 7 ? 3 : 1; }

    /** Numeric target for NUMBER habits. Ramp-up mode eases the first two weeks. */
    static double target(int habit, int day, boolean ramp) {
        int w = week(day);
        switch (habit) {
            case STEPS: return !ramp || w >= 3 ? 10000 : (w == 1 ? 7000 : 8500);
            case SLEEP: return 7;
            case READ: return 10;
            case EXPENSE: return 0;
            case SKILL: return !ramp || w >= 2 ? 45 : 30;
            case MEDITATE: return !ramp || w >= 2 ? 15 : 10;
            default: return 0;
        }
    }

    static String phase(int day) {
        switch (week(day)) {
            case 1: return "Week 1 · Foundation — show up, keep it easy";
            case 2: return "Week 2 · Build — raise the bar";
            case 3: return "Week 3 · Push — full targets, no excuses";
            case 4: return "Week 4 · Lock in — make it automatic";
            default: return "Final days · Finish strong";
        }
    }
}
