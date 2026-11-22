package com.ghora.zikrbar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * The heartbeat of the dhikr bar. Phones such as Infinix/Tecno (XOS), Xiaomi or Oppo freeze or kill an app's
 * background timers, so a plain Handler is not enough. An alarm is delivered even then: every minute it wakes
 * the app, restarts the dhikr service if the phone killed it, and shows the periodic popup when it is due.
 */
public class PopupAlarm extends BroadcastReceiver {

    static final String ACTION = "com.ghora.zikrbar.POPUP_TICK";
    private static final int CODE = 9100;

    @Override
    public void onReceive(Context c, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;
        L.init(c);
        Prefs.stamp(c, "dgAlarm");
        Prefs.log(c, "alarm run=" + Prefs.running(c) + " svc=" + (ZikrService.live != null) + " pop=" + Prefs.popupOn(c));
        if (Prefs.running(c)) {
            ZikrService s = ZikrService.live;
            if (s == null) {
                try {
                    Ui.startZikrService(c, new Intent(c, ZikrService.class).setAction(ZikrService.ACTION_POPUP_NOW));
                    Prefs.stamp(c, "dgRestart");
                } catch (RuntimeException e) {
                    Prefs.sp(c).edit().putString("dgErr", "start: " + e.getClass().getSimpleName()).apply();
                }
            } else {
                s.popupFromAlarm();
            }
        }
        schedule(c);
    }

    private static PendingIntent pending(Context c) {
        return PendingIntent.getBroadcast(c, CODE, new Intent(c, PopupAlarm.class).setAction(ACTION),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Sets the next beat (1 min when the popup is on, 5 min just to keep the service alive), or stops it. */
    static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = pending(c);
        if (!Prefs.running(c)) {
            am.cancel(pi);
            return;
        }
        long when = System.currentTimeMillis() + (Prefs.popupOn(c) ? 60_000L : 5 * 60_000L);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
            }
        } catch (RuntimeException e) {
            am.set(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }
}
