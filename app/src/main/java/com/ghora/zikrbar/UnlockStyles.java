package com.ghora.zikrbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The animated glass designs for the dhikr shown when the phone is unlocked.
 * LAYERS is drawn by ZikrService itself (the original design); the rest are built here.
 */
final class UnlockStyles {

    static final int LAYERS = 0, PILL = 1, FLIP = 2, PEARL = 3, STAR = 4, ARCH = 5, SHEET = 6, ROTATE = 7;
    static final int REAL = 7;
    static final String[] KEYS = {"us_layers", "us_pill", "us_flip", "us_pearl", "us_star", "us_arch", "us_sheet", "us_rotate"};
    static final String AYAH = "فَاذْكُرُونِي أَذْكُرْكُمْ";

    /** A built popup: its root view, where it sits, and how it comes in and goes out. */
    static final class Shown {
        View root;
        int gravity = Gravity.CENTER;
        int y;
        boolean fullWidth;
        long hold = 2300;
        Runnable enter;
        Exit exit;
        final List<Animator> loops = new ArrayList<>();

        void stopLoops() {
            for (Animator a : loops) a.cancel();
            loops.clear();
        }
    }

    interface Exit { void run(Runnable done); }

    private UnlockStyles() {}

    static Shown build(Context c, int style, String text, Point screen) {
        Kit k = new Kit(c);
        switch (style) {
            case PILL: return pill(c, k, text, screen);
            case FLIP: return flip(c, k, text, screen);
            case PEARL: return pearl(c, k, text, screen);
            case STAR: return star(c, k, text);
            case ARCH: return arch(c, k, text);
            default: return sheet(c, k, text);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static float fit(String t, float big) {
        int n = t.length();
        return n > 90 ? big * 0.62f : n > 55 ? big * 0.72f : n > 30 ? big * 0.84f : big;
    }

    private static TextView dhikr(Kit k, String text, float sp, int color, int lines) {
        TextView t = k.arabic(text, fit(text, sp), color);
        t.setLineSpacing(0, 1.12f);
        t.setMaxLines(lines);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private static GradientDrawable grad(int a, int b, float radius, int strokePx, int stroke) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{a, b});
        g.setCornerRadius(radius);
        if (strokePx > 0) g.setStroke(strokePx, stroke);
        return g;
    }

    private static ObjectAnimator loop(View v, String prop, long ms, float... values) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, prop, values);
        a.setDuration(ms);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        return a;
    }

    // ------------------------------------------------------------------ 1. pill from the top

    private static Shown pill(Context c, Kit k, String text, Point screen) {
        Shown s = new Shown();
        FrameLayout root = new FrameLayout(c);
        root.setClipChildren(false);
        root.setPadding(k.dp(12), k.dp(10), k.dp(12), k.dp(14));

        LinearLayout pill = new LinearLayout(c);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        pill.setPadding(k.dp(8), k.dp(8), k.dp(8), k.dp(8));
        pill.setBackground(grad(0xE6162033, 0xE60B1220, k.dp(32), k.dp(1.2f), 0x80FFFFFF));
        pill.setElevation(k.dp(10));

        TextView mark = k.arabic("ﷲ", 17, 0xFF3D2803);
        mark.setIncludeFontPadding(false);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(0xFFF5B83D);
        mark.setBackground(dot);
        pill.addView(mark, new LinearLayout.LayoutParams(k.dp(42), k.dp(42)));

        TextView zk = dhikr(k, text, 21, Color.WHITE, 2);
        zk.setPadding(k.dp(14), 0, k.dp(16), 0);
        zk.setAlpha(0f);
        int maxText = screen.x - k.dp(24 + 16 + 42 + 30);
        zk.setMaxWidth(maxText);
        pill.addView(zk, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(pill, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        s.root = root;
        s.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        s.y = k.dp(30);
        s.hold = 2600;
        s.enter = () -> {
            pill.setCameraDistance(8000 * k.dp(1));
            pill.setTranslationY(-k.dp(120));
            pill.setRotationX(60f);
            pill.setAlpha(0f);
            int full = pill.getWidth(), small = k.dp(58);
            root.setMinimumWidth(root.getWidth());
            root.setMinimumHeight(root.getHeight());
            // start as a small circle and stretch to the text's width
            pill.setClipToOutline(true);
            pill.setPivotX(full / 2f);
            pill.animate().translationY(0).rotationX(0).alpha(1f).setDuration(650)
                    .setInterpolator(new OvershootInterpolator(1.4f)).start();
            ValueAnimator grow = ValueAnimator.ofInt(small, full);
            grow.setStartDelay(380);
            grow.setDuration(700);
            grow.setInterpolator(new DecelerateInterpolator(2f));
            grow.addUpdateListener(a -> {
                ViewGroup.LayoutParams lp = pill.getLayoutParams();
                lp.width = (int) a.getAnimatedValue();
                pill.setLayoutParams(lp);
            });
            ViewGroup.LayoutParams lp = pill.getLayoutParams();
            lp.width = small;
            pill.setLayoutParams(lp);
            grow.start();
            zk.animate().alpha(1f).setStartDelay(760).setDuration(380).start();
            ObjectAnimator glow = loop(mark, "scaleX", 1100, 1f, 1.08f);
            ObjectAnimator glowY = loop(mark, "scaleY", 1100, 1f, 1.08f);
            glow.setStartDelay(1100);
            glowY.setStartDelay(1100);
            glow.start();
            glowY.start();
            s.loops.add(grow);
            s.loops.add(glow);
            s.loops.add(glowY);
        };
        s.exit = done -> pill.animate().translationY(-k.dp(130)).alpha(0f).setStartDelay(0).setDuration(320)
                .setInterpolator(new AccelerateInterpolator(1.5f)).withEndAction(done).start();
        return s;
    }

    // ------------------------------------------------------------------ 2. card that turns over

    private static Shown flip(Context c, Kit k, String text, Point screen) {
        Shown s = new Shown();
        FrameLayout root = new FrameLayout(c);
        root.setClipChildren(false);
        root.setPadding(k.dp(40), k.dp(40), k.dp(40), k.dp(40));

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(k.dp(26), k.dp(22), k.dp(26), k.dp(20));
        card.setBackground(grad(0xF20F4A2E, 0xF2041A10, k.dp(28), k.dp(2), 0xFFF3D77A));
        card.setElevation(k.dp(14));
        TextView zk = dhikr(k, text, 30, 0xFFF2FFF7, 4);
        zk.setMaxWidth(screen.x - k.dp(80 + 52));
        card.addView(zk);
        View bar = new View(c);
        bar.setBackgroundColor(0xFFF3D77A);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(k.dp(54), k.dp(2));
        bp.setMargins(0, k.dp(10), 0, k.dp(8));
        card.addView(bar, bp);
        card.addView(k.arabic(AYAH, 14, 0xFFBFE6CF));
        root.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        s.root = root;
        s.enter = () -> {
            card.setCameraDistance(9000 * k.dp(1));
            card.setAlpha(0f);
            ValueAnimator in = ValueAnimator.ofFloat(0f, 1f);
            in.setDuration(1000);
            in.setInterpolator(new OvershootInterpolator(1.1f));
            in.addUpdateListener(a -> {
                float p = (float) a.getAnimatedValue();
                card.setRotationY(-180f * (1f - p));
                float sc = 0.7f + 0.3f * p;
                card.setScaleX(sc);
                card.setScaleY(sc);
                card.setAlpha(p < 0.5f ? 0f : Math.min(1f, (p - 0.5f) * 2.4f));
            });
            in.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ObjectAnimator tilt = loop(card, "rotationX", 1600, 6f, -6f);
                    tilt.start();
                    s.loops.add(tilt);
                }
            });
            in.start();
            s.loops.add(in);
        };
        s.exit = done -> {
            s.stopLoops();
            card.animate().rotationY(90f).alpha(0f).setDuration(320).setInterpolator(new AccelerateInterpolator())
                    .withEndAction(done).start();
        };
        return s;
    }

    // ------------------------------------------------------------------ 3. light pearl card with a shine

    private static Shown pearl(Context c, Kit k, String text, Point screen) {
        Shown s = new Shown();
        FrameLayout root = new FrameLayout(c);
        root.setClipChildren(false);
        root.setPadding(k.dp(36), k.dp(36), k.dp(36), k.dp(36));

        FrameLayout card = new FrameLayout(c);
        card.setBackground(grad(0xF7FFFFFF, 0xD9EEF6F1, k.dp(40), k.dp(1.5f), 0xFFFFFFFF));
        card.setElevation(k.dp(16));
        card.setClipToOutline(true);
        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(k.dp(28), k.dp(24), k.dp(28), k.dp(22));
        TextView zk = dhikr(k, text, 30, 0xFF0B3D27, 4);
        zk.setMaxWidth(screen.x - k.dp(72 + 56));
        inner.addView(zk);
        TextView ay = k.arabic(AYAH, 14, 0xFF3F5A4C);
        ay.setPadding(0, k.dp(8), 0, 0);
        inner.addView(ay);
        card.addView(inner);
        View shine = new View(c);
        shine.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x00FFFFFF, 0xCCFFFFFF, 0x00FFFFFF}));
        shine.setRotation(18f);
        card.addView(shine, new FrameLayout.LayoutParams(k.dp(60), ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        int[] colors = {0xFFB8860B, 0xFF0F7A4B, 0xFF5B8DEF};
        int[][] spots = {{Gravity.TOP | Gravity.START, 14, 10}, {Gravity.BOTTOM | Gravity.END, 30, 14}, {Gravity.CENTER_VERTICAL | Gravity.END, 4, 0}};
        List<TextView> sparks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TextView sp = k.strong("✦", 16 - i * 2, colors[i]);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, spots[i][0]);
            lp.setMargins(k.dp(spots[i][1]), k.dp(spots[i][2]), k.dp(spots[i][1]), k.dp(spots[i][2]));
            sp.setScaleX(0f);
            sp.setScaleY(0f);
            root.addView(sp, lp);
            sparks.add(sp);
        }

        s.root = root;
        s.enter = () -> {
            card.setScaleX(0.4f);
            card.setScaleY(0.4f);
            card.setAlpha(0f);
            card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(700).setInterpolator(new OvershootInterpolator(1.6f)).start();
            ObjectAnimator hover = loop(card, "translationY", 1800, 0f, -k.dp(8));
            hover.setStartDelay(700);
            hover.start();
            s.loops.add(hover);
            int w = card.getWidth();
            shine.setTranslationX(-k.dp(90));
            ObjectAnimator sweep = ObjectAnimator.ofFloat(shine, "translationX", -k.dp(90), w + k.dp(30));
            sweep.setDuration(1300);
            sweep.setStartDelay(600);
            sweep.setRepeatCount(ValueAnimator.INFINITE);
            sweep.setInterpolator(new DecelerateInterpolator());
            sweep.start();
            s.loops.add(sweep);
            for (int i = 0; i < sparks.size(); i++) {
                ObjectAnimator a = loop(sparks.get(i), "scaleX", 900, 0f, 1.2f);
                ObjectAnimator b = loop(sparks.get(i), "scaleY", 900, 0f, 1.2f);
                a.setStartDelay(500 + i * 300L);
                b.setStartDelay(500 + i * 300L);
                a.start();
                b.start();
                s.loops.add(a);
                s.loops.add(b);
            }
        };
        s.exit = done -> {
            s.stopLoops();
            root.animate().scaleX(1.12f).scaleY(1.12f).alpha(0f).setDuration(300).withEndAction(done).start();
        };
        return s;
    }

    // ------------------------------------------------------------------ 4. eight-point star

    private static Shown star(Context c, Kit k, String text) {
        Shown s = new Shown();
        int size = k.dp(290);
        FrameLayout root = new FrameLayout(c);
        root.setClipChildren(false);
        FrameLayout box = new FrameLayout(c);
        box.setClipChildren(false);
        root.addView(box, new FrameLayout.LayoutParams(size, size));

        View sq1 = new View(c);
        sq1.setBackground(grad(0x66E8C96A, 0x14E8C96A, k.dp(34), k.dp(1.5f), 0xCCE8C96A));
        View sq2 = new View(c);
        sq2.setBackground(grad(0x40FFFFFF, 0x0AFFFFFF, k.dp(34), k.dp(1.5f), 0x99FFFFFF));
        sq2.setRotation(45f);
        int sq = k.dp(206);
        box.addView(sq1, new FrameLayout.LayoutParams(sq, sq, Gravity.CENTER));
        box.addView(sq2, new FrameLayout.LayoutParams(sq, sq, Gravity.CENTER));

        LinearLayout core = new LinearLayout(c);
        core.setOrientation(LinearLayout.VERTICAL);
        core.setGravity(Gravity.CENTER);
        core.setPadding(k.dp(16), k.dp(16), k.dp(16), k.dp(16));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xF22A3560, 0xF20A0F24});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(k.dp(1.8f), 0xFFE8C96A);
        core.setBackground(g);
        core.setElevation(k.dp(12));
        core.addView(dhikr(k, text, 22, 0xFFFFF8E6, 4));
        box.addView(core, new FrameLayout.LayoutParams(k.dp(176), k.dp(176), Gravity.CENTER));

        s.root = root;
        s.enter = () -> {
            box.setCameraDistance(9000 * k.dp(1));
            box.setRotationX(80f);
            box.setScaleX(0.5f);
            box.setScaleY(0.5f);
            box.setAlpha(0f);
            box.animate().rotationX(16f).scaleX(1f).scaleY(1f).alpha(1f).setDuration(900)
                    .setInterpolator(new OvershootInterpolator(1.1f)).start();
            ObjectAnimator r1 = ObjectAnimator.ofFloat(sq1, "rotation", 0f, 360f);
            ObjectAnimator r2 = ObjectAnimator.ofFloat(sq2, "rotation", 45f, -315f);
            for (ObjectAnimator r : new ObjectAnimator[]{r1, r2}) {
                r.setDuration(r == r1 ? 9000 : 12000);
                r.setRepeatCount(ValueAnimator.INFINITE);
                r.setInterpolator(new LinearInterpolator());
                r.start();
                s.loops.add(r);
            }
            ObjectAnimator tilt = loop(box, "rotationY", 1500, -10f, 10f);
            tilt.setStartDelay(900);
            tilt.start();
            s.loops.add(tilt);
        };
        s.exit = done -> {
            s.stopLoops();
            box.animate().rotationX(-60f).scaleX(1.3f).scaleY(1.3f).alpha(0f).setDuration(340)
                    .setInterpolator(new AccelerateInterpolator()).withEndAction(done).start();
        };
        return s;
    }

    // ------------------------------------------------------------------ 5. mihrab with opening doors

    private static Shown arch(Context c, Kit k, String text) {
        Shown s = new Shown();
        int w = k.dp(264), h = k.dp(352);
        float top = w / 2f, bot = k.dp(28);
        FrameLayout root = new FrameLayout(c);
        root.setClipChildren(false);
        root.setPadding(k.dp(24), k.dp(24), k.dp(24), k.dp(24));

        FrameLayout arch = new FrameLayout(c);
        arch.setClipChildren(false);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xF2183A2A, 0xF2071710});
        bg.setCornerRadii(new float[]{top, top, top, top, bot, bot, bot, bot});
        bg.setStroke(k.dp(1.6f), 0xFFF3D77A);
        arch.setBackground(bg);
        arch.setElevation(k.dp(14));
        root.addView(arch, new FrameLayout.LayoutParams(w, h, Gravity.CENTER));

        View inline = new View(c);
        GradientDrawable il = new GradientDrawable();
        float it = top - k.dp(14);
        il.setCornerRadii(new float[]{it, it, it, it, k.dp(18), k.dp(18), k.dp(18), k.dp(18)});
        il.setStroke(k.dp(1), 0x59F3D77A);
        inline.setBackground(il);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ilp.setMargins(k.dp(14), k.dp(14), k.dp(14), k.dp(14));
        arch.addView(inline, ilp);

        View lamp = new View(c);
        GradientDrawable lg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFFFFF3C4, 0xFFE8B84A});
        lg.setCornerRadii(new float[]{k.dp(12), k.dp(12), k.dp(12), k.dp(12), k.dp(7), k.dp(7), k.dp(7), k.dp(7)});
        lamp.setBackground(lg);
        lamp.setElevation(k.dp(6));
        FrameLayout.LayoutParams lpp = new FrameLayout.LayoutParams(k.dp(24), k.dp(30), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lpp.topMargin = k.dp(58);
        arch.addView(lamp, lpp);

        LinearLayout txt = new LinearLayout(c);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setGravity(Gravity.CENTER);
        txt.setPadding(k.dp(26), k.dp(70), k.dp(26), k.dp(20));
        txt.addView(dhikr(k, text, 27, 0xFFFFF8E6, 5));
        View bar = new View(c);
        bar.setBackgroundColor(0xFFF3D77A);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(k.dp(48), k.dp(2));
        bp.setMargins(0, k.dp(12), 0, k.dp(8));
        txt.addView(bar, bp);
        txt.addView(k.arabic(AYAH, 13, 0xFFE7DDBF));
        arch.addView(txt, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View dl = new View(c), dr = new View(c);
        GradientDrawable gl = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0F5032, 0xFF08281A});
        gl.setCornerRadii(new float[]{top, top, 0, 0, 0, 0, bot, bot});
        gl.setStroke(k.dp(1.4f), 0x99F3D77A);
        GradientDrawable gr = new GradientDrawable(GradientDrawable.Orientation.TR_BL, new int[]{0xFF0F5032, 0xFF08281A});
        gr.setCornerRadii(new float[]{0, 0, top, top, bot, bot, 0, 0});
        gr.setStroke(k.dp(1.4f), 0x99F3D77A);
        dl.setBackground(gl);
        dr.setBackground(gr);
        dl.setElevation(k.dp(16));
        dr.setElevation(k.dp(16));
        arch.addView(dl, new FrameLayout.LayoutParams(w / 2, h, Gravity.START));
        arch.addView(dr, new FrameLayout.LayoutParams(w / 2, h, Gravity.END));

        s.root = root;
        s.hold = 2700;
        s.enter = () -> {
            float cam = 9000 * k.dp(1);
            dl.setCameraDistance(cam);
            dr.setCameraDistance(cam);
            boolean ltrLeft = dl.getLeft() < dr.getLeft();
            View left = ltrLeft ? dl : dr, right = ltrLeft ? dr : dl;
            left.setPivotX(0);
            right.setPivotX(right.getWidth());
            arch.setScaleX(0.85f);
            arch.setScaleY(0.85f);
            arch.setAlpha(0f);
            arch.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(450).setInterpolator(new DecelerateInterpolator()).start();
            left.animate().rotationY(-100f).alpha(0f).setStartDelay(420).setDuration(1300)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator()).start();
            right.animate().rotationY(100f).alpha(0f).setStartDelay(420).setDuration(1300)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator()).start();
            ObjectAnimator glow = loop(lamp, "alpha", 1200, 0.7f, 1f);
            ObjectAnimator sway = loop(lamp, "translationY", 1500, 0f, k.dp(4));
            glow.start();
            sway.start();
            s.loops.add(glow);
            s.loops.add(sway);
        };
        s.exit = done -> {
            s.stopLoops();
            arch.animate().scaleX(0.9f).scaleY(0.9f).alpha(0f).setDuration(320).withEndAction(done).start();
        };
        return s;
    }

    // ------------------------------------------------------------------ 6. sheet from the bottom

    private static Shown sheet(Context c, Kit k, String text) {
        Shown s = new Shown();
        FrameLayout root = new FrameLayout(c);
        root.setClipChildren(false);
        root.setPadding(k.dp(12), k.dp(30), k.dp(12), k.dp(14));

        LinearLayout sh = new LinearLayout(c);
        sh.setOrientation(LinearLayout.VERTICAL);
        sh.setGravity(Gravity.CENTER_HORIZONTAL);
        sh.setPadding(k.dp(22), k.dp(12), k.dp(22), k.dp(22));
        sh.setBackground(grad(0xF21B2A44, 0xF20A1222, k.dp(32), k.dp(1.4f), 0x80FFFFFF));
        sh.setElevation(k.dp(14));
        View handle = new View(c);
        handle.setBackground(grad(0x80FFFFFF, 0x80FFFFFF, k.dp(3), 0, 0));
        sh.addView(handle, new LinearLayout.LayoutParams(k.dp(46), k.dp(5)));
        TextView zk = dhikr(k, text, 28, Color.WHITE, 4);
        zk.setPadding(0, k.dp(14), 0, k.dp(6));
        sh.addView(zk, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sh.addView(k.arabic(AYAH, 14, 0xFFA7E6DD));
        LinearLayout dots = new LinearLayout(c);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, k.dp(12), 0, 0);
        List<View> ds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            View d = new View(c);
            GradientDrawable dg = new GradientDrawable();
            dg.setShape(GradientDrawable.OVAL);
            dg.setColor(0xFF5EEAD4);
            d.setBackground(dg);
            d.setAlpha(0.4f);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(k.dp(8), k.dp(8));
            dp.setMargins(k.dp(4), 0, k.dp(4), 0);
            dots.addView(d, dp);
            ds.add(d);
        }
        sh.addView(dots);
        root.addView(sh, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        s.root = root;
        s.fullWidth = true;
        s.gravity = Gravity.BOTTOM;
        s.enter = () -> {
            sh.setCameraDistance(9000 * k.dp(1));
            sh.setPivotY(sh.getHeight());
            sh.setTranslationY(sh.getHeight() + k.dp(40));
            sh.setRotationX(-30f);
            sh.animate().translationY(0).rotationX(0).setDuration(800).setInterpolator(new OvershootInterpolator(0.9f)).start();
            for (int i = 0; i < ds.size(); i++) {
                ObjectAnimator a = loop(ds.get(i), "alpha", 600, 0.35f, 1f);
                a.setStartDelay(i * 200L);
                a.start();
                s.loops.add(a);
            }
        };
        s.exit = done -> {
            s.stopLoops();
            sh.animate().translationY(sh.getHeight() + k.dp(40)).setDuration(320)
                    .setInterpolator(new AccelerateInterpolator(1.4f)).withEndAction(done).start();
        };
        return s;
    }
}
