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
import android.provider.Settings;

import java.util.Calendar;

/**
 * Short on-screen announcements: Asma ul Husna every morning and evening, the prayer translation every night at
 * 9 until all of it is learned, and a reminder to switch the dhikr bar on again after it was stopped.
 */
public class AnnounceAlarm extends BroadcastReceiver {

    static final String ACTION = "com.ghora.zikrbar.ANNOUNCE";
    static final String EXTRA_KIND = "kind";
    static final int ASMA_AM = 1, ASMA_PM = 2, SALAH = 3, POWER = 4;
    static final long POWER_DELAY = 12 * 60 * 60_000L;
    private static final String CHANNEL = "announce";

    /** minutes after midnight */
    private static final int AM_AT = 7 * 60 + 30, PM_AT = 18 * 60, SALAH_AT = 21 * 60;

    @Override
    public void onReceive(Context c, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;
        L.init(c);
        int kind = intent.getIntExtra(EXTRA_KIND, 0);
        if (kind == POWER) {
            if (Prefs.running(c) || !isOn(c, POWER)) return;
            scheduleAt(c, POWER, System.currentTimeMillis() + POWER_DELAY);
            fire(c, POWER);
            return;
        }
        scheduleDaily(c, kind);
        if (isOn(c, kind) && (kind != SALAH || SalahWordsActivity.learned(c) < SalahWordsActivity.total())) fire(c, kind);
    }

    // ------------------------------------------------------------------ switches (each one lives in its own section)

    private static String key(int kind) {
        return kind == SALAH ? "annSalah" : kind == POWER ? "annPower" : "annAsma";
    }

    static boolean isOn(Context c, int kind) {
        return Prefs.sp(c).getBoolean(key(kind), true);
    }

    static void setOn(Context c, int kind, boolean on) {
        Prefs.sp(c).edit().putBoolean(key(kind), on).apply();
        if (kind == ASMA_AM || kind == ASMA_PM) {
            Prefs.sp(c).edit().putBoolean(key(ASMA_AM), on).apply();
        }
        scheduleAll(c);
    }

    // ------------------------------------------------------------------ alarms

    static void scheduleAll(Context c) {
        scheduleDaily(c, ASMA_AM);
        scheduleDaily(c, ASMA_PM);
        scheduleDaily(c, SALAH);
    }

    /** Called when the person stops the bar by hand: ask again after half a day if it is still off. */
    static void barStopped(Context c) {
        Prefs.sp(c).edit().putBoolean("barOffByHand", true).apply();
        if (isOn(c, POWER)) scheduleAt(c, POWER, System.currentTimeMillis() + POWER_DELAY);
    }

    static void barStarted(Context c) {
        Prefs.sp(c).edit().putBoolean("barOffByHand", false).apply();
        cancel(c, POWER);
        AnnounceService s = AnnounceService.live;
        if (s != null) s.dismissKind(POWER);
    }

    private static PendingIntent pending(Context c, int kind) {
        return PendingIntent.getBroadcast(c, 900 + kind,
                new Intent(c, AnnounceAlarm.class).setAction(ACTION).putExtra(EXTRA_KIND, kind),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void cancel(Context c, int kind) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(pending(c, kind));
    }

    private static void scheduleDaily(Context c, int kind) {
        cancel(c, kind);
        if (!isOn(c, kind)) return;
        int m = kind == ASMA_AM ? AM_AT : kind == ASMA_PM ? PM_AT : SALAH_AT;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, m / 60);
        cal.set(Calendar.MINUTE, m % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 5000) cal.add(Calendar.DAY_OF_YEAR, 1);
        scheduleAt(c, kind, cal.getTimeInMillis());
    }

    private static void scheduleAt(Context c, int kind, long when) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = pending(c, kind);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setWindow(AlarmManager.RTC_WAKEUP, when, 10 * 60_000L, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, when, 10 * 60_000L, pi);
            }
        } catch (RuntimeException e) {
            am.set(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    // ------------------------------------------------------------------ showing

    static void fire(Context c, int kind) {
        if (Settings.canDrawOverlays(c)) {
            try {
                Intent i = new Intent(c, AnnounceService.class).putExtra(EXTRA_KIND, kind);
                if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
                return;
            } catch (RuntimeException ignored) {
            }
        }
        notifyOnly(c, kind);
    }

    static String title(int kind) {
        return L.t(kind == SALAH ? "an_salah_t" : kind == POWER ? "an_power_t" : "an_asma_t");
    }

    static String body(Context c, int kind) {
        if (kind == SALAH) {
            return L.f("an_salah_b", L.num(SalahWordsActivity.learned(c)), L.num(SalahWordsActivity.total()));
        }
        return L.t(kind == POWER ? "an_power_b" : kind == ASMA_AM ? "an_asma_am" : "an_asma_pm");
    }

    /** What the main button of the announcement does. */
    static Intent target(Context c, int kind) {
        Intent i;
        if (kind == SALAH) i = new Intent(c, SalahWordsActivity.class);
        else if (kind == POWER) i = new Intent(c, MainActivity.class).putExtra("power_on", true);
        else i = new Intent(c, AdhkarActivity.class).putExtra(AdhkarActivity.EXTRA_TAB, AdhkarData.NAMES);
        return i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    static void notifyOnly(Context c, int kind) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        ensureChannel(nm);
        PendingIntent pi = PendingIntent.getActivity(c, 950 + kind, target(c, kind),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = Ui.notif(c, CHANNEL, false)
                .setSmallIcon(Icon.createWithBitmap(Ui.allahIcon(c)))
                .setContentTitle(title(kind))
                .setContentText(body(c, kind))
                .setStyle(new Notification.BigTextStyle().bigText(body(c, kind)))
                .setColor(0xFF1B5E20)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        nm.notify(960 + kind, n);
    }

    @android.annotation.SuppressLint("NewApi")
    static void ensureChannel(NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, L.t("an_channel"), NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    /** A small "show this announcement" switch row for the section the announcement belongs to. */
    static android.view.View toggleRow(Context c, int kind, int textColor) {
        android.widget.LinearLayout r = new android.widget.LinearLayout(c);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        float d = c.getResources().getDisplayMetrics().density;
        r.setPadding(Math.round(14 * d), Math.round(10 * d), Math.round(14 * d), Math.round(10 * d));
        android.widget.TextView t = new android.widget.TextView(c);
        t.setText(L.t(kind == SALAH ? "an_set_salah" : kind == POWER ? "an_set_power" : "an_set_asma"));
        t.setTextColor(textColor);
        t.setTextSize(14.5f);
        r.addView(t, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        android.widget.Switch sw = new android.widget.Switch(c);
        sw.setChecked(isOn(c, kind));
        sw.setOnCheckedChangeListener((b, on) -> setOn(c, kind, on));
        r.addView(sw);
        return r;
    }
}
