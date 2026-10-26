package com.ghora.zikrbar;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The Quran's surahs and parts, and the saved reading marks (where the user stopped reading). */
final class TilawatData {

    static final int PHONE = 0, MUSHAF = 1;

    static final String[] SURAH = ("الفاتحة،البقرة،آل عمران،النساء،المائدة،الأنعام،الأعراف،الأنفال،التوبة،يونس،هود،يوسف،الرعد،إبراهيم،الحجر،النحل،الإسراء،الكهف،مريم،طه،"
            + "الأنبياء،الحج،المؤمنون،النور،الفرقان،الشعراء،النمل،القصص،العنكبوت،الروم،لقمان،السجدة،الأحزاب،سبأ،فاطر،يس،الصافات،ص،الزمر،غافر،"
            + "فصلت،الشورى،الزخرف،الدخان،الجاثية،الأحقاف،محمد،الفتح،الحجرات،ق،الذاريات،الطور،النجم،القمر،الرحمن،الواقعة،الحديد،المجادلة،الحشر،الممتحنة،"
            + "الصف،الجمعة،المنافقون،التغابن،الطلاق،التحريم،الملك،القلم،الحاقة،المعارج،نوح،الجن،المزمل،المدثر،القيامة،الإنسان،المرسلات،النبأ،النازعات،عبس،"
            + "التكوير،الانفطار،المطففين،الانشقاق،البروج،الطارق،الأعلى،الغاشية،الفجر،البلد،الشمس،الليل،الضحى،الشرح،التين،العلق،القدر،البينة،الزلزلة،العاديات،"
            + "القارعة،التكاثر،العصر،الهمزة،الفيل،قريش،الماعون،الكوثر،الكافرون،النصر،المسد،الإخلاص،الفلق،الناس").split("،");

    /** Number of ayahs in each surah. */
    static final int[] AYAHS = {7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227,
            93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14,
            11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5,
            8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6};

    /** Where each juz (para) starts: surah, ayah. */
    private static final int[][] JUZ = {{1, 1}, {2, 142}, {2, 253}, {3, 93}, {4, 24}, {4, 148}, {5, 82}, {6, 111}, {7, 88}, {8, 41}, {9, 93}, {11, 6},
            {12, 53}, {15, 1}, {17, 1}, {18, 75}, {21, 1}, {23, 1}, {25, 21}, {27, 56}, {29, 46}, {33, 31}, {36, 28}, {39, 32}, {41, 47}, {46, 1},
            {51, 31}, {58, 1}, {67, 1}, {78, 1}};

    private TilawatData() {}

    /** The juz (1–30) that holds this ayah. */
    static int juzOf(int surah, int ayah) {
        int j = 1;
        for (int i = 0; i < JUZ.length; i++) {
            if (surah > JUZ[i][0] || (surah == JUZ[i][0] && ayah >= JUZ[i][1])) j = i + 1;
        }
        return j;
    }

    /** One saved mark. Fields that are 0 were not filled in. */
    static final class Mark {
        long time;
        int src, surah, ayah, ruku, juz, part, page;
        String note = "";
    }

    static List<Mark> load(Context c) {
        List<Mark> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(Prefs.sp(c).getString("tilawat", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Mark m = new Mark();
                m.time = o.optLong("t");
                m.src = o.optInt("src");
                m.surah = o.optInt("s");
                m.ayah = o.optInt("a");
                m.ruku = o.optInt("r");
                m.juz = o.optInt("j");
                m.part = o.optInt("p");
                m.page = o.optInt("pg");
                m.note = o.optString("n", "");
                out.add(m);
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    private static void save(Context c, List<Mark> marks) {
        JSONArray a = new JSONArray();
        try {
            for (Mark m : marks) {
                JSONObject o = new JSONObject();
                o.put("t", m.time);
                o.put("src", m.src);
                o.put("s", m.surah);
                o.put("a", m.ayah);
                o.put("r", m.ruku);
                o.put("j", m.juz);
                o.put("p", m.part);
                o.put("pg", m.page);
                o.put("n", m.note);
                a.put(o);
            }
        } catch (JSONException ignored) {
        }
        Prefs.sp(c).edit().putString("tilawat", a.toString()).apply();
    }

    /** Newest first; only the latest 60 marks are kept. */
    static void add(Context c, Mark m) {
        List<Mark> all = load(c);
        all.add(0, m);
        while (all.size() > 60) all.remove(all.size() - 1);
        save(c, all);
    }

    static void remove(Context c, long time) {
        List<Mark> all = load(c);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).time == time) {
                all.remove(i);
                break;
            }
        }
        save(c, all);
    }

    /** The newest mark made in the phone (src 0) or in the printed Quran (src 1); null if none. */
    static Mark latest(Context c, int src) {
        for (Mark m : load(c)) if (m.src == src) return m;
        return null;
    }

    static Mark latest(Context c) {
        List<Mark> all = load(c);
        return all.isEmpty() ? null : all.get(0);
    }

    /** Readable one-line description of a mark, in the app's language. */
    static String describe(Mark m) {
        List<String> p = new ArrayList<>();
        if (m.surah > 0) p.add(SURAH[m.surah - 1] + (m.ayah > 0 ? " : " + L.num(m.ayah) : ""));
        if (m.ruku > 0) p.add(L.t("tl_w_ruku") + " " + L.num(m.ruku));
        if (m.juz > 0) p.add(L.t("tl_w_juz") + " " + L.num(m.juz) + (m.part > 0 ? " · " + L.t("tl_p" + m.part) : ""));
        if (m.page > 0) p.add(L.t("tl_w_page") + " " + L.num(m.page));
        return p.isEmpty() ? "—" : android.text.TextUtils.join("  ·  ", p);
    }
}
