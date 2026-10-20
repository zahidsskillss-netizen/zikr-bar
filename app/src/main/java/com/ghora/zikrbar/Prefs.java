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

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("zikr", Context.MODE_PRIVATE);
    }

    // ---- dhikr list ----
    static String rawList(Context c) {
        return sp(c).getString("list", DEFAULT_LIST);
    }

    static void saveList(Context c, String raw) {
        sp(c).edit().putString("list", raw).putInt("index", 0).putInt("tIndex", 0).apply();
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
        return sp(c).getInt("tStyle", 0);
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
}
