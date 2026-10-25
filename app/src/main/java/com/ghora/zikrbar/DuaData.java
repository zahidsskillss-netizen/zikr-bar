package com.ghora.zikrbar;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Duas from assets/duas/&lt;category&gt;.json. Each dua has a title in every language and one or more
 * parts: the Arabic text (z), its meaning (m, one text or one per language) and the reference (r).
 */
final class DuaData {

    static final class Part {
        final String text, meaning, ref;

        Part(String text, String meaning, String ref) {
            this.text = text;
            this.meaning = meaning;
            this.ref = ref;
        }
    }

    static final class Dua {
        final String cat;
        final int index;
        final JSONObject titles;
        final Part[] parts;

        Dua(String cat, int index, JSONObject titles, Part[] parts) {
            this.cat = cat;
            this.index = index;
            this.titles = titles;
            this.parts = parts;
        }

        String title() {
            return pick(titles);
        }

        String id() {
            return cat + "#" + index;
        }
    }

    private static final HashMap<String, Dua[]> cache = new HashMap<>();

    private DuaData() {}

    private static final String[] LANG_KEYS = {"ps", "fa", "en", "ur", "ar"};

    /** The text for the app language, falling back to Pashto, English, Urdu, Arabic. */
    static String pick(JSONObject o) {
        if (o == null) return "";
        String v = o.optString(LANG_KEYS[L.lang], "");
        if (v.isEmpty()) v = o.optString("ps", "");
        if (v.isEmpty()) v = o.optString("en", "");
        if (v.isEmpty()) v = o.optString("ur", "");
        if (v.isEmpty()) v = o.optString("ar", "");
        return v;
    }

    static synchronized Dua[] category(Context c, String cat) {
        if (cat.equals("all")) {
            List<Dua> all = new ArrayList<>();
            for (String[] k : DuaActivity.CATS) if (!k[0].equals("all")) for (Dua d : category(c, k[0])) all.add(d);
            return all.toArray(new Dua[0]);
        }
        Dua[] out = cache.get(cat);
        if (out != null) return out;
        out = new Dua[0];
        try (InputStream in = c.getApplicationContext().getAssets().open("duas/" + cat + ".json")) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            JSONArray a = new JSONArray(new String(buf.toByteArray(), StandardCharsets.UTF_8));
            out = new Dua[a.length()];
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                JSONArray items = o.getJSONArray("items");
                Part[] parts = new Part[items.length()];
                for (int j = 0; j < items.length(); j++) {
                    JSONObject p = items.getJSONObject(j);
                    Object m = p.opt("m");
                    String meaning = m instanceof JSONObject ? pick((JSONObject) m) : p.optString("m");
                    parts[j] = new Part(p.optString("z"), meaning, p.optString("r"));
                }
                out[i] = new Dua(cat, i, o.optJSONObject("t"), parts);
            }
        } catch (Exception ignored) {
            out = new Dua[0];
        }
        cache.put(cat, out);
        return out;
    }

    static Dua find(Context c, String id) {
        int h = id.indexOf('#');
        if (h < 0) return null;
        Dua[] d = category(c, id.substring(0, h));
        try {
            int i = Integer.parseInt(id.substring(h + 1));
            return i >= 0 && i < d.length ? d[i] : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------------- favourites ("My duas") ----------------
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("duas", Context.MODE_PRIVATE);
    }

    static Set<String> favs(Context c) {
        return new HashSet<>(sp(c).getStringSet("fav", new HashSet<>()));
    }

    static boolean isFav(Context c, String id) {
        return favs(c).contains(id);
    }

    static void toggleFav(Context c, String id) {
        Set<String> s = favs(c);
        if (!s.remove(id)) s.add(id);
        sp(c).edit().putStringSet("fav", s).apply();
    }
}
