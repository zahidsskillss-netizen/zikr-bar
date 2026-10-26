package com.ghora.zikrbar;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Stores the dhikr list, status bar settings and floating tasbih settings. */
public final class Prefs {

    static final String DEFAULT_LIST =
            "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّد\n"
            + "فَاذْكُرُونِي أَذْكُرْكُمْ\n"
            + "سُبْحَانَ اللَّهِ\n"
            + "الْحَمْدُ لِلَّهِ\n"
            + "لَا إِلَهَ إِلَّا اللَّهُ\n"
            + "اللَّهُ أَكْبَرُ\n"
            + "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ\n"
            + "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ\n"
            + "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ\n"
            + "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ\n"
            + "لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ\n"
            + "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ\n"
            + "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا\n"
            + "سُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ وَلَا إِلَهَ إِلَّا اللَّهُ وَاللَّهُ أَكْبَرُ\n"
            + "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ";

    static final int[] INTERVALS = {1, 5, 15, 30, 60};
    static final int[] TARGETS = {33, 99, 100, 1000, 0};

    private Prefs() {}

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("zikr", Context.MODE_PRIVATE);
    }

    // ---- dhikr list ----
    static String rawList(Context c) {
        return sp(c).getString("list", DEFAULT_LIST);
    }

    static void saveList(Context c, String raw) {
        sp(c).edit().putString("list", raw).putInt("index", 0).putInt("tIndex", 0).apply();
    }

    /** Saves the rows from the list editor (order kept; the current positions stay where they are). */
    static void setList(Context c, List<String> rows) {
        StringBuilder b = new StringBuilder();
        for (String r : rows) {
            String t = r.replace('\n', ' ').trim();
            if (t.isEmpty()) continue;
            if (b.length() > 0) b.append('\n');
            b.append(t);
        }
        sp(c).edit().putString("list", b.toString()).apply();
    }

    static List<String> list(Context c) {
        List<String> out = new ArrayList<>();
        for (String line : rawList(c).split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        if (out.isEmpty()) out.add("سُبْحَانَ اللَّهِ");
        return out;
    }

    // ---- status bar dhikr ----
    static String current(Context c) {
        List<String> l = list(c);
        return l.get(Math.floorMod(sp(c).getInt("index", 0), l.size()));
    }

    static void next(Context c) {
        int size = list(c).size();
        sp(c).edit().putInt("index", (sp(c).getInt("index", 0) + 1) % size).apply();
    }

    static int intervalMinutes(Context c) {
        return sp(c).getInt("interval", 5);
    }

    static void setIntervalMinutes(Context c, int m) {
        sp(c).edit().putInt("interval", m).apply();
    }

    static long intervalMs(Context c) {
        return intervalMinutes(c) * 60_000L;
    }

    static boolean running(Context c) {
        return sp(c).getBoolean("running", false);
    }

    static void setRunning(Context c, boolean r) {
        sp(c).edit().putBoolean("running", r).apply();
    }

    // ---- floating tasbih ----
    static boolean overlay(Context c) {
        return sp(c).getBoolean("overlay", false);
    }

    static void setOverlay(Context c, boolean on) {
        sp(c).edit().putBoolean("overlay", on).apply();
    }

    static int tasbihStyle(Context c) {
        return TasbihView.valid(sp(c).getInt("tStyle", 6));
    }

    static void setTasbihStyle(Context c, int s) {
        sp(c).edit().putInt("tStyle", s).apply();
    }

    static int tasbihIndex(Context c) {
        return Math.floorMod(sp(c).getInt("tIndex", 2), list(c).size());
    }

    static void setTasbihIndex(Context c, int i) {
        sp(c).edit().putInt("tIndex", i).apply();
    }

    static String tasbihText(Context c) {
        return list(c).get(tasbihIndex(c));
    }

    static int target(Context c) {
        return sp(c).getInt("tTarget", 33);
    }

    static void setTarget(Context c, int t) {
        sp(c).edit().putInt("tTarget", t).apply();
    }

    static int count(Context c) {
        return sp(c).getInt("tCount", 0);
    }

    static void setCount(Context c, int n) {
        sp(c).edit().putInt("tCount", n).apply();
    }

    static int posX(Context c) {
        return sp(c).getInt("ox", 40);
    }

    static int posY(Context c) {
        return sp(c).getInt("oy", 400);
    }

    static void setPos(Context c, int x, int y) {
        sp(c).edit().putInt("ox", x).putInt("oy", y).apply();
    }

    /** Size of the floating tasbih, 0.5 – 2.0. */
    static float scale(Context c) {
        return sp(c).getFloat("tScale", 1f);
    }

    static void setScale(Context c, float s) {
        sp(c).edit().putFloat("tScale", s).apply();
    }

    /** 0 = shown, 1 = hidden on the left edge, 2 = hidden on the right edge. */
    static int mini(Context c) {
        return sp(c).getInt("mini", 0);
    }

    static int miniY(Context c) {
        return sp(c).getInt("miniY", 500);
    }

    static void setMini(Context c, int side, int y) {
        sp(c).edit().putInt("mini", side).putInt("miniY", y).apply();
    }

    // ---- reminder ----
    static final int[] REMINDER_HOURS = {0, 1, 2, 3};

    static int reminderHours(Context c) {
        return sp(c).getInt("remH", 2);
    }

    static void setReminderHours(Context c, int h) {
        sp(c).edit().putInt("remH", h).apply();
    }

    static long lastZikr(Context c) {
        return sp(c).getLong("lastZikr", 0);
    }

    static void setLastZikr(Context c, long t) {
        sp(c).edit().putLong("lastZikr", t).apply();
    }

    static long lastReminder(Context c) {
        return sp(c).getLong("lastRem", 0);
    }

    static void setLastReminder(Context c, long t) {
        sp(c).edit().putLong("lastRem", t).apply();
    }

    // ---- dhikr popup when the phone is unlocked ----
    static boolean unlockPopup(Context c) {
        return sp(c).getBoolean("unlock", true);
    }

    static void setUnlockPopup(Context c, boolean on) {
        sp(c).edit().putBoolean("unlock", on).apply();
    }

    static int unlockStyle(Context c) {
        int s = sp(c).getInt("uStyle", UnlockStyles.LAYERS);
        return s < 0 || s > UnlockStyles.ROTATE ? UnlockStyles.LAYERS : s;
    }

    static void setUnlockStyle(Context c, int s) {
        sp(c).edit().putInt("uStyle", s).apply();
    }

    /** The design to use now; "change every time" goes through all designs in turn. */
    static int unlockStyleNow(Context c) {
        int s = unlockStyle(c);
        if (s != UnlockStyles.ROTATE) return s;
        int n = sp(c).getInt("uRot", 0);
        sp(c).edit().putInt("uRot", n + 1).apply();
        return Math.floorMod(n, UnlockStyles.REAL);
    }

    /** Next dhikr for the unlock popup (rotates through the list). */
    static String nextUnlockText(Context c) {
        List<String> l = list(c);
        int i = Math.floorMod(sp(c).getInt("uIndex", 0), l.size());
        sp(c).edit().putInt("uIndex", (i + 1) % l.size()).apply();
        return l.get(i);
    }

    // ---- language: 0 ps, 1 fa, 2 en, 3 ur, 4 ar ----
    static int lang(Context c) {
        return sp(c).getInt("lang", 0);
    }

    static void setLang(Context c, int l) {
        sp(c).edit().putInt("lang", l).apply();
    }

    // ---- side-view roller tasbih: "krrr" sound and vibration while rolling
    static boolean rollSound(Context c) {
        return sp(c).getBoolean("rollSnd", true);
    }

    static void setRollSound(Context c, boolean on) {
        sp(c).edit().putBoolean("rollSnd", on).apply();
    }

    static boolean rollVib(Context c) {
        return sp(c).getBoolean("rollVib", true);
    }

    static void setRollVib(Context c, boolean on) {
        sp(c).edit().putBoolean("rollVib", on).apply();
    }

    // ---- counting dhikr with the eyes (0 = blink) or the mouth (1 = lips moving)
    static int faceMode(Context c) {
        return sp(c).getInt("faceMode", 0);
    }

    static void setFaceMode(Context c, int m) {
        sp(c).edit().putInt("faceMode", m).apply();
    }

    static boolean faceSound(Context c) {
        return sp(c).getBoolean("faceSnd", true);
    }

    static void setFaceSound(Context c, boolean on) {
        sp(c).edit().putBoolean("faceSnd", on).apply();
    }

    /** Remembers that the eyes/lips counter is running (the service sets and clears it). */
    static boolean faceOn(Context c) {
        return sp(c).getBoolean("faceOn", false);
    }

    static void setFaceOn(Context c, boolean on) {
        sp(c).edit().putBoolean("faceOn", on).apply();
    }
}
