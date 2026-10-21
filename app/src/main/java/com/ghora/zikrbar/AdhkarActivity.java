package com.ghora.zikrbar;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.ghora.zikrbar.Kit.*;

/** Morning adhkar, evening adhkar and the 99 Names, with tap-to-count cards. */
public class AdhkarActivity extends Activity {

    static final String EXTRA_TAB = "tab";
    static final int MORNING = 0, EVENING = 1, NAMES = 2;

    private Kit k;
    private int tab;
    private LinearLayout content;
    private final TextView[] tabViews = new TextView[3];
    private int[] left;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        getWindow().setStatusBarColor(GREEN2);
        getWindow().setNavigationBarColor(BG);
        tab = getIntent().getIntExtra(EXTRA_TAB, MORNING);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(32));
        root.addView(header());
        root.addView(tabs());
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, k.matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);
        show(tab);
    }

    // ------------------------------------------------ header and tabs
    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(k.dp(18), k.dp(14), k.dp(18), k.dp(30));
        h.setBackground(k.headerBackground());

        TextView back = k.strong((L.rtl() ? "→  " : "←  ") + L.t("back"), 14, 0xE6FFFFFF);
        back.setPaddingRelative(k.dp(14), k.dp(6), k.dp(16), k.dp(8));
        back.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), k.rounded(0x26FFFFFF, 18, 0x40FFFFFF), null));
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.START;
        h.addView(back, bp);

        TextView t = k.title(L.t("adhkar_t"), 30, WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, k.dp(12), 0, k.dp(6));
        h.addView(t);
        h.addView(k.arabic("أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", 20, 0xE6F3E3A5));
        return h;
    }

    private View tabs() {
        LinearLayout row = k.row();
        row.setPadding(k.dp(6), k.dp(6), k.dp(6), k.dp(6));
        row.setBackground(k.rounded(WHITE, 24, LINE));
        row.setElevation(k.dp(4));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(16), -k.dp(22), k.dp(16), 0);
        row.setLayoutParams(lp);
        String[] names = {L.t("morning"), L.t("evening"), L.t("names99")};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView tv = k.strong(names[i], 13.5f, GREEN);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(k.dp(4), k.dp(10), k.dp(4), k.dp(12));
            tv.setOnClickListener(v -> show(idx));
            tabViews[i] = tv;
            row.addView(tv, k.weight());
        }
        return row;
    }

    private void show(int t) {
        tab = t;
        for (int i = 0; i < 3; i++) {
            boolean sel = i == t;
            tabViews[i].setBackground(sel ? k.greenGradient(18) : null);
            tabViews[i].setTextColor(sel ? WHITE : GREEN);
        }
        content.removeAllViews();
        if (t == NAMES) buildNames();
        else buildList(t == MORNING ? Adhkar.MORNING : Adhkar.EVENING);
    }

    // ------------------------------------------------ adhkar list
    private void buildList(Adhkar.Item[] items) {
        left = load(items);
        TextView hint = k.text(L.t("adhkar_tap"), 13.5f, MUTED);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(k.dp(20), k.dp(16), k.dp(20), 0);
        content.addView(hint);
        for (int i = 0; i < items.length; i++) content.addView(itemCard(items, i));
        TextView reset = k.button(L.t("reset_all"), false, v -> {
            for (int i = 0; i < items.length; i++) left[i] = items[i].times;
            save();
            show(tab);
        });
        LinearLayout.LayoutParams rp = k.matchWrap();
        rp.setMargins(k.dp(16), k.dp(16), k.dp(16), 0);
        content.addView(reset, rp);
    }

    private View itemCard(Adhkar.Item[] items, int i) {
        Adhkar.Item it = items[i];
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(k.dp(18), k.dp(16), k.dp(18), k.dp(16));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(16), k.dp(12), k.dp(16), 0);
        c.setLayoutParams(lp);
        c.setElevation(k.dp(1.5f));

        if (it.title != null) {
            TextView title = k.arabic(it.title, 17, GOLD_LINE);
            title.setPadding(0, 0, 0, k.dp(6));
            c.addView(title);
        }
        TextView text = k.arabic(it.text, 21, INK);
        text.setLineSpacing(0, 1.35f);
        text.setTextDirection(View.TEXT_DIRECTION_RTL);
        c.addView(text);

        LinearLayout bottom = k.row();
        bottom.setPadding(0, k.dp(12), 0, 0);
        TextView chip = k.strong("", 13, WHITE);
        chip.setPadding(k.dp(14), k.dp(4), k.dp(14), k.dp(6));
        bottom.addView(chip);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(it.times);
        bar.setProgressTintList(ColorStateList.valueOf(GREEN2));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        LinearLayout.LayoutParams bl = k.weight();
        bl.setMarginStart(k.dp(14));
        bottom.addView(bar, bl);
        TextView total = k.strong(L.f("times", L.num(it.times)), 12.5f, MUTED);
        total.setPaddingRelative(k.dp(12), 0, 0, 0);
        bottom.addView(total);
        c.addView(bottom);

        Runnable paint = () -> {
            int l = left[i];
            boolean done = l <= 0;
            c.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20),
                    k.rounded(done ? 0xFFE7F3E8 : WHITE, 22, done ? 0xFF9CCB9F : LINE), null));
            text.setTextColor(done ? 0xFF55705A : INK);
            chip.setText(done ? L.t("done") : L.f("left", L.num(l)));
            GradientDrawable g = done ? k.greenGradient(14) : k.rounded(GOLD_LINE, 14, 0);
            chip.setBackground(g);
            chip.setTextColor(done ? WHITE : 0xFF2D2002);
            bar.setProgress(it.times - Math.max(0, l));
        };
        paint.run();
        c.setOnClickListener(v -> {
            if (left[i] <= 0) return;
            left[i]--;
            save();
            paint.run();
            vibrate(left[i] == 0 ? 60 : 14);
            if (allDone()) Toast.makeText(this, L.t("all_done"), Toast.LENGTH_LONG).show();
        });
        return c;
    }

    private boolean allDone() {
        for (int l : left) if (l > 0) return false;
        return true;
    }

    // ------------------------------------------------ 99 names
    private void buildNames() {
        TextView sub = k.text(L.t("names_s"), 14, MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(k.dp(20), k.dp(16), k.dp(20), k.dp(4));
        content.addView(sub);
        int cols = 3;
        LinearLayout row = null;
        for (int i = 0; i < Adhkar.NAMES.length; i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = k.matchWrap();
                rp.setMargins(k.dp(12), k.dp(8), k.dp(12), 0);
                content.addView(row, rp);
            }
            row.addView(nameCell(i), cellParams());
        }
        int rest = Adhkar.NAMES.length % cols;
        if (rest != 0) for (int j = rest; j < cols; j++) row.addView(new View(this), cellParams());
    }

    private LinearLayout.LayoutParams cellParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, k.dp(96), 1);
        lp.setMargins(k.dp(4), 0, k.dp(4), 0);
        return lp;
    }

    private View nameCell(int i) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(k.dp(6), k.dp(8), k.dp(6), k.dp(8));
        c.setBackground(k.rounded(WHITE, 20, LINE));
        TextView n = k.strong(L.num(i + 1), 11, GOLD_LINE);
        n.setGravity(Gravity.CENTER);
        c.addView(n);
        TextView name = k.arabic(Adhkar.NAMES[i], Adhkar.NAMES[i].length() > 14 ? 15 : 20, GREEN);
        name.setPadding(0, k.dp(2), 0, 0);
        c.addView(name);
        return c;
    }

    // ------------------------------------------------ progress for today
    private String key() {
        return "adh_" + tab + "_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private SharedPreferences sp() {
        return getSharedPreferences("adhkar", Context.MODE_PRIVATE);
    }

    private int[] load(Adhkar.Item[] items) {
        int[] out = new int[items.length];
        String raw = sp().getString(key(), null);
        String[] parts = raw == null ? new String[0] : raw.split(",");
        for (int i = 0; i < items.length; i++) {
            int v = items[i].times;
            if (i < parts.length) {
                try {
                    v = Math.max(0, Math.min(items[i].times, Integer.parseInt(parts[i])));
                } catch (NumberFormatException ignored) {
                }
            }
            out[i] = v;
        }
        return out;
    }

    private void save() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < left.length; i++) {
            if (i > 0) b.append(',');
            b.append(left[i]);
        }
        sp().edit().putString(key(), b.toString()).apply();
    }

    private void vibrate(long ms) {
        Vibrator v = getSystemService(Vibrator.class);
        if (v == null || !v.hasVibrator()) return;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(ms);
        }
    }
}
