package com.ghora.zikrbar;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The page opened from the 9:30 sleep reminder, in one of four designs:
 * moon card, small counting panel, calm full page, or a check list of the sleep adhkar.
 */
public class SleepActivity extends Activity {

    static final int CARD = 0, PANEL = 1, CALM = 2, LIST = 3;
    static final String[] KEYS = {"sl_card", "sl_panel", "sl_calm", "sl_list"};
    static final String EXTRA_STYLE = "style";

    private Kit k;
    private AdhkarData.Item[] items;
    private int[] left;
    private final List<ObjectAnimator> loops = new ArrayList<>();

    private static final int NIGHT = 0xFF050817, INK = 0xFFEEF4FF, DIM = 0xFFB9C8E6, MOON = 0xFFE8C96A;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        items = AdhkarData.items(this, AdhkarData.SLEEP);
        left = AdhkarData.left(this, AdhkarData.SLEEP);
        int style = getIntent().getIntExtra(EXTRA_STYLE, AdhkarData.sleepStyle(this));
        if (items.length == 0) {
            open();
            return;
        }
        FrameLayout root = new FrameLayout(this);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        setContentView(root);
        getWindow().setStatusBarColor(0);
        switch (style) {
            case PANEL: panel(root); break;
            case CALM: calm(root); break;
            case LIST: list(root); break;
            default: card(root);
        }
    }

    @Override
    protected void onDestroy() {
        for (ObjectAnimator a : loops) a.cancel();
        super.onDestroy();
    }

    // ------------------------------------------------------------------ shared parts

    private int find(String start) {
        for (int i = 0; i < items.length; i++) if (items[i].text.startsWith(start)) return i;
        return 0;
    }

    private void dim(FrameLayout root) {
        root.setBackgroundColor(0xB3000000);
        root.setOnClickListener(v -> finish());
    }

    private GradientDrawable glass(float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xF2222C55, 0xF20B1030});
        g.setCornerRadius(k.dp(r));
        g.setStroke(k.dp(1.2f), 0x59FFFFFF);
        return g;
    }

    private TextView moon(int size) {
        TextView m = k.strong("☾︎", size * 0.36f, 0xFF3B2A05);
        m.setGravity(Gravity.CENTER);
        m.setIncludeFontPadding(false);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFFFFF3C4, MOON});
        g.setShape(GradientDrawable.OVAL);
        m.setBackground(g);
        m.setElevation(k.dp(6));
        m.setLayoutParams(new LinearLayout.LayoutParams(k.dp(size), k.dp(size)));
        return m;
    }

    private TextView btn(String s, boolean main, View.OnClickListener l) {
        TextView b = k.strong(s, 15, main ? 0xFF2B1C02 : INK);
        b.setGravity(Gravity.CENTER);
        b.setPadding(k.dp(12), k.dp(12), k.dp(12), k.dp(14));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(k.dp(16));
        if (main) g.setColor(MOON); else {
            g.setColor(0x14FFFFFF);
            g.setStroke(k.dp(1), 0x59FFFFFF);
        }
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), g, null));
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout buttons(boolean startMain) {
        LinearLayout r = k.row();
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        b.setMarginStart(k.dp(10));
        r.addView(btn(L.t("sl_start"), startMain, v -> open()), a);
        r.addView(btn(L.t("sl_later"), false, v -> {
            AdhkarAlarm.snooze(this, AdhkarData.SLEEP, 15);
            android.widget.Toast.makeText(this, L.t("sl_later_ok"), android.widget.Toast.LENGTH_SHORT).show();
            finish();
        }), b);
        return r;
    }

    private void open() {
        startActivity(new Intent(this, AdhkarActivity.class).putExtra(AdhkarActivity.EXTRA_TAB, AdhkarData.SLEEP));
        finish();
    }

    private String clock() {
        return AdhkarData.clock(AdhkarData.remindAt(this, AdhkarData.SLEEP));
    }

    private void stop(View v) {
        v.setClickable(true);
        v.setOnClickListener(x -> { });
    }

    // ------------------------------------------------------------------ 1. moon card from the top

    private void card(FrameLayout root) {
        dim(root);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(k.dp(20), k.dp(20), k.dp(20), k.dp(18));
        c.setBackground(glass(30));
        c.setElevation(k.dp(16));
        stop(c);

        LinearLayout head = k.row();
        head.addView(k.iconGap(moon(52), 14));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(k.title(L.t("cat_sleep"), 19, INK));
        TextView when = k.text(clock() + "  ·  " + L.t("sl_time"), 13, DIM);
        when.setPadding(0, k.dp(2), 0, 0);
        tx.addView(when);
        head.addView(tx, k.weight());
        c.addView(head);

        AdhkarData.Item it = items[find("بِاسْمِكَ اللَّهُمَّ")];
        TextView ar = k.arabic(it.text, 25, 0xFFFFF7DD);
        ar.setLineSpacing(0, 1.3f);
        ar.setPadding(0, k.dp(16), 0, k.dp(4));
        c.addView(ar);
        TextView m = k.text(L.t("sl_m_amutu"), 14, 0xFFD5E0F5);
        m.setGravity(Gravity.CENTER);
        m.setPadding(0, 0, 0, k.dp(16));
        c.addView(m);
        c.addView(buttons(true), k.matchWrap());

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        lp.setMargins(k.dp(14), k.dp(44), k.dp(14), 0);
        root.addView(c, lp);
        c.post(() -> {
            c.setCameraDistance(9000 * k.dp(1));
            c.setPivotY(0);
            c.setTranslationY(-c.getHeight() - k.dp(60));
            c.setRotationX(40f);
            c.animate().translationY(0).rotationX(0).setDuration(800).setInterpolator(new OvershootInterpolator(1.1f)).start();
        });
    }

    // ------------------------------------------------------------------ 2. small panel that counts

    private int cur;
    private TextView panelText, panelCount, panelStep;
    private ProgressBar panelBar;

    private void panel(FrameLayout root) {
        dim(root);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(k.dp(20), k.dp(18), k.dp(20), k.dp(18));
        c.setBackground(glass(28));
        c.setElevation(k.dp(16));
        stop(c);

        LinearLayout head = k.row();
        head.addView(k.title(L.t("cat_sleep"), 17, INK), k.weight());
        panelStep = k.strong("", 13, 0xFFC4B5FD);
        head.addView(panelStep);
        c.addView(head);
        panelBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        panelBar.setMax(items.length);
        panelBar.setProgressTintList(ColorStateList.valueOf(0xFFA78BFA));
        panelBar.setProgressBackgroundTintList(ColorStateList.valueOf(0x33FFFFFF));
        c.addView(panelBar, k.topMargin(8));

        ScrollView sc = new ScrollView(this);
        panelText = k.arabic("", 22, Color.WHITE);
        panelText.setLineSpacing(0, 1.35f);
        panelText.setPadding(0, k.dp(10), 0, k.dp(10));
        sc.addView(panelText);
        c.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, k.dp(200)));

        panelCount = k.title("", 40, Color.WHITE);
        panelCount.setGravity(Gravity.CENTER);
        GradientDrawable cg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0x66A78BFA, 0x14FFFFFF});
        cg.setShape(GradientDrawable.OVAL);
        cg.setStroke(k.dp(1.5f), 0x80FFFFFF);
        panelCount.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), cg, null));
        panelCount.setOnClickListener(v -> countPanel());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(k.dp(128), k.dp(128));
        cp.gravity = Gravity.CENTER_HORIZONTAL;
        cp.setMargins(0, k.dp(8), 0, k.dp(14));
        c.addView(panelCount, cp);

        LinearLayout r = k.row();
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        b.setMarginStart(k.dp(10));
        r.addView(btn(L.t("sl_full"), false, v -> open()), a);
        r.addView(btn(L.t("sl_next"), true, v -> step(cur + 1)), b);
        c.addView(r, k.matchWrap());

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        lp.setMargins(k.dp(14), 0, k.dp(14), 0);
        root.addView(c, lp);
        cur = -1;
        step(0);
        c.post(() -> {
            c.setPivotX(L.rtl() ? k.dp(30) : c.getWidth() - k.dp(30));
            c.setPivotY(0);
            c.setScaleX(0.15f);
            c.setScaleY(0.15f);
            c.setAlpha(0f);
            c.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(550).setInterpolator(new OvershootInterpolator(1.2f)).start();
        });
    }

    /** Moves to the next item that is still left today (wrapping around). */
    private void step(int from) {
        int n = items.length;
        int found = -1;
        for (int j = 0; j < n; j++) {
            int i = Math.floorMod(from + j, n);
            if (left[i] > 0) {
                found = i;
                break;
            }
        }
        int done = AdhkarData.doneCount(left);
        panelBar.setProgress(done);
        panelStep.setText(L.f("prog", L.num(done), L.num(n)));
        if (found < 0) {
            panelText.setText(L.t("sl_all_done"));
            panelText.setTypeface(k.strong);
            panelText.setTextSize(18);
            panelCount.setText("✓");
            panelCount.setOnClickListener(v -> finish());
            return;
        }
        cur = found;
        panelText.setText(items[cur].text);
        panelText.setTextSize(items[cur].text.length() > 220 ? 18 : 22);
        panelCount.setText(L.num(left[cur]));
        panelText.setAlpha(0f);
        panelText.animate().alpha(1f).setDuration(220).start();
    }

    private void countPanel() {
        if (cur < 0 || left[cur] <= 0) return;
        left[cur]--;
        AdhkarData.save(this, AdhkarData.SLEEP, left);
        Prefs.setLastZikr(this, System.currentTimeMillis());
        panelCount.setText(L.num(left[cur]));
        panelCount.setScaleX(1.15f);
        panelCount.setScaleY(1.15f);
        panelCount.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
        if (left[cur] == 0) panelCount.postDelayed(() -> step(cur + 1), 260);
    }

    // ------------------------------------------------------------------ 3. calm full page

    private void calm(FrameLayout root) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF03050F, 0xFF0A1440, 0xFF132A6B});
        root.setBackground(bg);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(k.dp(26), k.dp(48), k.dp(26), k.dp(30));
        root.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView x = k.strong("✕", 18, INK);
        x.setGravity(Gravity.CENTER);
        x.setBackground(k.rounded(0x1AFFFFFF, 22, 0x40FFFFFF));
        x.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams xp = new LinearLayout.LayoutParams(k.dp(44), k.dp(44));
        xp.gravity = Gravity.END;
        col.addView(x, xp);

        FrameLayout orb = new FrameLayout(this);
        GradientDrawable og = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0x47FFFFFF, 0x0AFFFFFF});
        og.setShape(GradientDrawable.OVAL);
        og.setStroke(k.dp(1.2f), 0x59FFFFFF);
        orb.setBackground(og);
        TextView mn = k.strong("☾︎", 70, 0xFFF3D98A);
        mn.setGravity(Gravity.CENTER);
        mn.setIncludeFontPadding(false);
        orb.addView(mn, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams opp = new LinearLayout.LayoutParams(k.dp(170), k.dp(170));
        opp.topMargin = k.dp(20);
        col.addView(orb, opp);

        TextView when = k.text(clock() + "  ·  " + L.t("sl_time"), 15, DIM);
        when.setGravity(Gravity.CENTER);
        when.setPadding(0, k.dp(28), 0, k.dp(6));
        col.addView(when);
        TextView t = k.title(L.t("cat_sleep"), 30, INK);
        t.setGravity(Gravity.CENTER);
        col.addView(t);
        AdhkarData.Item it = items[find("اللَّهُمَّ قِنِي")];
        TextView ar = k.arabic(it.text, 26, 0xFFFFF3C4);
        ar.setLineSpacing(0, 1.4f);
        ar.setPadding(0, k.dp(16), 0, k.dp(6));
        col.addView(ar);
        TextView m = k.text(L.t("sl_m_qini"), 14, DIM);
        m.setGravity(Gravity.CENTER);
        col.addView(m);

        View space = new View(this);
        col.addView(space, new LinearLayout.LayoutParams(1, 0, 1));
        TextView start = btn(L.t("sl_start"), true, v -> open());
        col.addView(start, k.matchWrap());
        TextView later = k.strong(L.t("sl_later"), 14, DIM);
        later.setGravity(Gravity.CENTER);
        later.setPadding(0, k.dp(14), 0, k.dp(4));
        later.setOnClickListener(v -> {
            AdhkarAlarm.snooze(this, AdhkarData.SLEEP, 15);
            finish();
        });
        col.addView(later, k.matchWrap());

        orb.post(() -> {
            orb.setCameraDistance(8000 * k.dp(1));
            orb.setTranslationY(k.dp(120));
            orb.setAlpha(0f);
            orb.animate().translationY(0).alpha(1f).setDuration(1200).setInterpolator(new DecelerateInterpolator(2f)).start();
            ObjectAnimator sway = ObjectAnimator.ofFloat(orb, "rotationY", -18f, 18f);
            sway.setDuration(3000);
            sway.setRepeatCount(ValueAnimator.INFINITE);
            sway.setRepeatMode(ValueAnimator.REVERSE);
            sway.setStartDelay(1200);
            sway.start();
            loops.add(sway);
        });
    }

    // ------------------------------------------------------------------ 4. check list from the bottom

    private LinearLayout listBox;
    private TextView listProg;

    private void list(FrameLayout root) {
        dim(root);
        LinearLayout sh = new LinearLayout(this);
        sh.setOrientation(LinearLayout.VERTICAL);
        sh.setPadding(k.dp(16), k.dp(12), k.dp(16), k.dp(16));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xF2123A40, 0xF2071A22});
        g.setCornerRadius(k.dp(30));
        g.setStroke(k.dp(1.2f), 0x47FFFFFF);
        sh.setBackground(g);
        sh.setElevation(k.dp(16));
        stop(sh);

        View handle = new View(this);
        handle.setBackground(k.rounded(0x59FFFFFF, 3, 0));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(k.dp(44), k.dp(5));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        sh.addView(handle, hp);
        LinearLayout head = k.row();
        head.setPadding(k.dp(4), k.dp(12), k.dp(4), k.dp(8));
        head.addView(k.title(L.t("cat_sleep"), 18, 0xFFEFFFFC), k.weight());
        listProg = k.strong("", 13, 0xFF7FF5E6);
        head.addView(listProg);
        sh.addView(head);

        ScrollView sc = new ScrollView(this);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        sc.addView(listBox);
        int maxH = Math.round(getResources().getDisplayMetrics().heightPixels * 0.55f);
        sh.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxH));
        TextView full = btn(L.t("sl_full"), true, v -> open());
        sh.addView(full, k.topMargin(12));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.setMargins(k.dp(10), 0, k.dp(10), k.dp(10));
        root.addView(sh, lp);
        fillList();
        sh.post(() -> {
            sh.setTranslationY(sh.getHeight() + k.dp(20));
            sh.animate().translationY(0).setDuration(600).setInterpolator(new DecelerateInterpolator(2f)).start();
        });
    }

    private void fillList() {
        listBox.removeAllViews();
        for (int i = 0; i < items.length; i++) {
            final int n = i;
            boolean on = left[i] <= 0;
            LinearLayout r = k.row();
            r.setPadding(k.dp(12), k.dp(10), k.dp(12), k.dp(10));
            GradientDrawable rg = new GradientDrawable();
            rg.setCornerRadius(k.dp(18));
            rg.setColor(on ? 0x247FF5E6 : 0x0FFFFFFF);
            rg.setStroke(k.dp(1), on ? 0x737FF5E6 : 0x24FFFFFF);
            r.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30FFFFFF), rg, null));

            TextView ck = k.strong(on ? "✓" : "", 15, 0xFF053B3F);
            ck.setGravity(Gravity.CENTER);
            GradientDrawable cg = new GradientDrawable();
            cg.setShape(GradientDrawable.OVAL);
            cg.setColor(on ? 0xFF7FF5E6 : 0x00000000);
            cg.setStroke(k.dp(1.5f), 0xFF7FF5E6);
            ck.setBackground(cg);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(k.dp(28), k.dp(28));
            cp.setMarginEnd(k.dp(12));
            r.addView(ck, cp);

            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            TextView ar = k.arabic(items[i].text, 17, 0xFFEFFFFC);
            ar.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            ar.setTextDirection(View.TEXT_DIRECTION_RTL);
            ar.setMaxLines(2);
            ar.setEllipsize(TextUtils.TruncateAt.END);
            tx.addView(ar, k.matchWrap());
            TextView tm = k.strong(items[i].times == 1 ? L.t("once") : L.f("times", L.num(items[i].times)), 12, 0xFFA7E6DD);
            tx.addView(tm);
            r.addView(tx, k.weight());
            r.setOnClickListener(v -> {
                left[n] = left[n] <= 0 ? items[n].times : 0;
                AdhkarData.save(this, AdhkarData.SLEEP, left);
                if (left[n] == 0) Prefs.setLastZikr(this, System.currentTimeMillis());
                fillList();
            });
            LinearLayout.LayoutParams lp = k.matchWrap();
            lp.topMargin = k.dp(8);
            listBox.addView(r, lp);
        }
        listProg.setText(L.f("prog", L.num(AdhkarData.doneCount(left)), L.num(items.length)));
    }
}
