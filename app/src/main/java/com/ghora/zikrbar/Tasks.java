package com.ghora.zikrbar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

/**
 * The person's own to-do reminders ("tasks"): kept in the preferences, and each one rings once at its date and
 * time. A ringing task stays on the screen (see TaskOverlayService) until it is marked done or moved to a new time.
 */
final class Tasks {

    static final String ACTION_DUE = "com.ghora.zikrbar.TASK_DUE";
    static final String ACTION_DONE = "com.ghora.zikrbar.TASK_DONE";
    static final String ACTION_SNOOZE = "com.ghora.zikrbar.TASK_SNOOZE";
    static final String EXTRA_ID = "id";
    static final String CHANNEL = "tasks_due";
    private static final String KEY = "tasks";

    private Tasks() {}

    static final class Task {
        long id;
        String text;
        long when;
        boolean done;
    }

    // ------------------------------------------------------------------ storage

    static ArrayList<Task> all(Context c) {
        ArrayList<Task> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(Prefs.sp(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Task t = new Task();
                t.id = o.getLong("id");
                t.text = o.optString("t", "");
                t.when = o.optLong("w", 0);
                t.done = o.optBoolean("d", false);
                out.add(t);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void save(Context c, ArrayList<Task> list) {
        JSONArray a = new JSONArray();
        try {
            for (Task t : list) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("t", t.text);
                o.put("w", t.when);
                o.put("d", t.done);
                a.put(o);
            }
        } catch (Exception ignored) {
        }
        Prefs.sp(c).edit().putString(KEY, a.toString()).apply();
    }

    static Task get(Context c, long id) {
        for (Task t : all(c)) if (t.id == id) return t;
        return null;
    }

    static Task add(Context c, String text, long when) {
        ArrayList<Task> list = all(c);
        Task t = new Task();
        long id = System.currentTimeMillis();
        for (Task o : list) if (o.id >= id) id = o.id + 1;
        t.id = id;
        t.text = text;
        t.when = when;
        list.add(t);
        save(c, list);
        schedule(c, t);
        return t;
    }

    static void setDone(Context c, long id) {
        ArrayList<Task> list = all(c);
        for (Task t : list) if (t.id == id) t.done = true;
        save(c, list);
        cancelAlarm(c, id);
        cancelNotification(c, id);
        changed();
    }

    static void reschedule(Context c, long id, long when) {
        ArrayList<Task> list = all(c);
        Task hit = null;
        for (Task t : list) {
            if (t.id == id) {
                t.when = when;
                t.done = false;
                hit = t;
            }
        }
        save(c, list);
        cancelNotification(c, id);
        if (hit != null) schedule(c, hit);
        changed();
    }

    static void delete(Context c, long id) {
        ArrayList<Task> list = all(c);
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).id == id) list.remove(i);
        save(c, list);
        cancelAlarm(c, id);
        cancelNotification(c, id);
        changed();
    }

    /** Tasks not done yet, the nearest first. */
    static ArrayList<Task> pending(Context c, boolean onlyCount) {
        ArrayList<Task> out = new ArrayList<>();
        for (Task t : all(c)) if (!t.done) out.add(t);
        Collections.sort(out, (a, b) -> Long.compare(a.when, b.when));
        return out;
    }

    static int pendingCount(Context c) {
        return pending(c, true).size();
    }

    /** Tasks whose time has come and that are not done. */
    static ArrayList<Task> due(Context c) {
        ArrayList<Task> out = new ArrayList<>();
        long now = System.currentTimeMillis() + 2000;
        for (Task t : pending(c, false)) if (t.when <= now) out.add(t);
        return out;
    }

    static ArrayList<Task> finished(Context c) {
        ArrayList<Task> out = new ArrayList<>();
        for (Task t : all(c)) if (t.done) out.add(t);
        Collections.sort(out, (a, b) -> Long.compare(b.when, a.when));
        return out;
    }

    private static void changed() {
        TaskOverlayService s = TaskOverlayService.live;
        if (s != null) s.refresh();
    }

    // ------------------------------------------------------------------ alarms

    private static int code(long id) {
        return (int) (id % 1_000_000_000L);
    }

