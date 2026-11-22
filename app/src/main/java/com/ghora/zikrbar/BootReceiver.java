package com.ghora.zikrbar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
        L.init(context);
        if (ACTION_WATCHDOG.equals(a)) {
            if (ZikrService.live == null && Prefs.running(context)) Prefs.log(context, "watchdog: service was dead, starting");
            ensureRunning(context);
            PopupAlarm.schedule(context);
            if (Prefs.running(context)) scheduleWatchdog(context);
            Tasks.resumeDue(context);
            return;
        }
        Prefs.log(context, "system: " + (a == null ? "?" : a.substring(a.lastIndexOf('.') + 1)));
        AdhkarAlarm.scheduleAll(context);
        PrayerAlarm.schedule(context);
        LearnAlarm.schedule(context);
        AnnounceAlarm.scheduleAll(context);
        if (!Prefs.running(context) && AnnounceAlarm.isOn(context, AnnounceAlarm.POWER) && Prefs.sp(context).getBoolean("barOffByHand", false)) AnnounceAlarm.barStopped(context);
        Tasks.scheduleAll(context);
        PopupAlarm.schedule(context);
        Tasks.resumeDue(context);
        if ((Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) && Prefs.running(context)) {
            ensureRunning(context);
            scheduleWatchdog(context);
        }
    }

    private static void ensureRunning(Context context) {
        if (!Prefs.running(context) || ZikrService.live != null) return;
        try {
            Ui.startZikrService(context, new Intent(context, ZikrService.class));
        } catch (RuntimeException e) {
            Prefs.sp(context).edit().putString("dgErr", "restart: " + e.getClass().getSimpleName()).apply();
            Prefs.log(context, "restart FAILED " + e.getClass().getSimpleName());
            // Android may refuse to start a foreground service from the background; the app restarts it when opened.
        }
    }

    private static PendingIntent pending(Context c, int code) {
        return PendingIntent.getBroadcast(c, code, new Intent(c, BootReceiver.class).setAction(ACTION_WATCHDOG),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Checks every 10 minutes that the dhikr service is still alive. */
    static void scheduleWatchdog(Context c) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            if (am == null) return;
            // a one-shot exact alarm, set again each time: an inexact repeating one may not start the service (Android 12+)
            long at = SystemClock.elapsedRealtime() + 10 * 60_000L;
            PendingIntent pi = pending(c, 77);
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
            }
        } catch (RuntimeException ignored) {
        }
    }

    /** Starts the service again shortly (after the app was swiped away or the service was destroyed). */
    static void restartSoon(Context c, long ms) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            if (am == null) return;
            // only an exact alarm may start a foreground service from the background (Android 12+)
            long at = SystemClock.elapsedRealtime() + ms;
            PendingIntent pi = pending(c, 78);
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
            }
        } catch (RuntimeException ignored) {
        }
    }
}
