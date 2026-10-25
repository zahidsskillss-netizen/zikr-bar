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
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

/** Sends a message when each prayer time comes (the prayers the user switched on). */
public class PrayerAlarm extends BroadcastReceiver {

    static final String ACTION = "com.ghora.zikrbar.PRAYER_TIME";
    private static final String CHANNEL = "prayer_times";

    @Override
    public void onReceive(Context c, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;
        L.init(c);
        int p = intent.getIntExtra("p", -1);
        long at = intent.getLongExtra("at", 0);
        // ignore an alarm that arrives very late (phone was off)
        if (p >= 0 && PrayTimes.alertOn(c, p) && Math.abs(System.currentTimeMillis() - at) < 30 * 60_000L) show(c, p);
        schedule(c);
    }

    /** Sets one alarm for the next prayer that has its message switched on. */
    static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent base = new Intent(c, PrayerAlarm.class).setAction(ACTION);
        am.cancel(PendingIntent.getBroadcast(c, 500, base, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        long now = System.currentTimeMillis();
        for (int day = 0; day < 3; day++) {
            for (int p = 0; p < 6; p++) {
                if (!PrayTimes.alertOn(c, p)) continue;
                long t = PrayTimes.when(c, day, p);
                if (t <= now + 5000) continue;
                PendingIntent pi = PendingIntent.getBroadcast(c, 500,
                        new Intent(c, PrayerAlarm.class).setAction(ACTION).putExtra("p", p).putExtra("at", t),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
                try {
                    if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
                    else am.setWindow(AlarmManager.RTC_WAKEUP, t, 5 * 60_000L, pi);
                } catch (SecurityException e) {
                    am.setWindow(AlarmManager.RTC_WAKEUP, t, 5 * 60_000L, pi);
                }
                return;
            }
        }
    }

    private static void show(Context c, int p) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) createChannel(nm);
        Intent open = new Intent(c, PrayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 510, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String name = L.t(PrayTimes.KEYS[p]);
        String title = L.f(p == PrayTimes.SUNRISE ? "n_sunrise" : "n_prayer", name);
        int min = PrayTimes.minutes(c, java.util.Calendar.getInstance())[p];
        String body = PrayTimes.placeName(c) + " · " + PrayTimes.clock(min, true);
        Notification.Builder b = Ui.notif(c, CHANNEL, false)
                .setSmallIcon(Icon.createWithBitmap(Ui.allahIcon(c)))
                .setContentTitle(title)
                .setContentText(body)
                .setColor(0xFF1B5E20)
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (Build.VERSION.SDK_INT < 26) soundOld(b);
        nm.notify(600 + p, b.build());
    }

    @SuppressWarnings("deprecation")
    private static void soundOld(Notification.Builder b) {
        b.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        b.setPriority(Notification.PRIORITY_HIGH);
    }

    @android.annotation.TargetApi(26)
    private static void createChannel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("prayer_t"), NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }
}
