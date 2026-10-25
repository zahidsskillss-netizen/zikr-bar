package com.ghora.zikrbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After a restart or an app update: turns the dhikr bar back on and sets the daily adhkar alarms again. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        AdhkarAlarm.scheduleAll(context);
        PrayerAlarm.schedule(context);
        LearnAlarm.schedule(context);
        if ((Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) && Prefs.running(context)) {
            try {
                Ui.startZikrService(context, new Intent(context, ZikrService.class));
            } catch (RuntimeException ignored) {
                // Android may refuse to start a foreground service from this broadcast; the app restarts it when opened.
            }
        }
    }
}
