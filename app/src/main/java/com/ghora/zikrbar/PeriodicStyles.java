package com.ghora.zikrbar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Six animated dhikr popups that appear over any app every few minutes (see Prefs.popupEvery). */
final class PeriodicStyles {

    static final int COUNT = 6;
    static final String[] KEYS = {"pp_0", "pp_1", "pp_2", "pp_3", "pp_4", "pp_5"};
    static final int[] EVERY = {1, 5, 15, 30, 60};

    private PeriodicStyles() {}

    static UnlockStyles.Shown build(Context c, int style, String text, Point screen) {
        Kit k = new Kit(c);
        switch (style) {
            case 1: return island(c, k, text, screen);
            case 2: return bubble(c, k, text, screen);
            case 3: return glass(c, k, text, screen);
            case 4: return orbit(c, k, text, screen);
            case 5: return ribbon(c, k, text, screen);
            default: return bloom(c, k, text, screen);
        }
    }

    private static ValueAnimator repeat(UnlockStyles.Shown s, long ms, float from, float to, ValueAnimator.AnimatorUpdateListener l) {
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(ms);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(new LinearInterpolator());
        a.addUpdateListener(l);
        s.loops.add(a);
        a.start();
        return a;
    }

    private static GradientDrawable round(int color, float r, int strokePx, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r);
        if (strokePx > 0) g.setStroke(strokePx, stroke);
        return g;
    }

    // 1 ---------------------------------------------------------------- bloom circle
    private static UnlockStyles.Shown bloom(Context c, Kit k, String text, Point screen) {
        UnlockStyles.Shown s = new UnlockStyles.Shown();
        int D = Math.min(k.dp(230), screen.x - k.dp(40));
        FrameLayout root = new FrameLayout(c);
        root.setClipChildren(false);
        FrameLayout box = new FrameLayout(c);
        box.setClipChildren(false);
        root.addView(box, new FrameLayout.LayoutParams(D + k.dp(80), D + k.dp(80), Gravity.CENTER));
        View[] rip = new View[2];
        for (int i = 0; i < 2; i++) {
            rip[i] = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setStroke(k.dp(2), 0xAAE7C35E);
            rip[i].setBackground(g);
            rip[i].setAlpha(0f);
            box.addView(rip[i], new FrameLayout.LayoutParams(D, D, Gravity.CENTER));
        }
        FrameLayout disc = new FrameLayout(c);
        GradientDrawable dg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF25418A, 0xFF0A1638});
        dg.setShape(GradientDrawable.OVAL);
        dg.setStroke(k.dp(3), 0xFFE7C35E);
        disc.setBackground(dg);
        disc.setElevation(k.dp(14));
        TextView zk = UnlockStyles.dhikr(k, text, 28, Color.WHITE, 4);
        zk.setMaxWidth((int) (D * 0.76f));
        disc.addView(zk, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        box.addView(disc, new FrameLayout.LayoutParams(D, D, Gravity.CENTER));
        s.root = root;
        s.hold = 3000;
        s.enter = () -> {
            disc.setScaleX(0.2f); disc.setScaleY(0.2f); disc.setRotation(-40f); disc.setAlpha(0f);
            disc.animate().scaleX(1f).scaleY(1f).rotation(0f).alpha(1f).setDuration(650)
                    .setInterpolator(new OvershootInterpolator(1.5f)).start();
            for (int i = 0; i < 2; i++) {
                final View r = rip[i];
                final float off = i * 0.5f;
                repeat(s, 2400, 0f, 1f, a -> {
                    float f = ((float) a.getAnimatedValue() + off) % 1f;
                    r.setScaleX(0.9f + f * 0.55f);
                    r.setScaleY(0.9f + f * 0.55f);
                    r.setAlpha(0.8f * (1f - f));
                });
            }
        };
        s.exit = done -> disc.animate().scaleX(0.4f).scaleY(0.4f).alpha(0f).rotation(30f).setDuration(350)
                .setInterpolator(new AccelerateInterpolator()).withEndAction(done).start();
        return s;
    }

    // 2 ---------------------------------------------------------------- island capsule
    private static UnlockStyles.Shown island(Context c, Kit k, String text, Point screen) {
        UnlockStyles.Shown s = new UnlockStyles.Shown();
        FrameLayout root = new FrameLayout(c);
        root.setPadding(k.dp(10), k.dp(8), k.dp(10), k.dp(14));
        FrameLayout cap = new FrameLayout(c);
        cap.setBackground(round(0xFF05060A, k.dp(40), k.dp(1), 0x55E7C35E));
        cap.setElevation(k.dp(12));
        TextView mark = k.arabic("ﷲ", 20, 0xFFF0C85A);
        cap.addView(mark, new FrameLayout.LayoutParams(k.dp(44), k.dp(44), Gravity.CENTER_VERTICAL | Gravity.START));
        TextView zk = UnlockStyles.dhikr(k, text, 21, 0xFFF0C85A, 2);
        zk.setAlpha(0f);
        zk.setPadding(k.dp(52), k.dp(8), k.dp(18), k.dp(8));
        int full = screen.x - k.dp(20);
        zk.setMaxWidth(full);
        cap.addView(zk, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        root.addView(cap, new FrameLayout.LayoutParams(k.dp(44), k.dp(44), Gravity.CENTER_HORIZONTAL));
        s.root = root;
        s.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        s.y = k.dp(24);
        s.hold = 2800;
        s.enter = () -> {
            ValueAnimator g = ValueAnimator.ofFloat(0f, 1f);
            g.setDuration(650);
            g.setInterpolator(new OvershootInterpolator(0.9f));
            int target = Math.min(full, k.dp(340));
            g.addUpdateListener(a -> {
                float f = (float) a.getAnimatedValue();
                ViewGroup.LayoutParams lp = cap.getLayoutParams();
                lp.width = (int) (k.dp(44) + (target - k.dp(44)) * f);
                lp.height = (int) (k.dp(44) + (k.dp(78) - k.dp(44)) * Math.min(1f, f));
                cap.setLayoutParams(lp);
            });
            cap.setTranslationY(-k.dp(70));
            cap.animate().translationY(0).setDuration(380).start();
            g.setStartDelay(300);
            g.start();
            s.loops.add(g);
            zk.animate().alpha(1f).setStartDelay(750).setDuration(350).start();
        };
        s.exit = done -> cap.animate().translationY(-k.dp(110)).alpha(0f).setDuration(320)
                .setInterpolator(new AccelerateInterpolator()).withEndAction(done).start();
        return s;
    }

    // 3 ---------------------------------------------------------------- bubble that opens into a card
    private static UnlockStyles.Shown bubble(Context c, Kit k, String text, Point screen) {
        UnlockStyles.Shown s = new UnlockStyles.Shown();
        FrameLayout root = new FrameLayout(c);
        root.setPadding(k.dp(8), k.dp(8), k.dp(8), k.dp(8));
        FrameLayout b = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF4FE39A, 0xFF0F8A52});
        bg.setCornerRadius(k.dp(40));
        b.setBackground(bg);
        b.setElevation(k.dp(12));
        TextView mark = k.arabic("ﷲ", 26, Color.WHITE);
        b.addView(mark, new FrameLayout.LayoutParams(k.dp(68), k.dp(68), Gravity.CENTER_VERTICAL | Gravity.END));
        TextView zk = UnlockStyles.dhikr(k, text, 22, Color.WHITE, 2);
        zk.setAlpha(0f);
        zk.setPadding(k.dp(18), k.dp(8), k.dp(78), k.dp(8));
        zk.setMaxWidth(screen.x - k.dp(30));
        b.addView(zk, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_VERTICAL | Gravity.START));
        root.addView(b, new FrameLayout.LayoutParams(k.dp(68), k.dp(68), Gravity.END));
        s.root = root;
        s.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        s.hold = 2800;
        s.enter = () -> {
            b.setTranslationX(k.dp(120));
            b.animate().translationX(0).setDuration(520).setInterpolator(new OvershootInterpolator(1.6f)).start();
            ValueAnimator g = ValueAnimator.ofFloat(0f, 1f);
            g.setStartDelay(650);
            g.setDuration(650);
            g.setInterpolator(new DecelerateInterpolator(2f));
            int target = Math.min(screen.x - k.dp(30), k.dp(340));
            g.addUpdateListener(a -> {
                ViewGroup.LayoutParams lp = b.getLayoutParams();
                lp.width = (int) (k.dp(68) + (target - k.dp(68)) * (float) a.getAnimatedValue());
                lp.height = (int) (k.dp(68) + k.dp(30) * (float) a.getAnimatedValue());
                b.setLayoutParams(lp);
            });
            g.start();
            s.loops.add(g);
            zk.animate().alpha(1f).setStartDelay(1100).setDuration(350).start();
        };
        s.exit = done -> b.animate().translationX(k.dp(380)).setDuration(380)
                .setInterpolator(new AccelerateInterpolator()).withEndAction(done).start();
        return s;
    }

    // 4 ---------------------------------------------------------------- frosted glass sheet
    private static UnlockStyles.Shown glass(Context c, Kit k, String text, Point screen) {
        UnlockStyles.Shown s = new UnlockStyles.Shown();
        FrameLayout root = new FrameLayout(c);
        root.setPadding(k.dp(14), k.dp(10), k.dp(14), k.dp(30));
        FrameLayout sheet = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xE62E4A9A, 0xE60B1A44});
        bg.setCornerRadius(k.dp(28));
        bg.setStroke(k.dp(1.5f), 0x99FFFFFF);
        sheet.setBackground(bg);
        sheet.setElevation(k.dp(14));
        sheet.setClipToOutline(true);
        TextView zk = UnlockStyles.dhikr(k, text, 26, Color.WHITE, 3);
        zk.setPadding(k.dp(22), k.dp(30), k.dp(22), k.dp(30));
        sheet.addView(zk, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        View shine = new View(c);
        shine.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF}));
        shine.setRotation(16f);
        sheet.addView(shine, new FrameLayout.LayoutParams(k.dp(70), k.dp(400), Gravity.CENTER_VERTICAL));
        root.addView(sheet, new FrameLayout.LayoutParams(Math.min(screen.x - k.dp(28), k.dp(380)), -2, Gravity.CENTER));
        s.root = root;
        s.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        s.hold = 2800;
        s.enter = () -> {
            sheet.setTranslationY(k.dp(260));
            sheet.animate().translationY(0).setDuration(600).setInterpolator(new OvershootInterpolator(1.1f)).start();
            int w = Math.min(screen.x - k.dp(28), k.dp(380));
            repeat(s, 1800, -0.3f, 1.3f, a -> shine.setTranslationX(w * (float) a.getAnimatedValue()));
        };
        s.exit = done -> sheet.animate().translationY(k.dp(260)).alpha(0f).setDuration(380)
                .setInterpolator(new AccelerateInterpolator()).withEndAction(done).start();
        return s;
    }

    // 5 ---------------------------------------------------------------- orbit rings
    private static UnlockStyles.Shown orbit(Context c, Kit k, String text, Point screen) {
        UnlockStyles.Shown s = new UnlockStyles.Shown();
        int D = Math.min(k.dp(300), screen.x - k.dp(30));
        FrameLayout root = new FrameLayout(c);
        FrameLayout box = new FrameLayout(c);
        root.addView(box, new FrameLayout.LayoutParams(D, D, Gravity.CENTER));
        GradientDrawable core = new GradientDrawable();
        core.setShape(GradientDrawable.OVAL);
        core.setColor(0xCC0B1636);
        box.addView(new View(c) {{ setBackground(core); }}, new FrameLayout.LayoutParams((int) (D * 0.78f), (int) (D * 0.78f), Gravity.CENTER));
        View[] rings = new View[2];
        int[] cols = {0xFFFFE27A, 0xAAFFFFFF};
        for (int i = 0; i < 2; i++) {
            rings[i] = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setStroke(k.dp(2.5f), cols[i], k.dp(2), k.dp(8));
            rings[i].setBackground(g);
            int size = i == 0 ? D : (int) (D * 0.84f);
            box.addView(rings[i], new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        }
        TextView zk = UnlockStyles.dhikr(k, text, 26, Color.WHITE, 3);
        zk.setMaxWidth((int) (D * 0.6f));
        zk.setShadowLayer(k.dp(8), 0, 0, 0xCCFFE27A);
        box.addView(zk, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        s.root = root;
        s.hold = 3000;
        s.enter = () -> {
            box.setScaleX(0.5f); box.setScaleY(0.5f); box.setAlpha(0f);
            box.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(550).setInterpolator(new OvershootInterpolator(1.2f)).start();
            repeat(s, 6000, 0f, 360f, a -> rings[0].setRotation((float) a.getAnimatedValue()));
            repeat(s, 9000, 0f, 360f, a -> rings[1].setRotation(-(float) a.getAnimatedValue()));
        };
        s.exit = done -> box.animate().scaleX(1.25f).scaleY(1.25f).alpha(0f).setDuration(350).withEndAction(done).start();
        return s;
    }

    // 6 ---------------------------------------------------------------- gold ribbon
    private static UnlockStyles.Shown ribbon(Context c, Kit k, String text, Point screen) {
        UnlockStyles.Shown s = new UnlockStyles.Shown();
        FrameLayout band = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF0C5A37, 0xFF16A163, 0xFF0C5A37});
        bg.setStroke(k.dp(2), 0xFFFFE9A3);
        band.setBackground(bg);
        band.setElevation(k.dp(12));
        band.setClipChildren(true);
        TextView zk = UnlockStyles.dhikr(k, text, 26, Color.WHITE, 2);
        zk.setPadding(k.dp(20), k.dp(16), k.dp(20), k.dp(16));
        band.addView(zk, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        View shine = new View(c);
        shine.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x00FFE9A3, 0xAAFFE9A3, 0x00FFE9A3}));
        band.addView(shine, new FrameLayout.LayoutParams(k.dp(90), -1));
        s.root = band;
        s.fullWidth = true;
        s.gravity = Gravity.CENTER;
        s.hold = 2800;
        s.enter = () -> {
            band.setTranslationX(-screen.x);
            band.animate().translationX(0).setDuration(600).setInterpolator(new DecelerateInterpolator(2f)).start();
            repeat(s, 1800, -0.2f, 1.2f, a -> shine.setTranslationX(screen.x * (float) a.getAnimatedValue()));
        };
        s.exit = done -> band.animate().translationX(screen.x).setDuration(420)
                .setInterpolator(new AccelerateInterpolator()).withEndAction(done).start();
        return s;
    }
}
