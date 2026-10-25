package com.ghora.zikrbar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;

import java.util.Calendar;

/** Sends the morning and evening adhkar message every day at the chosen time. */
public class AdhkarAlarm extends BroadcastReceiver {

    static final String ACTION = "com.ghora.zikrbar.ADHKAR_TIME";
    private static final String CHANNEL = "adhkar_daily";
    static final int[] CATS = {AdhkarData.MORNING, AdhkarData.EVENING, AdhkarData.SLEEP};

    @Override
    public void onReceive(Context c, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;
        int cat = intent.getIntExtra("cat", AdhkarData.MORNING);
        L.init(c);
        boolean snoozed = intent.getBooleanExtra("snooze", false);
        if ((snoozed || AdhkarData.remindOn(c, cat)) && !AdhkarData.allDone(c, cat)) show(c, cat);
        if (!snoozed) schedule(c, cat);
    }

    /** Sets (or clears) both daily alarms. Safe to call often. */
    static void scheduleAll(Context c) {
        for (int cat : CATS) schedule(c, cat);
    }

    static void schedule(Context c, int cat) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = pending(c, cat);
        am.cancel(pi);
        if (!AdhkarData.remindOn(c, cat)) return;

        int at = AdhkarData.remindAt(c, cat);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, at / 60);
        cal.set(Calendar.MINUTE, at % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1);
        long when = cal.getTimeInMillis();

        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, when, 10 * 60_000L, pi);
            }
        } catch (SecurityException e) {
            am.setWindow(AlarmManager.RTC_WAKEUP, when, 10 * 60_000L, pi);
        }
    }

    /** Reminds again after some minutes (the "later" button on the sleep page). */
    static void snooze(Context c, int cat, int minutes) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent i = new Intent(c, AdhkarAlarm.class).setAction(ACTION).putExtra("cat", cat).putExtra("snooze", true);
        PendingIntent pi = PendingIntent.getBroadcast(c, 330 + cat, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        long when = System.currentTimeMillis() + minutes * 60_000L;
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            else am.setWindow(AlarmManager.RTC_WAKEUP, when, 5 * 60_000L, pi);
        } catch (SecurityException e) {
            am.setWindow(AlarmManager.RTC_WAKEUP, when, 5 * 60_000L, pi);
        }
    }

    private static PendingIntent pending(Context c, int cat) {
        Intent i = new Intent(c, AdhkarAlarm.class).setAction(ACTION).putExtra("cat", cat);
        return PendingIntent.getBroadcast(c, 300 + cat, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void show(Context c, int cat) {
        if (AdhkarData.bubbleOn(c) && BubbleService.show(c, cat)) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) createChannel(nm);
        Intent open = BubbleService.target(c, cat);
        PendingIntent pi = PendingIntent.getActivity(c, 310 + cat, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = BubbleService.title(cat);
        String body = BubbleService.body(cat);
        Notification n = Ui.notif(c, CHANNEL, false)
                .setSmallIcon(Icon.createWithBitmap(Ui.allahIcon(c)))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(0xFF1B5E20)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        nm.notify(400 + cat, n);
    }

    @android.annotation.TargetApi(26)
    private static void createChannel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("rem_daily_t"), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setShowBadge(true);
        nm.createNotificationChannel(ch);
    }
}
