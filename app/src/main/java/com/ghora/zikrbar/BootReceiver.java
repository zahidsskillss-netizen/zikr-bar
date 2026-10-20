package com.ghora.zikrbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Turns the dhikr bar back on after the phone restarts, if it was on before. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && Prefs.running(context)) {
            context.startForegroundService(new Intent(context, ZikrService.class));
        }
    }
}
