package dev.ayaz.november30;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Posts reminders and re-arms alarms after reboot or time changes. */
public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        int kind = intent.getIntExtra(Reminders.EXTRA_KIND, 0);
        if (kind == 0) { // boot / time change
            Reminders.scheduleAll(c);
            return;
        }
        Store store = new Store(c);
        if (store.remindersOn() && Challenge.isActiveToday()) post(c, store, kind);
        Reminders.scheduleAll(c);
    }

    private void post(Context c, Store store, int kind) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (!nm.areNotificationsEnabled()) return;
        Reminders.createChannel(c);
        int day = Challenge.todayIndex();
        String title, body;
        if (kind == Reminders.MORNING) {
            title = "Day " + day + " / 30 — dum hai?";
            body = "Phone down for the first hour. Write your 3 goals and get your steps in.";
        } else {
            int score = store.score(day);
            int left = Challenge.count() - score;
            title = "Evening check-in: " + score + " / " + Challenge.count();
            body = left == 0 ? "Perfect day. Log it and get 7–8 hours of sleep."
                    : left + " habits left. Log steps, expenses & gratitude before bed.";
        }
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, Reminders.CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        nm.notify(kind, n);
    }
}