    private static PendingIntent pending(Context c, long id) {
        Intent i = new Intent(c, TaskAlarm.class).setAction(ACTION_DUE).putExtra(EXTRA_ID, id)
                .setData(Uri.parse("zikrtask://" + id));
        return PendingIntent.getBroadcast(c, code(id), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void schedule(Context c, Task t) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null || t.done) return;
        PendingIntent pi = pending(c, t.id);
        am.cancel(pi);
        long when = Math.max(t.when, System.currentTimeMillis() + 1500);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            else am.setWindow(AlarmManager.RTC_WAKEUP, when, 5 * 60_000L, pi);
        } catch (SecurityException e) {
            am.setWindow(AlarmManager.RTC_WAKEUP, when, 5 * 60_000L, pi);
        }
    }

    private static void cancelAlarm(Context c, long id) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(pending(c, id));
    }

    /** Sets every waiting alarm again (after a restart, an update, or a clock change). */
    static void scheduleAll(Context c) {
        for (Task t : all(c)) if (!t.done && t.when > System.currentTimeMillis()) schedule(c, t);
    }

    // ------------------------------------------------------------------ ringing

    /** Shows every task whose time has come: over the screen when allowed, otherwise as a notification. */
    static void ring(Context c) {
        ArrayList<Task> due = due(c);
        if (due.isEmpty()) return;
        if (Settings.canDrawOverlays(c)) {
            try {
                Intent i = new Intent(c, TaskOverlayService.class);
                if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
                return;
            } catch (RuntimeException ignored) {
            }
        }
        for (Task t : due) notifyDue(c, t);
    }

    /** Called again and again (watchdog, restart): brings back what is still waiting to be done. */
    static void resumeDue(Context c) {
        if (TaskOverlayService.live != null) return;
        ring(c);
    }

    static int nid(long id) {
        return 6000 + (int) (id % 1000);
    }

    static void notifyDue(Context c, Task t) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(nid(t.id), build(c, t));
    }

    static void cancelNotification(Context c, long id) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(nid(id));
    }

    @android.annotation.TargetApi(26)
    private static void channel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("tk_notif"), NotificationManager.IMPORTANCE_HIGH);
        ch.setShowBadge(true);
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    /** The message for a ringing task, with "done" and "+10 min" buttons. {@code t} may be null (generic message). */
    static Notification build(Context c, Task t) {
        L.init(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) channel(nm);
        Intent open = new Intent(c, TasksActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 7001, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Ui.notif(c, CHANNEL, false)
                .setSmallIcon(Icon.createWithBitmap(Ui.allahIcon(c)))
                .setContentTitle(L.t("tk_notif"))
                .setColor(0xFF1B5E20)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi);
        if (t != null) {
            b.setContentText(t.text).setStyle(new Notification.BigTextStyle().bigText(t.text));
            b.addAction(new Notification.Action.Builder(Icon.createWithBitmap(Ui.allahIcon(c)), L.t("tk_done"), action(c, ACTION_DONE, t.id)).build());
            b.addAction(new Notification.Action.Builder(Icon.createWithBitmap(Ui.allahIcon(c)), L.f("tk_plus_min", L.num(10)), action(c, ACTION_SNOOZE, t.id)).build());
        }
        return b.build();
    }

    private static PendingIntent action(Context c, String action, long id) {
        Intent i = new Intent(c, TaskAlarm.class).setAction(action).putExtra(EXTRA_ID, id)
                .setData(Uri.parse("zikrtask://" + action + "/" + id));
        return PendingIntent.getBroadcast(c, code(id) + (ACTION_DONE.equals(action) ? 1 : 2), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // ------------------------------------------------------------------ times

    static long in(int minutes) {
        return System.currentTimeMillis() + minutes * 60_000L;
    }

    static long at(int dayOffset, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, dayOffset);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Tonight at 8 (or tomorrow night if it is already later than that). */
    static long tonight() {
        long t = at(0, 20, 0);
        return t <= System.currentTimeMillis() + 60_000L ? at(1, 20, 0) : t;
    }

    static long tomorrowMorning() {
        return at(1, 8, 0);
    }

    private static int dayDiff(Calendar a, Calendar b) {
        Calendar x = (Calendar) a.clone(), y = (Calendar) b.clone();
        for (Calendar z : new Calendar[]{x, y}) {
            z.set(Calendar.HOUR_OF_DAY, 12);
            z.set(Calendar.MINUTE, 0);
            z.set(Calendar.SECOND, 0);
            z.set(Calendar.MILLISECOND, 0);
        }
        return (int) Math.round((y.getTimeInMillis() - x.getTimeInMillis()) / 86400000.0);
    }

    /** The digits of this text in the language's own digits. */
    static String nd(String s) {
        if (L.lang == L.EN) return s;
        char zero = L.lang == L.AR ? '٠' : '۰';
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append(ch >= '0' && ch <= '9' ? (char) (zero + (ch - '0')) : ch);
        return b.toString();
    }

    static Locale locale() {
        switch (L.lang) {
            case L.EN: return Locale.ENGLISH;
            case L.UR: return new Locale("ur");
            case L.AR: return new Locale("ar");
            default: return new Locale("fa");
        }
    }

    static String day(long ms) {
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ms);
        int diff = dayDiff(Calendar.getInstance(), t);
        if (diff == 0) return L.t("tk_today");
        if (diff == 1) return L.t("tk_tomorrow");
        return nd(new SimpleDateFormat("EEE d MMM", locale()).format(t.getTime()));
    }

    static String clock(long ms) {
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ms);
        return nd(String.format(Locale.US, "%02d:%02d", t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE)));
    }

    /** "Tomorrow · 08:00" */
    static String when(Context c, long ms) {
        return day(ms) + "  ·  " + clock(ms);
    }
}
