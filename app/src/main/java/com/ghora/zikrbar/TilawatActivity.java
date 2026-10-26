package com.ghora.zikrbar;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

import static com.ghora.zikrbar.Kit.*;

/**
 * Reading marks (د تلاوت نښې): where the user stopped reading the Quran — on the phone or in a printed
 * Quran — by surah and ayah, ruku, juz (with quarter / half / three-quarters), and page number.
 */
public class TilawatActivity extends Activity {

    private Kit k;
    private LinearLayout latestBox, historyBox;
    private int src = TilawatData.PHONE;
    private TextView chipPhone, chipMushaf;
    private Spinner surahSp, juzSp, partSp;
    private EditText ayahEt, rukuEt, pageEt, noteEt;
    private boolean setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.window(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(32));
        root.addView(header());

        latestBox = new LinearLayout(this);
        latestBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(latestBox, k.matchWrap());
        root.addView(form());
        historyBox = new LinearLayout(this);
        historyBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyBox, k.matchWrap());

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(root);
        setContentView(sv);
        paint();
    }

    // ---------------------------------------------------------------- views
    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(k.dp(18), k.dp(14), k.dp(18), k.dp(24));
        h.setBackground(k.headerBackground());
        TextView back = k.strong((L.rtl() ? "→  " : "←  ") + L.t("back"), 14, 0xE6FFFFFF);
        back.setPaddingRelative(k.dp(14), k.dp(6), k.dp(16), k.dp(8));
        back.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), k.rounded(0x26FFFFFF, 18, 0x40FFFFFF), null));
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.START;
        bp.bottomMargin = k.dp(10);
        h.addView(back, bp);
        TextView t = k.title(L.t("tl_title"), 26, WHITE);
        t.setGravity(Gravity.CENTER);
        h.addView(t);
        TextView s = k.text(L.t("tl_sub"), 13.5f, 0xD9FFFFFF);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, k.dp(6), 0, 0);
        h.addView(s);
        return h;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTypeface(k.body);
        e.setTextSize(16);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setSingleLine(true);
        e.setBackground(k.rounded(SOFT, 16, LINE));
        e.setPadding(k.dp(14), k.dp(10), k.dp(14), k.dp(10));
        return e;
    }

    private TextView chip(String s, View.OnClickListener l) {
        TextView c = k.strong(s, 14.5f, INK);
        c.setGravity(Gravity.CENTER);
        c.setPadding(k.dp(10), k.dp(11), k.dp(10), k.dp(11));
        c.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(k.dp(4), 0, k.dp(4), 0);
        c.setLayoutParams(lp);
        return c;
    }

    private View form() {
        LinearLayout c = k.card("۞", L.t("tl_new"), L.t("tl_new_s"));

        c.addView(k.label(L.t("tl_source")));
        LinearLayout row = k.row();
        chipPhone = chip("📱  " + L.t("tl_phone"), v -> setSource(TilawatData.PHONE));
        chipMushaf = chip("📖  " + L.t("tl_mushaf"), v -> setSource(TilawatData.MUSHAF));
        row.addView(chipPhone);
        row.addView(chipMushaf);
        c.addView(row, k.matchWrap());

        c.addView(k.label(L.t("tl_surah")));
        String[] sn = new String[114];
        sn[0] = "—";
        String[] all = new String[115];
        all[0] = "—";
        for (int i = 0; i < 114; i++) all[i + 1] = L.num(i + 1) + " · " + TilawatData.SURAH[i];
        surahSp = k.spinner(all, true);
        c.addView(surahSp, k.matchWrap());

        LinearLayout r1 = k.row();
        LinearLayout a = column(L.t("tl_ayah"));
        ayahEt = field("");
        ayahEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        a.addView(ayahEt, k.matchWrap());
        LinearLayout b = column(L.t("tl_ruku"));
        rukuEt = field("");
        rukuEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        b.addView(rukuEt, k.matchWrap());
        r1.addView(a, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        r1.addView(b, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        c.addView(r1, k.matchWrap());

        LinearLayout r2 = k.row();
        String[] juz = new String[31];
        juz[0] = "—";
        for (int i = 1; i <= 30; i++) juz[i] = L.t("tl_w_juz") + " " + L.num(i);
        LinearLayout j = column(L.t("tl_juz"));
        juzSp = k.spinner(juz, false);
        j.addView(juzSp, k.matchWrap());
        String[] parts = {"—", L.t("tl_p1"), L.t("tl_p2"), L.t("tl_p3"), L.t("tl_p4")};
        LinearLayout p = column(L.t("tl_part"));
        partSp = k.spinner(parts, false);
        p.addView(partSp, k.matchWrap());
        r2.addView(j, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        r2.addView(p, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        c.addView(r2, k.matchWrap());

        c.addView(k.label(L.t("tl_page")));
        pageEt = field("");
        pageEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        c.addView(pageEt, k.matchWrap());

        c.addView(k.label(L.t("tl_note")));
        noteEt = field("");
        noteEt.setInputType(InputType.TYPE_CLASS_TEXT);
        c.addView(noteEt, k.matchWrap());

        // choosing a surah and ayah suggests the juz
        AdapterView.OnItemSelectedListener suggest = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!setting) suggestJuz();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        surahSp.setOnItemSelectedListener(suggest);
        ayahEt.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!setting) suggestJuz();
            }
        });

        c.addView(k.button(L.t("tl_save"), true, v -> save()), k.topMargin(16));
        setSource(TilawatData.PHONE);
        return c;
    }

    private LinearLayout column(String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(k.dp(4), 0, k.dp(4), 0);
        TextView t = k.title(label, 14.5f, INK);
        t.setPadding(0, k.dp(16), 0, k.dp(8));
        col.addView(t);
        return col;
    }

    private void setSource(int s) {
        src = s;
        chipPhone.setBackground(k.rounded(s == TilawatData.PHONE ? 0x331B5E20 : SOFT, 16, s == TilawatData.PHONE ? GREEN : LINE));
        chipMushaf.setBackground(k.rounded(s == TilawatData.MUSHAF ? 0x331B5E20 : SOFT, 16, s == TilawatData.MUSHAF ? GREEN : LINE));
    }

    private void suggestJuz() {
        int s = surahSp.getSelectedItemPosition();
        int a = num(ayahEt);
        if (s <= 0) return;
        juzSp.setSelection(TilawatData.juzOf(s, Math.max(1, a)));
    }

    private static int num(EditText e) {
        String t = e.getText().toString().trim();
        if (t.isEmpty()) return 0;
        try {
            return Integer.parseInt(t.length() > 4 ? t.substring(0, 4) : t);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- saving
    private void save() {
        TilawatData.Mark m = new TilawatData.Mark();
        m.time = System.currentTimeMillis();
        m.src = src;
        m.surah = surahSp.getSelectedItemPosition();
        m.ayah = num(ayahEt);
        m.ruku = num(rukuEt);
        m.juz = juzSp.getSelectedItemPosition();
        m.part = partSp.getSelectedItemPosition();
        m.page = num(pageEt);
        m.note = noteEt.getText().toString().trim();
        if (m.surah == 0) m.ayah = 0;
        if (m.juz == 0) m.part = 0;
        if (m.surah == 0 && m.ruku == 0 && m.juz == 0 && m.page == 0) {
            Toast.makeText(this, L.t("tl_empty"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (m.surah > 0 && m.ayah > TilawatData.AYAHS[m.surah - 1]) {
            Toast.makeText(this, L.f("tl_ayah_bad", L.num(TilawatData.AYAHS[m.surah - 1])), Toast.LENGTH_LONG).show();
            return;
        }
        if (m.page > 700) {
            Toast.makeText(this, L.t("tl_page_bad"), Toast.LENGTH_SHORT).show();
            return;
        }
        TilawatData.add(this, m);
        Toast.makeText(this, L.t("tl_saved"), Toast.LENGTH_SHORT).show();
        noteEt.setText("");
        paint();
    }

    // ---------------------------------------------------------------- showing the marks
    private void paint() {
        latestBox.removeAllViews();
        historyBox.removeAllViews();
        TilawatData.Mark phone = TilawatData.latest(this, TilawatData.PHONE);
        TilawatData.Mark mushaf = TilawatData.latest(this, TilawatData.MUSHAF);
        if (phone != null || mushaf != null) {
            LinearLayout c = k.card("★", L.t("tl_last"), null);
            if (phone != null) c.addView(summary(phone, true), k.topMargin(8));
            if (mushaf != null) c.addView(summary(mushaf, true), k.topMargin(8));
            latestBox.addView(c);
        }
        List<TilawatData.Mark> all = TilawatData.load(this);
        if (!all.isEmpty()) {
            LinearLayout c = k.card("☰", L.t("tl_history"), null);
            for (TilawatData.Mark m : all) c.addView(summary(m, false), k.topMargin(8));
            historyBox.addView(c);
        }
    }

    private View summary(TilawatData.Mark m, boolean big) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(k.dp(14), k.dp(12), k.dp(14), k.dp(12));
        box.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.rounded(SOFT, 18, LINE), null));
        LinearLayout top = k.row();
        TextView who = k.strong((m.src == TilawatData.PHONE ? "📱 " : "📖 ") + L.t(m.src == TilawatData.PHONE ? "tl_phone" : "tl_mushaf"), 12.5f, GREEN2);
        top.addView(who, k.weight());
        TextView del = k.strong("×", 20, MUTED);
        del.setPadding(k.dp(14), 0, k.dp(4), 0);
        del.setOnClickListener(v -> {
            TilawatData.remove(this, m.time);
            paint();
        });
        top.addView(del);
        box.addView(top);
        TextView d = k.title(TilawatData.describe(m), big ? 17 : 15.5f, INK);
        d.setPadding(0, k.dp(6), 0, 0);
        box.addView(d);
        String when = date(m.time) + (m.note.isEmpty() ? "" : "  ·  " + m.note);
        TextView w = k.text(when, 12.5f, MUTED);
        w.setPadding(0, k.dp(4), 0, 0);
        box.addView(w);
        box.setOnClickListener(v -> load(m));
        return box;
    }

    /** Puts a saved mark back into the form, to continue from it. */
    private void load(TilawatData.Mark m) {
        setting = true;
        setSource(m.src);
        surahSp.setSelection(m.surah);
        ayahEt.setText(m.ayah > 0 ? String.valueOf(m.ayah) : "");
        rukuEt.setText(m.ruku > 0 ? String.valueOf(m.ruku) : "");
        juzSp.setSelection(m.juz);
        partSp.setSelection(m.part);
        pageEt.setText(m.page > 0 ? String.valueOf(m.page) : "");
        noteEt.setText(m.note);
        setting = false;
        Toast.makeText(this, L.t("tl_loaded"), Toast.LENGTH_SHORT).show();
    }

    private static String date(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return L.num(c.get(Calendar.YEAR)) + "/" + L.num(c.get(Calendar.MONTH) + 1) + "/" + L.num(c.get(Calendar.DAY_OF_MONTH))
                + "  " + L.num(c.get(Calendar.HOUR_OF_DAY)) + ":" + (c.get(Calendar.MINUTE) < 10 ? L.num(0) : "") + L.num(c.get(Calendar.MINUTE));
    }
}
