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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Reminders, at the times the learner picked, to practise the word-by-word prayer translation. */
public class LearnAlarm extends BroadcastReceiver {

    static final String ACTION = "com.ghora.zikrbar.LEARN_TIME";
    private static final String CHANNEL = "learn_salah";

    @Override
    public void onReceive(Context c, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;
        L.init(c);
        if (on(c)) show(c);
        schedule(c);
    }

    static boolean on(Context c) {
        return SalahWordsActivity.sp(c).getBoolean("remind", false);
    }

    static void setOn(Context c, boolean v) {
        SalahWordsActivity.sp(c).edit().putBoolean("remind", v).apply();
    }

    /** Reminder times as minutes after midnight, sorted. */
    static List<Integer> times(Context c) {
        List<Integer> out = new ArrayList<>();
        String raw = SalahWordsActivity.sp(c).getString("times", "");
        for (String p : raw.split(",")) {
            try {
                if (!p.trim().isEmpty()) out.add(Integer.parseInt(p.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    static void setTimes(Context c, List<Integer> t) {
        StringBuilder b = new StringBuilder();
        for (int m : t) {
            if (b.length() > 0) b.append(',');
            b.append(m);
        }
        SalahWordsActivity.sp(c).edit().putString("times", b.toString()).apply();
    }

    static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(c, 800, new Intent(c, LearnAlarm.class).setAction(ACTION),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
        if (!on(c)) return;
        List<Integer> t = times(c);
        if (t.isEmpty()) return;
        long now = System.currentTimeMillis(), best = Long.MAX_VALUE;
        for (int day = 0; day < 2; day++) {
            for (int m : t) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, day);
                cal.set(Calendar.HOUR_OF_DAY, m / 60);
                cal.set(Calendar.MINUTE, m % 60);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long w = cal.getTimeInMillis();
                if (w > now + 5000 && w < best) best = w;
            }
        }
        if (best == Long.MAX_VALUE) return;
        am.setWindow(AlarmManager.RTC_WAKEUP, best, 10 * 60_000L, pi);
    }

    private static void show(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) createChannel(nm);
        int[] w = SalahWordsActivity.nextWord(c);
        String body;
        int section = 0;
        if (w == null) {
            body = L.t("sw_all_learned");
        } else {
            section = w[0];
            String[] s = SalahWordsActivity.SECTIONS[w[0]];
            body = s[w[1]] + "  =  " + s[w[1] + 1];
        }
        Intent open = new Intent(c, SalahWordsActivity.class).putExtra(SalahWordsActivity.EXTRA_SECTION, section)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 810, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = Ui.notif(c, CHANNEL, false)
                .setSmallIcon(Icon.createWithBitmap(Ui.allahIcon(c)))
                .setContentTitle(L.f("sw_n_title", L.num(SalahWordsActivity.learned(c)), L.num(SalahWordsActivity.total())))
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(0xFF1B5E20)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        nm.notify(820, n);
    }

    @android.annotation.TargetApi(26)
    private static void createChannel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("sw_remind"), NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(ch);
    }
}
