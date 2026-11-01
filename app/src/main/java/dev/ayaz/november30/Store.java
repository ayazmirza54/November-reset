package dev.ayaz.november30;

import android.content.Context;
import android.content.SharedPreferences;

/** All data lives on-device in SharedPreferences. */
final class Store {
    static final String WORKOUT = "workout", REST = "rest";

    private final SharedPreferences p;

    Store(Context c) { p = c.getSharedPreferences("november_reset", Context.MODE_PRIVATE); }

    private static String key(int day, int habit, String field) { return "d" + day + ".h" + habit + "." + field; }

    boolean done(int day, int habit) { return p.getBoolean(key(day, habit, "done"), false); }

    void setDone(int day, int habit, boolean v) { p.edit().putBoolean(key(day, habit, "done"), v).apply(); }

    /** Raw logged value, or null when nothing has been logged. */
    String value(int day, int habit) { return p.getString(key(day, habit, "value"), null); }

    void setValue(int day, int habit, String v) {
        if (v == null || v.trim().isEmpty()) p.edit().remove(key(day, habit, "value")).apply();
        else p.edit().putString(key(day, habit, "value"), v).apply();
    }

    double number(int day, int habit) {
        String v = value(day, habit);
        if (v == null) return 0;
        try { return Double.parseDouble(v.trim()); } catch (NumberFormatException e) { return 0; }
    }

    boolean hasNumber(int day, int habit) {
        String v = value(day, habit);
        if (v == null) return false;
        try { Double.parseDouble(v.trim()); return true; } catch (NumberFormatException e) { return false; }
    }

    int score(int day) {
        int s = 0;
        for (Habit h : Challenge.HABITS) if (done(day, h.id)) s++;
        return s;
    }

    int restDaysInWeek(int day, int excludingDay) {
        int n = 0;
        for (int d = Challenge.weekStart(day); d <= Challenge.weekEnd(day); d++)
            if (d != excludingDay && REST.equals(value(d, Challenge.WORKOUT))) n++;
        return n;
    }

    int workoutsInWeek(int day) {
        int n = 0;
        for (int d = Challenge.weekStart(day); d <= Challenge.weekEnd(day); d++)
            if (WORKOUT.equals(value(d, Challenge.WORKOUT))) n++;
        return n;
    }

    // ---- settings ----
    String skillName() { return p.getString("skill", "Terraform / AWS"); }
    void setSkillName(String s) { p.edit().putString("skill", s).apply(); }

    /** Accent theme: green, amber, cyan or you (wallpaper colors). */
    String accent() { return p.getString("accent", "green"); }
    void setAccent(String a) { p.edit().putString("accent", a).apply(); }

    boolean ramp() { return p.getBoolean("ramp", true); }
    void setRamp(boolean v) { p.edit().putBoolean("ramp", v).apply(); }

    boolean remindersOn() { return p.getBoolean("reminders", true); }
    void setRemindersOn(boolean v) { p.edit().putBoolean("reminders", v).apply(); }

    /** Minutes after midnight. */
    int morningReminder() { return p.getInt("morning", 7 * 60); }
    void setMorningReminder(int m) { p.edit().putInt("morning", m).apply(); }

    int eveningReminder() { return p.getInt("evening", 21 * 60 + 30); }
    void setEveningReminder(int m) { p.edit().putInt("evening", m).apply(); }

    boolean askedNotifications() { return p.getBoolean("askedNotif", false); }
    void setAskedNotifications() { p.edit().putBoolean("askedNotif", true).apply(); }

    /** Clears all logged progress but keeps settings. */
    void resetProgress() {
        SharedPreferences.Editor e = p.edit();
        for (String k : p.getAll().keySet()) if (k.startsWith("d")) e.remove(k);
        e.apply();
    }
}
