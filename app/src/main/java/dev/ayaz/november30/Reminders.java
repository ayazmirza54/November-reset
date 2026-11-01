package dev.ayaz.november30;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

/** Schedules the morning and evening reminders (inexact alarms, no special permission). */
final class Reminders {
    private Reminders() {}

    static final String CHANNEL = "daily_reminders";
    static final int MORNING = 1, EVENING = 2;
    static final String EXTRA_KIND = "kind";

    static void createChannel(Context c) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Daily reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Morning plan and evening check-in for the November challenge");
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    static void scheduleAll(Context c) {
        Store s = new Store(c);
        cancel(c, MORNING);
        cancel(c, EVENING);
        if (!s.remindersOn()) return;
        schedule(c, MORNING, s.morningReminder());
        schedule(c, EVENING, s.eveningReminder());
    }

    static void schedule(Context c, int kind, int minutesOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, minutesOfDay / 60);
        cal.set(Calendar.MINUTE, minutesOfDay % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.setWindow(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 10 * 60 * 1000L, pending(c, kind));
    }

    private static void cancel(Context c, int kind) {
        c.getSystemService(AlarmManager.class).cancel(pending(c, kind));
    }

    private static PendingIntent pending(Context c, int kind) {
        Intent i = new Intent(c, ReminderReceiver.class).putExtra(EXTRA_KIND, kind);
        return PendingIntent.getBroadcast(c, kind, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
