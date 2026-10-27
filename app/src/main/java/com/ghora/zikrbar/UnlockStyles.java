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

    static final int LAYERS = 0, PILL = 1, FLIP = 2, PEARL = 3, STAR = 4, ARCH = 5, SHEET = 6;
    /** Six dua-in-a-circle designs (ORB0 .. ORB0+5), each with its own colours and motion. */
    static final int ORB0 = 7, ORB_COUNT = 6, ROTATE = 13;
    static final int REAL = 13;
    static final String[] KEYS = {"us_layers", "us_pill", "us_flip", "us_pearl", "us_star", "us_arch", "us_sheet",
            "us_orb0", "us_orb1", "us_orb2", "us_orb3", "us_orb4", "us_orb5", "us_rotate"};
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

    static Shown build(Context c, int style, String text, Point screen, float mul) {
        Kit k = new Kit(c);
        if (style >= ORB0 && style < ORB0 + ORB_COUNT) return orb(c, k, text, screen, mul, style - ORB0);
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

    /**
     * An endless smooth swing around the current value: it starts exactly where the view is (no jump)
     * and eases in, so the motion never stops or snaps between the entrance and the loop.
     */
    private static ValueAnimator sway(View v, String prop, long period, float amp, long delay) {
        final float base;
        switch (prop) {
            case "rotationX": base = v.getRotationX(); break;
            case "rotationY": base = v.getRotationY(); break;
            case "translationY": base = v.getTranslationY(); break;
            default: base = v.getAlpha(); break;
        }
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(period);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.RESTART);
        a.setInterpolator(new LinearInterpolator());
        final long[] t0 = {-1};
        a.addUpdateListener(an -> {
            long now = android.os.SystemClock.uptimeMillis();
            if (t0[0] < 0) t0[0] = now;
            float ease = Math.min(1f, (now - t0[0]) / 900f);
            float val = base + amp * ease * (float) Math.sin(2 * Math.PI * (float) an.getAnimatedValue());
            switch (prop) {
                case "rotationX": v.setRotationX(val); break;
                case "rotationY": v.setRotationY(val); break;
                case "translationY": v.setTranslationY(val); break;
                default: v.setAlpha(Math.max(0f, Math.min(1f, val))); break;
            }
        });
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
                    ValueAnimator tilt = sway(card, "rotationX", 3200, 6f, 0);
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
            box.postDelayed(() -> {
                ValueAnimator tilt = sway(box, "rotationY", 3000, 10f, 0);
                tilt.start();
                s.loops.add(tilt);
            }, 950);
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
            lamp.setAlpha(0.85f);
            ValueAnimator glow = sway(lamp, "alpha", 2400, 0.15f, 0);
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
    // ------------------------------------------------------------------ dua in a circle (6 colour / motion variants)

    private static final class Spec {
        final int cA, cB, ring, text, s1f, s1s, s2f, s2s, glow;
        Spec(int cA, int cB, int ring, int text, int s1f, int s1s, int s2f, int s2s, int glow) {
            this.cA = cA; this.cB = cB; this.ring = ring; this.text = text;
            this.s1f = s1f; this.s1s = s1s; this.s2f = s2f; this.s2s = s2s; this.glow = glow;
        }
    }

    private static final Spec[] ORB = {
            // 0 midnight gold (the design from the screenshot): navy circle, gold ring, gold + glass squares
            new Spec(0xFF1A2B63, 0xFF0B1433, 0xFFE8C96A, 0xFFFFFFFF, 0x73E8C96A, 0xCCFFE18A, 0x24FFFFFF, 0x88FFFFFF, 0x66FFD25A),
            // 1 emerald and cream
            new Spec(0xFF0B6B4F, 0xFF053226, 0xFFFFE9A8, 0xFFFFF8E1, 0x66FFD45E, 0xCCFFE18A, 0x2E7CF0C8, 0x887CF0C8, 0x5539E0A6),
            // 2 sky and sun
            new Spec(0xFF1E9BE0, 0xFF0A5CB0, 0xFFFFFFFF, 0xFFFFFFFF, 0x8CFFD84D, 0xFFFFE98A, 0x40FFFFFF, 0xCCFFFFFF, 0x6638BDF8),
            // 3 sunrise amber with a green square
            new Spec(0xFFFFCB45, 0xFFFF9F1C, 0xFFFFFFFF, 0xFF3B2400, 0x8CFFFFFF, 0xFFFFFFFF, 0x4D0F9D6B, 0xCC0F9D6B, 0x66FFC93C),
            // 4 teal aurora with a violet square
            new Spec(0xFF0F3B4A, 0xFF06141D, 0xFF5EEAD4, 0xFFE8FFFB, 0x4D5EEAD4, 0xCC5EEAD4, 0x4D7C5CFF, 0xCCA896FF, 0x665EEAD4),
            // 5 ivory with green and sky squares
            new Spec(0xFFFFFDF2, 0xFFF0E9C8, 0xFF0F9D6B, 0xFF0B3D27, 0x590F9D6B, 0xCC0F9D6B, 0x5938BDF8, 0xCC38BDF8, 0x6638BDF8)
    };

    /** A thin circle outline that draws itself like a stopwatch. */
    private static final class SweepRing extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF box = new android.graphics.RectF();
        private float sweep = 360f;

        SweepRing(Context c, int color, float stroke) {
            super(c);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            paint.setStrokeWidth(stroke);
            paint.setColor(color);
        }

        void setSweep(float s) {
            sweep = s;
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas cv) {
            float h = paint.getStrokeWidth() / 2f;
            box.set(h, h, getWidth() - h, getHeight() - h);
            cv.drawArc(box, -90f, sweep, false, paint);
        }
    }

    private static GradientDrawable oval(int a, int b) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{a, b});
        g.setShape(GradientDrawable.OVAL);
        return g;
    }

    /** Endless smooth swing of one property around an explicit base value (eases in, never jumps). */
    private static ValueAnimator swing(View v, String prop, float base, long period, float amp, long delay) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(period);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.RESTART);
        a.setInterpolator(new LinearInterpolator());
        final long[] t0 = {-1};
        a.addUpdateListener(an -> {
            long now = android.os.SystemClock.uptimeMillis();
            if (t0[0] < 0) t0[0] = now;
            float ease = Math.min(1f, (now - t0[0]) / 900f);
            float val = base + amp * ease * (float) Math.sin(2 * Math.PI * (float) an.getAnimatedValue());
            switch (prop) {
                case "rotation": v.setRotation(val); break;
                case "rotationY": v.setRotationY(val); break;
                case "translationY": v.setTranslationY(val); break;
                case "scale": v.setScaleX(val); v.setScaleY(val); break;
                default: v.setAlpha(Math.max(0f, Math.min(1f, val))); break;
            }
        });
        return a;
    }

    private static ValueAnimator ringSweep(SweepRing r, long dur, long delay) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 360f);
        a.setDuration(dur);
        a.setStartDelay(delay);
        a.setInterpolator(new DecelerateInterpolator(1.6f));
        a.addUpdateListener(an -> r.setSweep((float) an.getAnimatedValue()));
        return a;
    }

    private static Shown orb(Context c, Kit k, String text, Point screen, float mul, int idx) {
        final Spec sp = ORB[idx];
        final Shown s = new Shown();
        final float m = Math.min(mul, (screen.x - k.dp(24)) / (float) k.dp(330));
        final int S = k.dp(330 * m), D = k.dp(244 * m);

        FrameLayout root = new FrameLayout(c);
        root.setClipChildren(false);
        final FrameLayout box = new FrameLayout(c);
        box.setClipChildren(false);
        root.addView(box, new FrameLayout.LayoutParams(S, S, Gravity.CENTER));

        // soft light behind everything
        final View glow = new View(c);
        GradientDrawable gg = new GradientDrawable();
        gg.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gg.setGradientRadius(S / 2f);
        gg.setColors(new int[]{sp.glow, 0x00000000});
        glow.setBackground(gg);
        box.addView(glow, new FrameLayout.LayoutParams(S, S, Gravity.CENTER));

        // the two tilted rounded squares
        final View sq1 = new View(c), sq2 = new View(c);
        sq1.setBackground(grad(sp.s1f, sp.s1f, k.dp(66 * m), k.dp(2), sp.s1s));
        sq2.setBackground(grad(sp.s2f, sp.s2f, k.dp(62 * m), k.dp(2), sp.s2s));
        final int a1 = (int) (S * 0.84f), a2 = (int) (S * 0.92f);
        box.addView(sq1, new FrameLayout.LayoutParams(a1, a1, Gravity.CENTER));
        box.addView(sq2, new FrameLayout.LayoutParams(a2, a2, Gravity.CENTER));
        final float r1 = 24f, r2 = -8f;
        sq1.setRotation(r1);
        sq2.setRotation(r2);

        // ripples (only used by two of the motions)
        final View rip1 = new View(c), rip2 = new View(c);
        for (View rp : new View[]{rip1, rip2}) {
            GradientDrawable rg = new GradientDrawable();
            rg.setShape(GradientDrawable.OVAL);
            rg.setColor(0x00000000);
            rg.setStroke(k.dp(2), sp.ring);
            rp.setBackground(rg);
            rp.setAlpha(0f);
            box.addView(rp, new FrameLayout.LayoutParams(D, D, Gravity.CENTER));
        }

        // circle + ring
        final FrameLayout core = new FrameLayout(c);
        core.setClipChildren(false);
        box.addView(core, new FrameLayout.LayoutParams(S, S, Gravity.CENTER));
        final FrameLayout disc = new FrameLayout(c);
        disc.setBackground(oval(sp.cA, sp.cB));
        disc.setElevation(k.dp(12));
        disc.setClipToOutline(true);
        core.addView(disc, new FrameLayout.LayoutParams(D, D, Gravity.CENTER));
        final SweepRing ring = new SweepRing(c, sp.ring, k.dp(3.4f));
        core.addView(ring, new FrameLayout.LayoutParams(D + k.dp(2), D + k.dp(2), Gravity.CENTER));

        final TextView zk = dhikr(k, text, 25 * m, sp.text, 7);
        zk.setGravity(Gravity.CENTER);
        zk.setMaxWidth((int) (D * 0.76f));
        FrameLayout.LayoutParams zp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        disc.addView(zk, zp);

        final View shine = new View(c);
        shine.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF}));
        shine.setRotation(18f);
        shine.setAlpha(idx == 4 ? 1f : 0f);
        disc.addView(shine, new FrameLayout.LayoutParams((int) (D * 0.3f), D * 2, Gravity.CENTER_VERTICAL));

        // little stars around
        final List<TextView> sparks = new ArrayList<>();
        int[][] pos = {{Gravity.TOP | Gravity.START, 30, 30}, {Gravity.TOP | Gravity.END, 18, 56}, {Gravity.BOTTOM | Gravity.START, 22, 40}, {Gravity.BOTTOM | Gravity.END, 40, 22}};
        for (int i = 0; i < 4; i++) {
            TextView st = k.strong("✦", 15 - i, sp.ring);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, pos[i][0]);
            lp.setMargins(k.dp(pos[i][1]), k.dp(pos[i][2]), k.dp(pos[i][1]), k.dp(pos[i][2]));
            st.setAlpha(0f);
            box.addView(st, lp);
            sparks.add(st);
        }

        s.root = root;
        s.hold = 2400 + Math.min(2600, text.length() * 28L);
        final float cam = 9000 * k.dp(1);
        s.enter = () -> {
            box.setCameraDistance(cam);
            core.setCameraDistance(cam);
            final OvershootInterpolator over = new OvershootInterpolator(1.2f);
            // start state
            glow.setAlpha(0f);
            zk.setAlpha(0f);
            ring.setSweep(0f);
            for (View v : new View[]{sq1, sq2}) {
                v.setAlpha(0f);
                v.setScaleX(0.3f);
                v.setScaleY(0.3f);
            }
            core.setAlpha(0f);
            core.setScaleX(0.4f);
            core.setScaleY(0.4f);

            ValueAnimator rs = ringSweep(ring, 850, 250);
            rs.start();
            s.loops.add(rs);
            glow.animate().alpha(1f).setDuration(900).start();
            zk.animate().alpha(1f).setStartDelay(520).setDuration(420).start();

            switch (idx) {
                case 0: { // bloom: squares unfold from inside the circle
                    sq1.setRotation(r1 - 120f);
                    sq2.setRotation(r2 + 120f);
                    sq1.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(r1).setDuration(950).setInterpolator(over).start();
                    sq2.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(r2).setStartDelay(90).setDuration(950).setInterpolator(over).start();
                    core.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(100).setDuration(720).setInterpolator(new OvershootInterpolator(1.4f)).start();
                    s.loops.add(swing(sq1, "rotation", r1, 5200, 4f, 1000));
                    s.loops.add(swing(sq2, "rotation", r2, 6400, -4f, 1000));
                    s.loops.add(swing(glow, "alpha", 0.8f, 2200, 0.2f, 1000));
                    break;
                }
                case 1: { // orbit: the squares keep turning slowly
                    sq1.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start();
                    sq2.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(120).setDuration(600).start();
                    core.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650).setInterpolator(new OvershootInterpolator(1.5f)).start();
                    ObjectAnimator o1 = ObjectAnimator.ofFloat(sq1, "rotation", r1, r1 + 360f);
                    ObjectAnimator o2 = ObjectAnimator.ofFloat(sq2, "rotation", r2, r2 - 360f);
                    o1.setDuration(15000);
                    o2.setDuration(21000);
                    for (ObjectAnimator o : new ObjectAnimator[]{o1, o2}) {
                        o.setRepeatCount(ValueAnimator.INFINITE);
                        o.setInterpolator(new LinearInterpolator());
                        o.start();
                        s.loops.add(o);
                    }
                    s.loops.add(swing(core, "translationY", 0f, 2600, k.dp(5), 700));
                    break;
                }
                case 2: { // 3D turn
                    box.setRotationY(-90f);
                    box.setAlpha(0f);
                    box.animate().rotationY(0f).alpha(1f).setDuration(950).setInterpolator(new OvershootInterpolator(0.9f)).start();
                    sq1.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start();
                    sq2.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start();
                    core.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).setInterpolator(over).start();
                    s.loops.add(swing(box, "rotationY", 0f, 3600, 8f, 1000));
                    s.loops.add(swing(sq1, "rotation", r1, 5600, 5f, 1000));
                    break;
                }
                case 3: { // drop with a bounce, then a ripple
                    core.setScaleX(1f);
                    core.setScaleY(1f);
                    core.setAlpha(1f);
                    core.setTranslationY(-screen.y * 0.5f);
                    core.animate().translationY(0f).setDuration(1000).setInterpolator(new android.view.animation.BounceInterpolator()).start();
                    sq1.setRotation(0f);
                    sq2.setRotation(0f);
                    sq1.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(r1).setStartDelay(520).setDuration(800).setInterpolator(over).start();
                    sq2.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(r2).setStartDelay(600).setDuration(800).setInterpolator(over).start();
                    rip1.setScaleX(1f);
                    rip1.setScaleY(1f);
                    rip1.setAlpha(0.7f);
                    rip1.animate().scaleX(1.9f).scaleY(1.9f).alpha(0f).setStartDelay(620).setDuration(900).start();
                    s.loops.add(swing(core, "translationY", 0f, 2400, k.dp(4), 1300));
                    s.loops.add(swing(sq1, "rotation", r1, 5000, 4f, 1400));
                    break;
                }
                case 4: { // zoom out of the distance + a light that sweeps over the circle
                    box.setScaleX(1.9f);
                    box.setScaleY(1.9f);
                    box.setAlpha(0f);
                    box.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(900).setInterpolator(new DecelerateInterpolator(2f)).start();
                    sq1.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start();
                    sq2.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start();
                    core.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start();
                    ObjectAnimator sw = ObjectAnimator.ofFloat(shine, "translationX", -D * 0.4f, D * 1.1f);
                    sw.setDuration(1500);
                    sw.setStartDelay(800);
                    sw.setRepeatCount(ValueAnimator.INFINITE);
                    sw.setInterpolator(new DecelerateInterpolator());
                    sw.start();
                    s.loops.add(sw);
                    ObjectAnimator o1 = ObjectAnimator.ofFloat(sq1, "rotation", r1, r1 - 360f);
                    o1.setDuration(30000);
                    o1.setRepeatCount(ValueAnimator.INFINITE);
                    o1.setInterpolator(new LinearInterpolator());
                    o1.start();
                    s.loops.add(o1);
                    s.loops.add(swing(sq2, "rotation", r2, 6000, 5f, 1000));
                    break;
                }
                default: { // breathing with ripples
                    sq1.setRotation(r1 - 45f);
                    sq2.setRotation(r2 + 45f);
                    sq1.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(r1).setDuration(800).setInterpolator(over).start();
                    sq2.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(r2).setStartDelay(100).setDuration(800).setInterpolator(over).start();
                    core.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650).setInterpolator(new OvershootInterpolator(1.6f)).start();
                    View[] rips = {rip1, rip2};
                    for (int i = 0; i < 2; i++) {
                        final View rp = rips[i];
                        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
                        va.setDuration(2000);
                        va.setStartDelay(600 + i * 1000L);
                        va.setRepeatCount(ValueAnimator.INFINITE);
                        va.setInterpolator(new DecelerateInterpolator());
                        va.addUpdateListener(an -> {
                            float f = (float) an.getAnimatedValue();
                            rp.setScaleX(1f + 0.6f * f);
                            rp.setScaleY(1f + 0.6f * f);
                            rp.setAlpha(0.55f * (1f - f));
                        });
                        va.start();
                        s.loops.add(va);
                    }
                    s.loops.add(swing(core, "scale", 1f, 2400, 0.025f, 1000));
                    s.loops.add(swing(sq1, "scale", 1f, 3000, 0.03f, 1000));
                    break;
                }
            }
            // twinkling stars
            for (int i = 0; i < sparks.size(); i++) {
                ObjectAnimator tw = ObjectAnimator.ofFloat(sparks.get(i), "alpha", 0f, 1f);
                tw.setDuration(900);
                tw.setStartDelay(700 + i * 260L);
                tw.setRepeatCount(ValueAnimator.INFINITE);
                tw.setRepeatMode(ValueAnimator.REVERSE);
                tw.start();
                s.loops.add(tw);
            }
        };
        s.exit = done -> {
            s.stopLoops();
            if (idx == 2) {
                box.animate().rotationY(80f).alpha(0f).setDuration(320).setInterpolator(new AccelerateInterpolator()).withEndAction(done).start();
            } else {
                box.animate().scaleX(1.18f).scaleY(1.18f).alpha(0f).setDuration(300).setInterpolator(new AccelerateInterpolator()).withEndAction(done).start();
            }
        };
        return s;
    }
}
