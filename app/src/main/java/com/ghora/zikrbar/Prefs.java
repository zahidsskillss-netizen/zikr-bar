package com.ghora.zikrbar;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Stores the dhikr list, the current position, the interval and whether the bar is on. */
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

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("zikr", Context.MODE_PRIVATE);
    }

    static String rawList(Context c) {
        return sp(c).getString("list", DEFAULT_LIST);
    }

    static void saveList(Context c, String raw) {
        sp(c).edit().putString("list", raw).putInt("index", 0).apply();
    }

    static List<String> list(Context c) {
        List<String> out = new ArrayList<>();
        for (String line : rawList(c).split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        if (out.isEmpty()) out.add("اللَّهُ");
        return out;
    }

    static String current(Context c) {
        List<String> l = list(c);
        int i = sp(c).getInt("index", 0);
        return l.get(Math.floorMod(i, l.size()));
    }

    static void next(Context c) {
        int size = list(c).size();
        int i = sp(c).getInt("index", 0);
        sp(c).edit().putInt("index", (i + 1) % size).apply();
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
}
