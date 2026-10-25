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

/**
 * Sends the message for each prayer the user switched on, at the prayer time or a few minutes
 * before/after it, with the chosen alert: vibration, silent, phone sound, long beep or own sound.
 */
public class PrayerAlarm extends BroadcastReceiver {

    static final String ACTION = "com.ghora.zikrbar.PRAYER_TIME";
    static final String CH_SOUND = "prayer_times", CH_VIB = "prayer_vibrate", CH_SILENT = "prayer_silent";

    @Override
    public void onReceive(Context c, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;
        L.init(c);
        int p = intent.getIntExtra("p", -1);
        long at = intent.getLongExtra("at", 0);
        // ignore an alarm that arrives very late (phone was off)
        if (p >= 0 && PrayTimes.alertOn(c, p) && Math.abs(System.currentTimeMillis() - at) < 30 * 60_000L) fire(c, p);
        schedule(c);
    }

    /** Moment of the message for this prayer on that day: prayer time plus the chosen offset. */
    static long alertTime(Context c, int day, int p) {
        return PrayTimes.when(c, day, p) + PrayTimes.alertOffset(c, p) * 60_000L;
    }

    /** Sets one alarm for the next prayer that has its message switched on. */
    static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent base = new Intent(c, PrayerAlarm.class).setAction(ACTION);
        am.cancel(PendingIntent.getBroadcast(c, 500, base, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        long now = System.currentTimeMillis();
        long best = Long.MAX_VALUE;
        int bestP = -1;
        for (int day = 0; day < 3; day++) {
            for (int p = 0; p < 6; p++) {
                if (!PrayTimes.alertOn(c, p)) continue;
                long t = alertTime(c, day, p);
                if (t > now + 5000 && t < best) {
                    best = t;
                    bestP = p;
                }
            }
        }
        if (bestP < 0) return;
        PendingIntent pi = PendingIntent.getBroadcast(c, 500,
                new Intent(c, PrayerAlarm.class).setAction(ACTION).putExtra("p", bestP).putExtra("at", best),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
            else am.setWindow(AlarmManager.RTC_WAKEUP, best, 5 * 60_000L, pi);
        } catch (SecurityException e) {
            am.setWindow(AlarmManager.RTC_WAKEUP, best, 5 * 60_000L, pi);
        }
    }

    /** The next time a message will come for this prayer (for the settings page). */
    static long nextFor(Context c, int p) {
        long now = System.currentTimeMillis();
        for (int day = 0; day < 3; day++) {
            long t = alertTime(c, day, p);
            if (t > now) return t;
        }
        return 0;
    }

    static String title(Context c, int p) {
        String name = L.t(PrayTimes.KEYS[p]);
        int off = PrayTimes.alertOffset(c, p);
        if (off < 0) return L.f("n_before", L.num(-off), name);
        return L.f(p == PrayTimes.SUNRISE ? "n_sunrise" : "n_prayer", name);
    }

    private static void fire(Context c, int p) {
        int mode = PrayTimes.alertMode(c, p);
        if (mode == PrayTimes.A_BEEP || mode == PrayTimes.A_CUSTOM) {
            try {
                Intent s = new Intent(c, AdhanService.class).putExtra("p", p);
                if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s);
                else c.startService(s);
                return;
            } catch (RuntimeException e) {
                mode = PrayTimes.A_DEFAULT; // Android refused to start playback; fall back to a normal message
            }
        }
        show(c, p, mode);
    }

    static void show(Context c, int p, int mode) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) createChannels(nm);
        String ch = mode == PrayTimes.A_VIBRATE ? CH_VIB : mode == PrayTimes.A_SILENT ? CH_SILENT : CH_SOUND;
        int min = PrayTimes.minutes(c, java.util.Calendar.getInstance())[p];
        String body = PrayTimes.placeName(c) + " · " + PrayTimes.clock(min, true);
        Notification.Builder b = Ui.notif(c, ch, mode == PrayTimes.A_SILENT)
                .setSmallIcon(Icon.createWithBitmap(Ui.allahIcon(c)))
                .setContentTitle(title(c, p))
                .setContentText(body)
                .setColor(0xFF1B5E20)
                .setAutoCancel(true)
                .setContentIntent(openPage(c));
        if (Build.VERSION.SDK_INT < 26) oldAlert(b, mode);
        nm.notify(600 + p, b.build());
    }

    static PendingIntent openPage(Context c) {
        Intent open = new Intent(c, PrayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 510, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @SuppressWarnings("deprecation")
    private static void oldAlert(Notification.Builder b, int mode) {
        if (mode == PrayTimes.A_VIBRATE) {
            b.setVibrate(new long[]{0, 400, 200, 400});
            b.setPriority(Notification.PRIORITY_HIGH);
        } else if (mode == PrayTimes.A_DEFAULT) {
            b.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            b.setPriority(Notification.PRIORITY_HIGH);
        }
    }

    @android.annotation.TargetApi(26)
    static void createChannels(NotificationManager nm) {
        NotificationChannel snd = new NotificationChannel(CH_SOUND, L.t("prayer_t"), NotificationManager.IMPORTANCE_HIGH);
        snd.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        snd.enableVibration(true);
        nm.createNotificationChannel(snd);
        NotificationChannel vib = new NotificationChannel(CH_VIB, L.t("pa_vibrate"), NotificationManager.IMPORTANCE_HIGH);
        vib.setSound(null, null);
        vib.enableVibration(true);
        vib.setVibrationPattern(new long[]{0, 400, 200, 400});
        nm.createNotificationChannel(vib);
        NotificationChannel sil = new NotificationChannel(CH_SILENT, L.t("pa_silent"), NotificationManager.IMPORTANCE_LOW);
        sil.setSound(null, null);
        sil.enableVibration(false);
        nm.createNotificationChannel(sil);
    }
}
