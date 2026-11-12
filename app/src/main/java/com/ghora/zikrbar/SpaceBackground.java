package com.ghora.zikrbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/** A calm animated night-sky: drifting nebula, twinkling stars and a glowing sun with planets circling it. */
final class SpaceBackground extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] sx = new float[90], sy = new float[90], sr = new float[90], sph = new float[90];
    // planets: orbit size (fraction of width), speed, phase, radius (dp), color, ring
    private static final float[][] PLANETS = {
            {0.20f, 0.55f, 0.5f, 3.5f, 0}, {0.32f, 0.38f, 2.1f, 5f, 1}, {0.45f, 0.27f, 4.0f, 4f, 2},
            {0.58f, 0.19f, 1.2f, 7f, 3}, {0.72f, 0.13f, 5.3f, 5f, 4}};
    private static final int[] PCOL = {0xFF8FB3CC, 0xFFE9A35A, 0xFFB9C4CC, 0xFFD9A76A, 0xFF7DA0B8};
    private final long t0 = SystemClock.uptimeMillis();
    private final float d;

    SpaceBackground(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        Random r = new Random(7);
        for (int i = 0; i < sx.length; i++) {
            sx[i] = r.nextFloat();
            sy[i] = r.nextFloat() * 0.8f;
            sr[i] = 0.4f + r.nextFloat() * 1.3f;
            sph[i] = r.nextFloat() * 6.28f;
        }
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    private void blob(Canvas c, float x, float y, float rad, int col) {
        p.setShader(new RadialGradient(x, y, rad, col, col & 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, rad, p);
        p.setShader(null);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float t = (SystemClock.uptimeMillis() - t0) / 1000f;

        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, 0, h, new int[]{0xFF04070D, 0xFF13202B, 0xFF272B31, 0xFF4A2D1C},
                new float[]{0f, 0.45f, 0.75f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        // nebula
        blob(c, w * (0.28f + 0.05f * (float) Math.sin(t * 0.21f)), h * 0.20f, w * 0.85f, 0x66B2622A);
        blob(c, w * (0.85f + 0.04f * (float) Math.cos(t * 0.17f)), h * 0.38f, w * 0.75f, 0x5538586A);
        blob(c, w * 0.15f, h * 0.55f, w * 0.65f, 0x4438586A);
        blob(c, w * 0.5f, h * 0.9f, w * 1.0f, 0x66E8541C);

        // stars
        p.setColor(0xFFFFFFFF);
        for (int i = 0; i < sx.length; i++) {
            float tw = 0.45f + 0.55f * (float) Math.sin(t * (0.8f + (i % 5) * 0.35f) + sph[i]);
            p.setAlpha((int) (200 * Math.max(0.1f, tw)));
            c.drawCircle(sx[i] * w, sy[i] * h, sr[i] * d * 0.8f, p);
        }
        p.setAlpha(255);

        // sun with slowly turning rays
        float cx = w / 2f, cy = h * 0.90f, sun = w * 0.05f;
        blob(c, cx, cy, w * 0.42f, 0x55FF7A2E);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(d * 1.2f);
        for (int i = 0; i < 24; i++) {
            float a = (float) (i * Math.PI * 2 / 24 + t * 0.07f);
            float len = w * (i % 2 == 0 ? 0.36f : 0.24f);
            p.setColor(i % 2 == 0 ? 0x33FFB27A : 0x22FFB27A);
            c.drawLine(cx + (float) Math.cos(a) * sun, cy + (float) Math.sin(a) * sun * 0.6f,
                    cx + (float) Math.cos(a) * len, cy + (float) Math.sin(a) * len * 0.55f, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx, cy, sun * 1.6f, new int[]{0xFFFFF4D6, 0xFFFF9A3C, 0xFFE8541C, 0x00E8541C},
                new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, sun * 1.6f, p);
        p.setShader(null);

        // orbits and planets
        for (float[] pl : PLANETS) {
            float rx = w * pl[0], ry = rx * 0.27f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(d * 0.8f);
            p.setColor(0x1FFFFFFF);
            c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, p);
            float a = t * pl[1] * 0.5f + pl[2];
            float px = cx + rx * (float) Math.cos(a), py = cy + ry * (float) Math.sin(a);
            float near = 0.8f + 0.35f * (float) Math.sin(a);
            float rad = pl[3] * d * near;
            int col = PCOL[(int) pl[4]];
            p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(px - rad * 0.35f, py - rad * 0.35f, rad * 1.4f,
                    new int[]{0xFFFFFFFF, col, 0xFF1B2630}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(px, py, rad, p);
            p.setShader(null);
            if (pl[4] == 3 || pl[4] == 1) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(d * 1.1f);
                p.setColor(0x88FFE2B0);
                c.drawOval(px - rad * 1.9f, py - rad * 0.55f, px + rad * 1.9f, py + rad * 0.55f, p);
            }
        }
        p.setStyle(Paint.Style.FILL);
        postInvalidateOnAnimation();
    }
}
