package com.ghora.zikrbar;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import static com.ghora.zikrbar.Kit.*;

/**
 * The adhkar page. Without a category it shows the list of categories (morning, evening, mosque, waking up,
 * sleep, prayer, the 99 Names) and the daily reminder times. With a category it shows its adhkar as cards:
 * each tap counts one down, and a finished dhikr leaves the list so the next one moves up.
 */
public class AdhkarActivity extends Activity {

    static final String EXTRA_TAB = "tab";
    static final int MORNING = AdhkarData.MORNING, EVENING = AdhkarData.EVENING, NAMES = AdhkarData.NAMES;

    private Kit k;
    private int cat;
    private ScrollView scroll;
    private LinearLayout root;

    // category list state
    private AdhkarData.Item[] items;
    private int[] left;
    private LinearLayout list;
    private TextView progText;
    private ProgressBar progBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        getWindow().setStatusBarColor(GREEN2);
        getWindow().setNavigationBarColor(BG);
        cat = getIntent().getIntExtra(EXTRA_TAB, AdhkarData.HUB);
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        setContentView(scroll);
        build();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        cat = intent.getIntExtra(EXTRA_TAB, AdhkarData.HUB);
        build();
    }

    private boolean resumed;

    @Override
    protected void onResume() {
        super.onResume();
        // refresh the hub when coming back from a category (progress) or after midnight
        if (resumed && cat == AdhkarData.HUB) build();
        resumed = true;
    }

    private void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(32));
        scroll.removeAllViews();
        scroll.addView(root);
        if (cat == AdhkarData.HUB) buildHub();
        else if (cat == AdhkarData.NAMES) buildNames();
        else buildCategory();
    }

    // ================================================== header
    private LinearLayout header(String title, String arabicLine) {
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
        h.addView(back, bp);

        TextView t = k.title(title, 28, WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, k.dp(12), 0, k.dp(6));
        h.addView(t);
        if (arabicLine != null) h.addView(k.arabic(arabicLine, 19, 0xE6F3E3A5));
        root.addView(h);
        return h;
    }

    // ================================================== hub: list of categories
    private void buildHub() {
        header(L.t("adhkar_t"), "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ");

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(k.dp(12), k.dp(6), k.dp(12), 0);
        root.addView(grid);
        LinearLayout row = null;
        int[] order = AdhkarData.ORDER;
        for (int i = 0; i < order.length; i++) {
            boolean last = i == order.length - 1 && order.length % 2 == 1;
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, k.matchWrap());
            }
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tp.setMargins(k.dp(4), k.dp(8), k.dp(4), 0);
            row.addView(catTile(order[i], last), tp);
        }

        root.addView(reminderCard());
    }

    private View catTile(int c, boolean wide) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(k.dp(10), k.dp(16), k.dp(10), k.dp(16));
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.rounded(WHITE, 22, LINE), null));
        t.setElevation(k.dp(1.5f));
        t.addView(k.badge(AdhkarData.glyph(c), 48));

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setGravity(wide ? Gravity.START | Gravity.CENTER_VERTICAL : Gravity.CENTER);
        if (wide) tx.setPaddingRelative(k.dp(14), 0, 0, 0);
        TextView name = k.title(L.t(AdhkarData.nameKey(c)), 15, INK);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, wide ? 0 : k.dp(10), 0, k.dp(4));
        tx.addView(name);

        String sub;
        int subColor = MUTED;
        if (c == AdhkarData.NAMES) {
            sub = L.t("names_s");
        } else {
            int n = AdhkarData.items(this, c).length;
            if (n == 0) {
                sub = L.t("soon");
                subColor = GOLD_LINE;
            } else {
                int[] l = AdhkarData.left(this, c);
                int d = AdhkarData.doneCount(l);
                if (d == n) {
                    sub = L.t("done");
                    subColor = GREEN2;
                } else if (d > 0) {
                    sub = L.f("prog", L.num(d), L.num(n));
                    subColor = GREEN2;
                } else {
                    sub = L.f("n_adhkar", L.num(n));
                }
            }
        }
        TextView s = k.strong(sub, 12, subColor);
        s.setGravity(Gravity.CENTER);
        tx.addView(s);
        t.addView(tx, wide ? new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1) : k.matchWrap());

        t.setOnClickListener(v -> startActivity(new Intent(this, AdhkarActivity.class).putExtra(EXTRA_TAB, c)));
        return t;
    }

    private View reminderCard() {
        LinearLayout card = k.card("\u25F7", L.t("rem_daily_t"), L.t("rem_daily_s"));
        for (int c : new int[]{AdhkarData.MORNING, AdhkarData.EVENING}) {
            LinearLayout r = k.row();
            r.setPadding(0, k.dp(12), 0, 0);
            TextView name = k.strong(L.t(AdhkarData.nameKey(c)), 15, INK);
            r.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView time = k.strong(AdhkarData.clock(AdhkarData.remindAt(this, c)), 16, GREEN);
            time.setTextDirection(View.TEXT_DIRECTION_LTR);
            time.setPadding(k.dp(16), k.dp(6), k.dp(16), k.dp(8));
            time.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.rounded(SOFT, 14, LINE), null));
            time.setOnClickListener(v -> {
                int at = AdhkarData.remindAt(this, c);
                new TimePickerDialog(this, android.R.style.Theme_Material_Light_Dialog, (picker, h, m) -> {
                    AdhkarData.setRemindAt(this, c, h * 60 + m);
                    AdhkarData.setRemindOn(this, c, true);
                    AdhkarAlarm.schedule(this, c);
                    build();
                }, at / 60, at % 60, true).show();
            });
            r.addView(time);

            Switch sw = new Switch(this);
            sw.setChecked(AdhkarData.remindOn(this, c));
            sw.setThumbTintList(ColorStateList.valueOf(sw.isChecked() ? GREEN2 : 0xFFBDBDBD));
            sw.setOnCheckedChangeListener((b, on) -> {
                AdhkarData.setRemindOn(this, c, on);
                AdhkarAlarm.schedule(this, c);
                sw.setThumbTintList(ColorStateList.valueOf(on ? GREEN2 : 0xFFBDBDBD));
                time.setAlpha(on ? 1f : 0.45f);
            });
            time.setAlpha(sw.isChecked() ? 1f : 0.45f);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.setMarginStart(k.dp(10));
            r.addView(sw, sp);
            card.addView(r);
        }
        return card;
    }

    // ================================================== one category
    private void buildCategory() {
        items = AdhkarData.items(this, cat);
        LinearLayout h = header(L.t(AdhkarData.nameKey(cat)), null);

        if (items.length == 0) {
            LinearLayout soon = k.card(AdhkarData.glyph(cat), L.t("soon"), L.t("soon_s"));
            root.addView(soon);
            return;
        }

        left = AdhkarData.left(this, cat);
        progText = k.strong("", 13.5f, 0xE6FFFFFF);
        progText.setGravity(Gravity.CENTER);
        h.addView(progText);
        progBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progBar.setMax(items.length);
        progBar.setProgressTintList(ColorStateList.valueOf(GOLD_LINE));
        progBar.setProgressBackgroundTintList(ColorStateList.valueOf(0x40FFFFFF));
        LinearLayout.LayoutParams pb = k.matchWrap();
        pb.setMargins(k.dp(30), k.dp(8), k.dp(30), 0);
        h.addView(progBar, pb);

        TextView hint = k.text(L.t("adhkar_tap"), 13, MUTED);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(k.dp(22), k.dp(14), k.dp(22), 0);
        root.addView(hint);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LayoutTransition lt = new LayoutTransition();
        lt.setAnimator(LayoutTransition.DISAPPEARING, null);
        lt.setDuration(260);
        list.setLayoutTransition(lt);
        root.addView(list, k.matchWrap());
        for (int i = 0; i < items.length; i++) if (left[i] > 0) list.addView(itemCard(i));

        TextView src = k.text(L.t("source"), 12, MUTED);
        src.setGravity(Gravity.CENTER);
        src.setPadding(0, k.dp(18), 0, 0);
        root.addView(src);
        TextView reset = k.button(L.t("reset_all"), false, v -> {
            AdhkarData.reset(this, cat);
            build();
            scroll.scrollTo(0, 0);
        });
        LinearLayout.LayoutParams rp = k.matchWrap();
        rp.setMargins(k.dp(16), k.dp(10), k.dp(16), 0);
        root.addView(reset, rp);

        updateProgress();
    }

    private View itemCard(int i) {
        AdhkarData.Item it = items[i];
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(k.dp(18), k.dp(16), k.dp(18), k.dp(14));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(14), k.dp(12), k.dp(14), 0);
        c.setLayoutParams(lp);
        c.setElevation(k.dp(2));
        c.setBackground(new RippleDrawable(ColorStateList.valueOf(0x301B5E20), k.rounded(WHITE, 22, LINE), null));

        if (!it.title.isEmpty()) {
            TextView title = k.arabic(it.title, 17, GOLD_LINE);
            title.setPadding(0, 0, 0, k.dp(8));
            c.addView(title);
        }
        TextView text = k.arabic(it.text, 21, INK);
        text.setLineSpacing(0, 1.45f);
        text.setTextDirection(View.TEXT_DIRECTION_RTL);
        c.addView(text);

        if (!it.virtue.isEmpty()) {
            TextView v = k.arabic(it.virtue, 14.5f, 0xFF55705A);
            v.setLineSpacing(0, 1.3f);
            v.setTextDirection(View.TEXT_DIRECTION_RTL);
            v.setPadding(k.dp(12), k.dp(8), k.dp(12), k.dp(10));
            v.setBackground(k.rounded(0xFFF3F8F1, 14, 0));
            LinearLayout.LayoutParams vp = k.matchWrap();
            vp.setMargins(0, k.dp(12), 0, 0);
            c.addView(v, vp);
        }

        LinearLayout bottom = k.row();
        bottom.setPadding(0, k.dp(12), 0, 0);
        TextView count = k.title(L.num(left[i]), 18, WHITE);
        count.setGravity(Gravity.CENTER);
        count.setBackground(k.greenGradient(24));
        bottom.addView(count, new LinearLayout.LayoutParams(k.dp(48), k.dp(48)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPaddingRelative(k.dp(12), 0, 0, 0);
        TextView times = k.strong(it.times == 1 ? L.t("once") : L.f("times", L.num(it.times)), 13, INK);
        info.addView(times);
        if (!it.ref.isEmpty()) {
            TextView ref = k.text(it.ref, 11.5f, MUTED);
            ref.setMaxLines(2);
            info.addView(ref);
        }
        bottom.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        c.addView(bottom);

        c.setOnClickListener(v -> {
            if (left[i] <= 0) return;
            left[i]--;
            AdhkarData.save(this, cat, left);
            count.setText(L.num(left[i]));
            count.animate().cancel();
            count.setScaleX(1.25f);
            count.setScaleY(1.25f);
            count.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
            Prefs.setLastZikr(this, System.currentTimeMillis());
            if (left[i] > 0) {
                vibrate(14);
                return;
            }
            vibrate(60);
            c.setClickable(false);
            float dx = (L.rtl() ? -1 : 1) * c.getWidth() * 0.35f;
            c.animate().alpha(0f).translationX(dx).scaleX(0.94f).scaleY(0.94f).setDuration(240)
                    .withEndAction(() -> {
                        list.removeView(c);
                        updateProgress();
                    }).start();
        });
        return c;
    }

    private void updateProgress() {
        int d = AdhkarData.doneCount(left);
        progText.setText(L.f("prog", L.num(d), L.num(items.length)));
        progBar.setProgress(d);
        if (d == items.length && list.getChildCount() == 0) list.addView(doneCard());
    }

    private View doneCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(k.dp(20), k.dp(26), k.dp(20), k.dp(24));
        c.setBackground(k.rounded(0xFFE7F3E8, 24, 0xFF9CCB9F));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(14), k.dp(14), k.dp(14), 0);
        c.setLayoutParams(lp);
        c.addView(k.badge("✓", 64));
        TextView t = k.title(L.t("all_done"), 18, GREEN);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, k.dp(14), 0, k.dp(6));
        c.addView(t);
        c.addView(k.arabic("جَزَاكَ اللَّهُ خَيْرًا", 20, GOLD_LINE));
        return c;
    }

    // ================================================== 99 names
    private void buildNames() {
        header(L.t("names99"), null);
        TextView sub = k.text(L.t("names_s"), 14, MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(k.dp(20), k.dp(16), k.dp(20), k.dp(4));
        root.addView(sub);
        int cols = 3;
        LinearLayout row = null;
        for (int i = 0; i < Adhkar.NAMES.length; i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = k.matchWrap();
                rp.setMargins(k.dp(12), k.dp(8), k.dp(12), 0);
                root.addView(row, rp);
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

    // ================================================== helpers
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
