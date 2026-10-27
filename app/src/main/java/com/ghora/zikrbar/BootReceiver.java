package com.ghora.zikrbar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * After a restart or an app update: turns the dhikr bar back on and sets the daily adhkar alarms again.
 * Also the "watchdog": a repeating alarm that starts the dhikr service again if the phone's memory cleaner
 * killed it, so the unlock dhikr keeps coming and does not silently stop.
 */
public class BootReceiver extends BroadcastReceiver {
    static final String ACTION_WATCHDOG = "com.ghora.zikrbar.WATCHDOG";

    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (ACTION_WATCHDOG.equals(a)) {
            ensureRunning(context);
            return;
        }
        AdhkarAlarm.scheduleAll(context);
        PrayerAlarm.schedule(context);
        LearnAlarm.schedule(context);
        if ((Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) && Prefs.running(context)) {
            ensureRunning(context);
            scheduleWatchdog(context);
        }
    }

    private static void ensureRunning(Context context) {
        if (!Prefs.running(context) || ZikrService.live != null) return;
        try {
            Ui.startZikrService(context, new Intent(context, ZikrService.class));
        } catch (RuntimeException ignored) {
            // Android may refuse to start a foreground service from the background; the app restarts it when opened.
        }
    }

    private static PendingIntent pending(Context c, int code) {
        return PendingIntent.getBroadcast(c, code, new Intent(c, BootReceiver.class).setAction(ACTION_WATCHDOG),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Checks every ~10 minutes that the dhikr service is still alive. */
    static void scheduleWatchdog(Context c) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            if (am == null) return;
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10 * 60_000L,
                    10 * 60_000L, pending(c, 77));
        } catch (RuntimeException ignored) {
        }
    }

    /** Starts the service again shortly (after the app was swiped away or the service was destroyed). */
    static void restartSoon(Context c, long ms) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            if (am == null) return;
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ms, pending(c, 78));
        } catch (RuntimeException ignored) {
        }
    }
}
