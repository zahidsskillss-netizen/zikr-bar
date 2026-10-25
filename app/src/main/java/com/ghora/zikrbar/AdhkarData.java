package com.ghora.zikrbar;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The adhkar categories. Texts come word for word from Hisn al-Muslim (assets/adhkar/*.json);
 * each item has a title (t), the text (z), how many times (n), the reference (r) and its virtue (v).
 */
final class AdhkarData {

    static final int HUB = -1, MORNING = 0, EVENING = 1, NAMES = 2, MOSQUE = 3, WAKE = 4, SLEEP = 5, PRAYER = 6;

    /** Categories in the order they appear on the adhkar page. */
    static final int[] ORDER = {MORNING, EVENING, MOSQUE, WAKE, SLEEP, PRAYER, NAMES};

    static final class Item {
        final String title, text, ref, virtue;
        final int times;

        Item(String title, String text, int times, String ref, String virtue) {
            this.title = title;
            this.text = text;
            this.times = Math.max(1, times);
            this.ref = ref;
            this.virtue = virtue;
        }
    }

    private static final Item[][] cache = new Item[7][];

    private AdhkarData() {}

    static String nameKey(int cat) {
        switch (cat) {
            case MORNING: return "morning";
            case EVENING: return "evening";
            case MOSQUE: return "cat_mosque";
            case WAKE: return "cat_wake";
            case SLEEP: return "cat_sleep";
            case PRAYER: return "cat_prayer";
            default: return "names99";
        }
    }

    static String glyph(int cat) {
        switch (cat) {
            case MORNING: return "☀︎";
            case EVENING: return "☾︎";
            case MOSQUE: return "۩";
            case WAKE: return "☼︎";
            case SLEEP: return "★︎";
            case PRAYER: return "۞";
            default: return "ﷲ";
        }
    }

    private static String file(int cat) {
        switch (cat) {
            case MORNING: return "morning";
            case EVENING: return "evening";
            case MOSQUE: return "mosque";
            case WAKE: return "wake";
            case SLEEP: return "sleep";
            case PRAYER: return "prayer";
            default: return null;
        }
    }

    /** The items of a category; empty when that category has not been added yet. */
    static synchronized Item[] items(Context c, int cat) {
        if (cat < 0 || cat >= cache.length) return new Item[0];
        if (cache[cat] != null) return cache[cat];
        Item[] out = new Item[0];
        String f = file(cat);
        if (f != null) {
            try (InputStream in = c.getApplicationContext().getAssets().open("adhkar/" + f + ".json")) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) buf.write(b, 0, n);
                JSONArray a = new JSONArray(new String(buf.toByteArray(), StandardCharsets.UTF_8));
                out = new Item[a.length()];
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    out[i] = new Item(o.optString("t"), o.optString("z"), o.optInt("n", 1),
                            o.optString("r"), o.optString("v"));
                }
            } catch (Exception ignored) {
                out = new Item[0];
            }
        }
        cache[cat] = out;
        return out;
    }

    // ---------------- today's progress ----------------

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("adhkar", Context.MODE_PRIVATE);
    }

    private static String key(int cat) {
        return "left_" + cat + "_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    /** How many times each item is still left today. */
    static int[] left(Context c, int cat) {
        Item[] items = items(c, cat);
        int[] out = new int[items.length];
        String raw = sp(c).getString(key(cat), null);
        String[] parts = raw == null ? new String[0] : raw.split(",");
        for (int i = 0; i < items.length; i++) {
            int v = items[i].times;
            if (parts.length == items.length) {
                try {
                    v = Math.max(0, Math.min(items[i].times, Integer.parseInt(parts[i])));
                } catch (NumberFormatException ignored) {
                }
            }
            out[i] = v;
        }
        return out;
    }

    static void save(Context c, int cat, int[] left) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < left.length; i++) {
            if (i > 0) b.append(',');
            b.append(left[i]);
        }
        sp(c).edit().putString(key(cat), b.toString()).apply();
    }

    static void reset(Context c, int cat) {
        sp(c).edit().remove(key(cat)).apply();
    }

    static int doneCount(int[] left) {
        int d = 0;
        for (int l : left) if (l <= 0) d++;
        return d;
    }

    static boolean allDone(Context c, int cat) {
        int[] l = left(c, cat);
        return l.length > 0 && doneCount(l) == l.length;
    }

    // ---------------- daily reminder settings ----------------

    static boolean remindOn(Context c, int cat) {
        return sp(c).getBoolean("on_" + cat, true);
    }

    static void setRemindOn(Context c, int cat, boolean on) {
        sp(c).edit().putBoolean("on_" + cat, on).apply();
    }

    /** Minutes after midnight: morning 06:00, evening 17:00 by default. */
    static int remindAt(Context c, int cat) {
        return sp(c).getInt("at_" + cat, cat == MORNING ? 6 * 60 : 17 * 60);
    }

    static void setRemindAt(Context c, int cat, int minutes) {
        sp(c).edit().putInt("at_" + cat, minutes).apply();
    }

    /** "06:00" with the digits of the chosen language. */
    static String clock(int minutes) {
        int h = minutes / 60, m = minutes % 60;
        return two(h) + ":" + two(m);
    }

    private static String two(int n) {
        return n < 10 ? L.num(0) + L.num(n) : L.num(n);
    }
}
