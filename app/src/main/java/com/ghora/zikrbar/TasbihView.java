package com.ghora.zikrbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/** Draws the floating tasbih counter: ring counters, circles, a pill, and side-view rollers (10–14). */
public class TasbihView extends View {

    /** The designs offered, in picker order (ids; 6–8 are the white, gold and black ring counters). */
    static final int[] STYLES = {15, 16, 17, 6, 7, 8, 0, 2, 3, 10, 11, 12, 13, 14};
    static final int COUNT = STYLES.length;

    static String name(int id) {
        return L.t("d" + id);
    }

    /** Old saved designs that were removed fall back to the first one. */
    static int valid(int id) {
        for (int s : STYLES) if (s == id) return id;
        return STYLES[0];
    }

    // ring-counter bitmaps (680 x 840 px; body 600 x 760 at 40,40)
    private static final int[] RING_RES = {R.drawable.tasbih_white, R.drawable.tasbih_gold, R.drawable.tasbih_black};
    private static final android.graphics.Bitmap[] RING_BMP = new android.graphics.Bitmap[3];
    private static final float BMP_W = 680, BMP_H = 840;
    private static final float LCD_L = 40 + 0.17f * 600 + 16, LCD_R = 40 + 0.83f * 600 - 16;
    private static final float LCD_T = 40 + 0.13f * 760 + 16, LCD_B = 40 + 0.36f * 760 - 16;

    private static final float PAD = 10;
    /** Width of the column on the left that holds the "change dhikr" button. */
    private static final float BTN = 0, GAP = 0;

    private int style;
    private String text = "";
    private int count;
    private int target = 33;

    private final float density;
    private float d;
    private final Typeface amiriBold;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint np = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    /** Where the "change dhikr" button is, in view coordinates (for touch). */
    private final RectF btn = new RectF();

    public TasbihView(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        d = density;
        amiriBold = Ui.amiriBold(c);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    /** Makes the whole design bigger or smaller (1 = normal). */
    public void setSize(float scale) {
        float nd = density * scale;
        if (nd == d) return;
        d = nd;
        requestLayout();
        invalidate();
    }

    public void setState(int style, String text, int count, int target) {
        style = valid(style);
        boolean relayout = style != this.style || !text.equals(this.text);
        this.style = style;
        this.text = text;
        this.count = count;
        this.target = target;
        if (relayout) requestLayout();
        invalidate();
    }

    private float dp(float v) {
        return v * d;
    }

    private int goal() {
        return target > 0 ? target : 33;
    }

    private int inRound() {
        return count == 0 ? 0 : ((count - 1) % goal()) + 1;
    }

    // ---------- text helpers ----------
    private void zk(float sizeDp, int color) {
        tp.setTypeface(amiriBold);
        tp.setTextSize(dp(sizeDp));
        tp.setColor(color);
    }

    private String fit(float maxW) {
        return TextUtils.ellipsize(text, tp, maxW, TextUtils.TruncateAt.END).toString();
    }

    private float pillH(float sizeDp) {
        return dp(sizeDp) * 2f;
    }

    private float pillW(float sizeDp, float maxW) {
        zk(sizeDp, 0);
        return Math.min(tp.measureText(text), maxW) + dp(28);
    }

    private static void centerText(Canvas c, Paint paint, String s, float cx, float cy) {
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = paint.getFontMetrics();
        c.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, paint);
    }

    private void pill(Canvas c, float cx, float top, float sizeDp, float maxW, int bg, int fg) {
        zk(sizeDp, fg);
        String s = fit(maxW);
        float w = tp.measureText(s) + dp(28);
        float h = pillH(sizeDp);
        r.set(cx - w / 2, top, cx + w / 2, top + h);
        textArea.set(r);
        textArea.offset(dp(PAD), dp(PAD));
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(bg);
        p.setShadowLayer(dp(4), 0, dp(2), 0x33000000);
        c.drawRoundRect(r, h / 2, h / 2, p);
        p.clearShadowLayer();
        centerText(c, tp, s, cx, top + h / 2);
    }

    private void number(float sizeDp, int color, boolean mono) {
        np.setTypeface(mono ? Typeface.MONOSPACE : Typeface.DEFAULT_BOLD);
        np.setTextSize(dp(sizeDp));
        np.setColor(color);
    }

    private void fillCircle(Canvas c, float cx, float cy, float rad, int color, boolean shadow) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        if (shadow) p.setShadowLayer(dp(8), 0, dp(4), 0x55000000);
        c.drawCircle(cx, cy, rad, p);
        p.clearShadowLayer();
    }

    // ---------- size ----------
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float maxPill = dp(220);
        float w, h;
        switch (style) {
            case 0:
                w = Math.max(dp(132), pillW(15, maxPill));
                h = pillH(15) + dp(8) + dp(122);
                break;
            case 1:
                w = Math.max(dp(100), pillW(15, maxPill));
                h = pillH(15) + dp(8) + dp(96);
                break;
            case 2:
                w = Math.max(dp(154), pillW(16, maxPill));
                h = pillH(16) + dp(10) + dp(150);
                break;
            case 3:
                w = dp(280);
                h = dp(60);
                break;
            case 6:
            case 7:
            case 8: {
                float sc = ringScale();
                w = Math.max(BMP_W * sc, pillW(15, maxPill));
                h = pillH(15) + dp(2) + BMP_H * sc;
                break;
            }
            case 15:
            case 16:
            case 17:
                w = Math.max(dp(170), pillW(15, maxPill));
                h = pillH(15) + dp(6) + dp(236);
                break;
            case 4:
                w = dp(200);
                h = dp(196);
                break;
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
                w = Math.max(dp(84), pillW(14, dp(200)));
                h = pillH(14) + dp(8) + dp(RH) + dp(8) + dp(26) + dp(6) + dp(16);
                break;
            default:
                zk(14, 0);
                w = Math.min(tp.measureText(text), dp(180)) + dp(20) + dp(8) + dp(60);
                h = dp(60);
        }
        float pad = dp(PAD) * 2;
        setMeasuredDimension((int) Math.ceil(w + pad + dp(BTN + GAP)), (int) Math.ceil(h + pad));
    }

    // ---------- drawing ----------
    @Override
    protected void onDraw(Canvas c) {
        textArea.set(0, 0, getWidth(), getHeight() * 0.38f);
        c.save();
        c.translate(dp(PAD) + dp(BTN + GAP), dp(PAD));
        float w = getWidth() - dp(PAD) * 2 - dp(BTN + GAP);
        switch (style) {
            case 0: drawRing(c, w); break;
            case 1: drawOrb(c, w); break;
            case 2: drawBeads(c, w); break;
            case 3: drawPill(c, w); break;
            case 4: drawGold(c, w); break;
            case 15:
            case 16:
            case 17: drawTally(c, w, style - 15); break;
            case 6:
            case 7:
            case 8: drawRingCounter(c, w, style - 6); break;
            case 10:
            case 11:
            case 12:
            case 13:
            case 14: drawRoller(c, w); break;
            default: drawHead(c, w);
        }
        c.restore();
    }

    private final RectF textArea = new RectF();

    /** True when (x, y) — in view coordinates — is on the dhikr text; a sideways swipe there changes the dhikr. */
    public boolean isTextHit(float x, float y) {
        RectF t = new RectF(textArea);
        t.inset(-dp(10), -dp(10));
        return t.contains(x, y);
    }

    private void arrowHead(Canvas c, float cx, float cy, float ar, float deg) {
        double a = Math.toRadians(deg);
        float x = cx + ar * (float) Math.cos(a), y = cy + ar * (float) Math.sin(a);
        // tangent direction (clockwise)
        float tx = (float) -Math.sin(a), ty = (float) Math.cos(a);
        float nx = (float) Math.cos(a), ny = (float) Math.sin(a);
        float len = dp(4.2f), wid = dp(3.4f);
        path.reset();
        path.moveTo(x + tx * len, y + ty * len);
        path.lineTo(x + nx * wid, y + ny * wid);
        path.lineTo(x - nx * wid, y - ny * wid);
        path.close();
        c.drawPath(path, p);
    }

    // ---------- white / gold / black finger-ring counters ----------
    private float ringScale() {
        return dp(124) / 600f;
    }

    private android.graphics.Bitmap ringBitmap(int i) {
        if (RING_BMP[i] == null) {
            RING_BMP[i] = android.graphics.BitmapFactory.decodeResource(getResources(), RING_RES[i]);
        }
        return RING_BMP[i];
    }

    private final Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private void drawRingCounter(Canvas c, float w, int i) {
        float cx = w / 2;
        pill(c, cx, 0, 15, dp(220), i == 0 ? 0xE6202124 : 0xB0000000, Color.WHITE);
        float sc = ringScale();
        float left = cx - BMP_W * sc / 2, top = pillH(15) + dp(2);
        android.graphics.Bitmap b = ringBitmap(i);
        if (b != null) {
            r.set(left, top, left + BMP_W * sc, top + BMP_H * sc);
            c.drawBitmap(b, null, r, bp);
        }
        RectF lcd = new RectF(left + LCD_L * sc, top + LCD_T * sc, left + LCD_R * sc, top + LCD_B * sc);
        sevenSeg(c, String.valueOf(count), lcd, 0xFF2B322A);
    }


    // body top, body bottom, button top, button bottom, glow
    private static final int[][] TALLY = {
            {0xFF4FA3FF, 0xFF0B4CC4, 0xFF06307F, 0xFF59B6FF},
            {0xFF34C27A, 0xFF0F7A47, 0xFF07492A, 0xFFF2C94C},
            {0xFF4A4F57, 0xFF1B1E23, 0xFF0B0C0E, 0xFFE8C45A}};

    /** A realistic hand-held tally counter: glossy body, LCD window, silver press button and glow rings. */
    private void drawTally(Canvas c, float w, int v) {
        int[] col = TALLY[v];
        float cx = w / 2;
        pill(c, cx, 0, 15, dp(220), 0xB0000000, Color.WHITE);
        float top = pillH(15) + dp(6);
        float bw = dp(150), bh = dp(230);
        RectF body = new RectF(cx - bw / 2, top, cx + bw / 2, top + bh);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 3; i++) {
            p.setStrokeWidth(dp(2));
            p.setColor((col[3] & 0xFFFFFF) | ((70 - i * 20) << 24));
            c.drawRoundRect(body.left - dp(4 + i * 5), body.top - dp(4 + i * 5), body.right + dp(4 + i * 5), body.bottom + dp(4 + i * 5), dp(40 + i * 5), dp(40 + i * 5), p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setShader(new android.graphics.RadialGradient(cx, body.centerY(), bw * 0.95f, (col[3] & 0xFFFFFF) | 0x66000000, col[3] & 0xFFFFFF, android.graphics.Shader.TileMode.CLAMP));
        c.drawCircle(cx, body.centerY(), bw * 0.95f, p);
        p.setShader(null);
        p.setColor(0x55000000);
        c.drawOval(body.left + dp(8), body.bottom + dp(2), body.right - dp(8), body.bottom + dp(12), p);
        p.setColor(col[2]);
        c.drawRoundRect(body.left, body.top + dp(5), body.right, body.bottom + dp(5), dp(36), dp(36), p);
        p.setShader(new android.graphics.LinearGradient(body.left, 0, body.right, 0, 0x55FFFFFF, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
        p.setShader(new android.graphics.LinearGradient(0, body.top, 0, body.bottom, col[0], col[1], android.graphics.Shader.TileMode.CLAMP));
        p.setShadowLayer(dp(10), 0, dp(5), 0x66000000);
        c.drawRoundRect(body, dp(36), dp(36), p);
        p.clearShadowLayer();
        // gloss
        p.setShader(new android.graphics.LinearGradient(0, body.top, 0, body.top + bh * 0.45f, 0x55FFFFFF, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
        c.drawRoundRect(body.left + dp(4), body.top + dp(3), body.right - dp(4), body.top + bh * 0.45f, dp(32), dp(32), p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.5f));
        p.setColor(0x66FFFFFF);
        c.drawRoundRect(body.left + dp(1), body.top + dp(1), body.right - dp(1), body.bottom - dp(1), dp(35), dp(35), p);
        p.setStyle(Paint.Style.FILL);
        // label
        np.setTypeface(Typeface.DEFAULT_BOLD);
        np.setTextSize(dp(8.5f));
        np.setColor(0xDDFFFFFF);
        np.setTextAlign(Paint.Align.CENTER);
        c.drawText("TALLY COUNTER", cx, body.top + dp(18), np);
        // LCD window
        RectF lcd = new RectF(body.left + dp(16), body.top + dp(26), body.right - dp(16), body.top + dp(80));
        p.setColor(0xFF0A1220);
        c.drawRoundRect(lcd, dp(10), dp(10), p);
        RectF in = new RectF(lcd.left + dp(3), lcd.top + dp(3), lcd.right - dp(3), lcd.bottom - dp(3));
        p.setShader(new android.graphics.LinearGradient(0, in.top, 0, in.bottom, 0xFFCFE0C4, 0xFFA9C29C, android.graphics.Shader.TileMode.CLAMP));
        c.drawRoundRect(in, dp(8), dp(8), p);
        p.setShader(null);
        sevenSeg(c, String.valueOf(count), in, 0xFF1F2A1C);
        p.setShader(new android.graphics.LinearGradient(0, in.top, 0, in.centerY(), 0x66FFFFFF, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
        c.drawRoundRect(in.left, in.top, in.right, in.centerY(), dp(8), dp(8), p);
        p.setShader(null);
        // round count in goal
        np.setTextSize(dp(10));
        np.setColor(0xCCFFFFFF);
        c.drawText(inRound() + " / " + goal(), cx, lcd.bottom + dp(15), np);
        // silver button
        float bx = cx, by = body.top + dp(150), br = dp(46);
        p.setStyle(Paint.Style.FILL);
        p.setColor(col[2]);
        c.drawCircle(bx, by, br + dp(5), p);
        p.setShader(new android.graphics.LinearGradient(0, by - br, 0, by + br, 0xFFF4F6F8, 0xFF8C96A3, android.graphics.Shader.TileMode.CLAMP));
        p.setShadowLayer(dp(5), 0, dp(3), 0x66000000);
        c.drawCircle(bx, by, br, p);
        p.clearShadowLayer();
        p.setShader(new android.graphics.RadialGradient(bx - br * 0.2f, by - br * 0.3f, br, 0x55FFFFFF, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
        c.drawCircle(bx, by, br * 0.82f, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(0x33000000);
        c.drawCircle(bx, by, br * 0.82f, p);
        c.drawCircle(bx, by, br * 0.6f, p);
        p.setColor(0x99FFFFFF);
        c.drawArc(bx - br * 0.9f, by - br * 0.9f, bx + br * 0.9f, by + br * 0.9f, 200, 70, false, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xCCFFFFFF);
        c.drawOval(bx - br * 0.35f, by - br * 0.7f, bx + br * 0.1f, by - br * 0.5f, p);
        p.setStyle(Paint.Style.FILL);
        // small reset button
        p.setShader(new android.graphics.LinearGradient(0, body.bottom - dp(34), 0, body.bottom - dp(14), 0xFFE9EDF1, 0xFF8C96A3, android.graphics.Shader.TileMode.CLAMP));
        c.drawCircle(body.right - dp(30), body.bottom - dp(24), dp(10), p);
        p.setShader(null);
        np.setTextSize(dp(8));
        np.setColor(0xCCFFFFFF);
        c.drawText("RESET", body.right - dp(30), body.bottom - dp(5), np);
    }

    private static final String[] SEGS = {"abcdef", "bc", "abged", "abgcd", "fgbc", "afgcd", "afgedc", "abc", "abcdefg", "abcfgd"};

    /** Draws digits like an LCD counter, right-aligned inside the box. */
    private void sevenSeg(Canvas c, String digits, RectF box, int color) {
        float h = box.height() * 0.66f;
        float dw = h * 0.5f, gap = h * 0.16f, t = h * 0.13f;
        float total = digits.length() * dw + (digits.length() - 1) * gap;
        float maxW = box.width() * 0.88f;
        if (total > maxW) {
            float k = maxW / total;
            h *= k; dw *= k; gap *= k; t *= k; total = maxW;
        }
        float x = box.right - box.width() * 0.06f - total;
        float y = box.centerY() - h / 2;
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        // faint "off" segments, like a real LCD
        for (int k = 0; k < digits.length(); k++) {
            drawDigit(c, "abcdefg", x + k * (dw + gap), y, dw, h, t, 0x14000000);
            drawDigit(c, SEGS[digits.charAt(k) - '0'], x + k * (dw + gap), y, dw, h, t, color);
        }
    }

    private void drawDigit(Canvas c, String on, float x, float y, float w, float h, float t, int color) {
        p.setColor(color);
        float g = t * 0.25f;
        float x0 = x + t / 2, x1 = x + w - t / 2, y0 = y + t / 2, ym = y + h / 2, y1 = y + h - t / 2;
        for (int i = 0; i < on.length(); i++) {
            switch (on.charAt(i)) {
                case 'a': hSeg(c, x0, x1, y0, t, g); break;
                case 'g': hSeg(c, x0, x1, ym, t, g); break;
                case 'd': hSeg(c, x0, x1, y1, t, g); break;
                case 'f': vSeg(c, x0, y0, ym, t, g); break;
                case 'b': vSeg(c, x1, y0, ym, t, g); break;
                case 'e': vSeg(c, x0, ym, y1, t, g); break;
                default: vSeg(c, x1, ym, y1, t, g); break;
            }
        }
    }

    private void hSeg(Canvas c, float a, float b, float y, float t, float g) {
        a += g;
        b -= g;
        path.reset();
        path.moveTo(a, y);
        path.lineTo(a + t / 2, y - t / 2);
        path.lineTo(b - t / 2, y - t / 2);
        path.lineTo(b, y);
        path.lineTo(b - t / 2, y + t / 2);
        path.lineTo(a + t / 2, y + t / 2);
        path.close();
        c.drawPath(path, p);
    }

    private void vSeg(Canvas c, float x, float a, float b, float t, float g) {
        a += g;
        b -= g;
        path.reset();
        path.moveTo(x, a);
        path.lineTo(x + t / 2, a + t / 2);
        path.lineTo(x + t / 2, b - t / 2);
        path.lineTo(x, b);
        path.lineTo(x - t / 2, b - t / 2);
        path.lineTo(x - t / 2, a + t / 2);
        path.close();
        c.drawPath(path, p);
    }

    /** 1 — ring counter, like a finger tasbih ring. */
    private void drawRing(Canvas c, float w) {
        float cx = w / 2;
        pill(c, cx, 0, 15, dp(220), 0xB0000000, Color.WHITE);

        float top = pillH(15) + dp(8);
        float bw = dp(118), bh = dp(122);
        r.set(cx - bw / 2, top, cx + bw / 2, top + bh);
        float a = dp(34), b = dp(46);
        path.reset();
        path.addRoundRect(r, new float[]{a, a, a, a, b, b, b, b}, Path.Direction.CW);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0xFFE2955A, 0xFFC9763C, Shader.TileMode.CLAMP));
        p.setShadowLayer(dp(8), 0, dp(4), 0x55000000);
        c.drawPath(path, p);
        p.clearShadowLayer();
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(4));
        p.setColor(Color.WHITE);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);

        RectF lcd = new RectF(r.left + dp(10), top + dp(14), r.right - dp(10), top + dp(56));
        p.setColor(0xFF1C1C1C);
        c.drawRoundRect(lcd, dp(10), dp(10), p);
        RectF in = new RectF(lcd.left + dp(5), lcd.top + dp(5), lcd.right - dp(5), lcd.bottom - dp(5));
        p.setShader(new LinearGradient(0, in.top, 0, in.bottom, 0xFFB9C2B5, 0xFFA3ADA0, Shader.TileMode.CLAMP));
        c.drawRoundRect(in, dp(5), dp(5), p);
        p.setShader(null);

        number(count >= 10000 ? 18 : 24, 0xFF2B322A, true);
        np.setTextAlign(Paint.Align.RIGHT);
        Paint.FontMetrics fm = np.getFontMetrics();
        c.drawText(String.valueOf(count), in.right - dp(6), in.centerY() - (fm.ascent + fm.descent) / 2, np);

        float oy = lcd.bottom + dp(14) + dp(17);
        RectF o = new RectF(cx - dp(29), oy - dp(17), cx + dp(29), oy + dp(17));
        p.setColor(0xFFA9662F);
        c.drawOval(new RectF(o.left - dp(3), o.top - dp(3), o.right + dp(3), o.bottom + dp(3)), p);
        p.setShader(new RadialGradient(o.left + o.width() * .4f, o.top + o.height() * .3f, o.width() * .6f,
                new int[]{0xFFFFFFFF, 0xFFB8BEC4, 0xFF6F767C}, new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
        c.drawOval(o, p);
        p.setShader(null);
    }

    /** 2 — green circle with a gold progress ring. */
    private void drawOrb(Canvas c, float w) {
        float cx = w / 2;
        pill(c, cx, 0, 15, dp(220), 0xFF1B5E20, Color.WHITE);
        float cy = pillH(15) + dp(8) + dp(48);

        float ir = dp(39);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx - ir * .3f, cy - ir * .4f, ir * 1.4f, 0xFF43A047, 0xFF1B5E20, Shader.TileMode.CLAMP));
        p.setShadowLayer(dp(8), 0, dp(4), 0x55000000);
        c.drawCircle(cx, cy, ir, p);
        p.clearShadowLayer();
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(6));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(0x88FFFFFF);
        c.drawCircle(cx, cy, dp(44), p);
        float sweep = 360f * inRound() / goal();
        if (sweep > 0) {
            p.setColor(0xFFC9A227);
            r.set(cx - dp(44), cy - dp(44), cx + dp(44), cy + dp(44));
            c.drawArc(r, -90, sweep, false, p);
        }
        p.setStyle(Paint.Style.FILL);

        number(count >= 1000 ? 21 : 28, Color.WHITE, false);
        if (target > 0) {
            centerText(c, np, Ui.fa(count), cx, cy - dp(5));
            number(11, 0xCCFFFFFF, false);
            centerText(c, np, "/ " + Ui.fa(target), cx, cy + dp(17));
        } else {
            centerText(c, np, Ui.fa(count), cx, cy);
        }
    }

    /** 3 — 33 beads around a circle that light up one by one. */
    private void drawBeads(Canvas c, float w) {
        float cx = w / 2;
        pill(c, cx, 0, 16, dp(220), 0xFFFFF8E8, 0xFF3E2A12);
        float cy = pillH(16) + dp(10) + dp(75);
        fillCircle(c, cx, cy, dp(75), 0xFF3E2A12, true);

        int on = Math.round(inRound() * 33f / goal());
        for (int i = 0; i < 33; i++) {
            double ang = i / 33.0 * Math.PI * 2 - Math.PI / 2;
            float x = cx + dp(62) * (float) Math.cos(ang);
            float y = cy + dp(62) * (float) Math.sin(ang);
            if (i < on) {
                p.setShader(new RadialGradient(x - dp(1.5f), y - dp(1.5f), dp(6), 0xFFFFF4C2, 0xFFD4A72C, Shader.TileMode.CLAMP));
                c.drawCircle(x, y, dp(5), p);
                p.setShader(null);
            } else {
                p.setColor(0xFF6B4B25);
                c.drawCircle(x, y, dp(5), p);
            }
        }
        number(count >= 1000 ? 26 : 34, 0xFFFFF1CF, false);
        centerText(c, np, Ui.fa(count), cx, cy);
    }

    /** 4 — dark pill along the bottom of the screen. */
    private void drawPill(Canvas c, float w) {
        r.set(0, 0, w, dp(60));
        p.setShader(null);
        p.setColor(0xE6101611);
        p.setShadowLayer(dp(8), 0, dp(4), 0x55000000);
        c.drawRoundRect(r, dp(30), dp(30), p);
        p.clearShadowLayer();

        float pcx = w - dp(8) - dp(22);
        fillCircle(c, pcx, dp(30), dp(22), 0xFF2E7D32, false);
        number(28, Color.WHITE, false);
        centerText(c, np, "+", pcx, dp(30));

        number(20, 0xFF9BE29F, false);
        String n = Ui.fa(count);
        float nRight = w - dp(8) - dp(44) - dp(10);
        np.setTextAlign(Paint.Align.RIGHT);
        Paint.FontMetrics fm = np.getFontMetrics();
        c.drawText(n, nRight, dp(30) - (fm.ascent + fm.descent) / 2, np);

        float textRight = nRight - np.measureText(n) - dp(12);
        textArea.set(dp(PAD), dp(PAD), dp(PAD) + textRight, dp(PAD) + dp(60));
        zk(17, Color.WHITE);
        String s = fit(textRight - dp(18));
        tp.setTextAlign(Paint.Align.RIGHT);
        fm = tp.getFontMetrics();
        c.drawText(s, textRight, dp(30) - (fm.ascent + fm.descent) / 2, tp);
    }

    /** 5 — dark green card with gold details. */
    private void drawGold(Canvas c, float w) {
        float h = dp(196);
        r.set(0, 0, w, h);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, w, h, 0xFF0F3D1F, 0xFF072613, Shader.TileMode.CLAMP));
        p.setShadowLayer(dp(10), 0, dp(5), 0x66000000);
        c.drawRoundRect(r, dp(22), dp(22), p);
        p.clearShadowLayer();
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.5f));
        p.setColor(0xFFC9A227);
        r.inset(dp(1), dp(1));
        c.drawRoundRect(r, dp(21), dp(21), p);
        p.setStyle(Paint.Style.FILL);

        float cx = w / 2;
        zk(20, 0xFFF3E3A5);
        centerText(c, tp, fit(w - dp(28)), cx, dp(34));

        number(count >= 10000 ? 26 : 34, Color.WHITE, false);
        centerText(c, np, Ui.fa(count), cx, dp(80));

        float by = dp(142);
        fillCircle(c, cx, by + dp(4), dp(32), 0xFF7A5B08, false);
        p.setShader(new RadialGradient(cx - dp(10), by - dp(10), dp(44), 0xFFF6DD7D, 0xFFB88A12, Shader.TileMode.CLAMP));
        c.drawCircle(cx, by, dp(32), p);
        p.setShader(null);
        zk(26, 0xFF2D2002);
        centerText(c, tp, "ﷲ", cx, by);
    }

    /** 6 — small floating head with a dhikr bubble. */
    private void drawHead(Canvas c, float w) {
        float cx = w - dp(30), cy = dp(30);
        fillCircle(c, cx, cy, dp(30), Color.WHITE, true);
        fillCircle(c, cx, cy, dp(27), 0xFF1B5E20, false);
        number(count >= 1000 ? 15 : 20, Color.WHITE, false);
        centerText(c, np, Ui.fa(count), cx, cy);

        zk(14, Color.WHITE);
        String s = fit(dp(180));
        float bw = tp.measureText(s) + dp(20);
        float right = w - dp(60) - dp(8);
        r.set(right - bw, dp(14), right, dp(46));
        p.setColor(0xE61B5E20);
        p.setShadowLayer(dp(4), 0, dp(2), 0x33000000);
        c.drawRoundRect(r, dp(10), dp(10), p);
        p.clearShadowLayer();
        centerText(c, tp, s, r.centerX(), r.centerY());
    }

    // ---------- side-view rollers: a thin wheel seen from its edge, rolled with the thumb ----------
    static boolean isRollerStyle(int s) {
        return s >= 10 && s <= 14;
    }

    public boolean isRoller() {
        return isRollerStyle(style);
    }

    /** Roller body size in dp (thin: width is the wheel's thickness). */
    private static final float RW = 38, RH = 120;
    /** How far the surface has rolled, in px along the unrolled surface. */
    private float rot;
    /** How many notches (ribs, beads, words) the wheel has all the way round; one dhikr is counted for each. */
    private int notches = 12, stripWords = 12;
    private android.graphics.Bitmap strip;
    private int stripStyle = -1;
    private float stripD;
    private String stripText = "";
    private final RectF rollerArea = new RectF(), gripArea = new RectF();
    private final android.graphics.Rect src = new android.graphics.Rect();
    private final RectF dst = new RectF();

    /** True when (x, y) — view coordinates — is on the roller (rolling counts there). */
    public boolean isRollerHit(float x, float y) {
        RectF t = new RectF(rollerArea);
        t.inset(-dp(16), -dp(4));
        return t.contains(x, y);
    }

    /** True when (x, y) — view coordinates — is on the small handle under the roller (the only place that moves it). */
    public boolean isGripHit(float x, float y) {
        RectF t = new RectF(gripArea);
        t.inset(-dp(14), -dp(10));
        return t.contains(x, y);
    }

    /** How many notches pass the pointer for each px of finger movement (the wheel spins faster than the finger). */
    private static final float ROLL_GAIN = 2.4f;

    /** Called for every notch that passes the gold pointer while the wheel spins on by itself after a flick. */
    public interface OnNotch {
        void onNotch(int n);
    }

    private OnNotch notchListener;
    private float flingV;
    private boolean flinging;

    public void setOnNotch(OnNotch l) {
        notchListener = l;
    }

    /** Rolls the surface by dy px of finger movement; returns how many notches passed the gold pointer. */
    public int roll(float dy) {
        return spin(dy * ROLL_GAIN);
    }

    private int spin(float dsurface) {
        strip();
        float sp = notchSpacing();
        float before = (float) Math.floor(-rot / sp - 0.5f);
        rot += dsurface;
        float after = (float) Math.floor(-rot / sp - 0.5f);
        invalidate();
        return Math.round(Math.abs(after - before));
    }

    /** Lets the wheel keep spinning after the finger lifts, slowing down by itself. */
    public void fling(float fingerVelocityPxPerSec) {
        flingV = fingerVelocityPxPerSec * ROLL_GAIN * 0.7f;
        if (Math.abs(flingV) < dp(160)) return;
        flinging = true;
        removeCallbacks(flingStep);
        postOnAnimation(flingStep);
    }

    public void stopFling() {
        flinging = false;
        removeCallbacks(flingStep);
    }

    private final Runnable flingStep = new Runnable() {
        @Override
        public void run() {
            if (!flinging) return;
            int n = spin(flingV / 60f);
            if (n > 0 && notchListener != null) notchListener.onNotch(n);
            flingV *= 0.955f;
            if (Math.abs(flingV) > dp(40)) postOnAnimation(this);
            else flinging = false;
        }
    };

    /** Height of one notch on the unrolled surface, in px. */
    private float notchSpacing() {
        return (float) (Math.PI * dp(RH)) / Math.max(1, notches);
    }

    private void drawRoller(Canvas c, float w) {
        float cx = w / 2;
        pill(c, cx, 0, 14, dp(200), 0xC0000000, Color.WHITE);
        float top = pillH(14) + dp(8);
        float rw = dp(RW), rh = dp(RH), rad = rh / 2f, corner = dp(8);
        float left = cx - rw / 2;
        RectF body = new RectF(left, top, left + rw, top + rh);
        rollerArea.set(body);
        rollerArea.offset(dp(PAD), dp(PAD));

        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF000000);
        p.setShadowLayer(dp(6), 0, dp(3), 0x66000000);
        c.drawRoundRect(body, corner, corner, p);
        p.clearShadowLayer();

        android.graphics.Bitmap s = strip();
        int circ = s.getHeight();
        c.save();
        path.reset();
        path.addRoundRect(body, corner, corner, Path.Direction.CW);
        c.clipPath(path);
        int rows = (int) Math.ceil(rh);
        for (int i = 0; i < rows; i++) {
            float yc = i + 0.5f - rad;
            double phi = Math.asin(Math.max(-1f, Math.min(1f, yc / rad)));
            int row = Math.floorMod((int) Math.floor(phi * rad - rot), circ);
            src.set(0, row, s.getWidth(), row + 1);
            dst.set(left, top + i, left + rw, top + i + 1);
            c.drawBitmap(s, src, dst, bp);
        }
        p.setShader(new LinearGradient(0, top, 0, top + rh,
                new int[]{0xE0000000, 0x30000000, 0x22FFFFFF, 0x00000000, 0x30000000, 0xE0000000},
                new float[]{0f, .28f, .42f, .5f, .72f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(body, p);
        p.setShader(null);
        c.restore();

        // gold pointer: where each dhikr is counted
        float py = top + rad, px = left + rw + dp(3);
        path.reset();
        path.moveTo(px, py);
        path.lineTo(px + dp(7), py - dp(6));
        path.lineTo(px + dp(7), py + dp(6));
        path.close();
        p.setColor(0xFFD4A648);
        c.drawPath(path, p);

        // count
        float ny = top + rh + dp(8) + dp(13);
        number(count >= 1000 ? 15 : 19, Color.WHITE, false);
        String cs = Ui.fa(count);
        float cw = Math.max(np.measureText(cs) + dp(22), dp(46));
        r.set(cx - cw / 2, ny - dp(13), cx + cw / 2, ny + dp(13));
        p.setColor(0xE00F3D2A);
        c.drawRoundRect(r, dp(13), dp(13), p);
        centerText(c, np, cs, cx, ny);

        // handle: the only place that moves the roller
        float gy = ny + dp(13) + dp(6);
        r.set(cx - dp(22), gy, cx + dp(22), gy + dp(16));
        p.setColor(0xD0202124);
        c.drawRoundRect(r, dp(8), dp(8), p);
        gripArea.set(r);
        gripArea.offset(dp(PAD), dp(PAD));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.6f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(0xFFCFCFCF);
        for (int k = -1; k <= 1; k += 2) {
            float yy = gy + dp(8) + k * dp(2.5f);
            c.drawLine(cx - dp(9), yy, cx + dp(9), yy, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    /** The roller's surface, unrolled into one tall strip (its height is the wheel's circumference). */
    private android.graphics.Bitmap strip() {
        if (strip != null && stripStyle == style && stripD == d && (style != 14 || stripText.equals(text))) return strip;
        int W = Math.max(2, Math.round(dp(RW)));
        int C = Math.max(2, Math.round((float) (Math.PI * dp(RH))));
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(W, C, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas k = new Canvas(b);
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setStyle(Paint.Style.FILL);
        switch (style) {
            case 10: { // black tyre
                k.drawColor(0xFF1C1C1C);
                int n = 12;
                float sp = C / (float) n, h = sp * 0.42f, t = sp * 0.30f, e = dp(5);
                q.setColor(0xFF424242);
                for (int i = -1; i <= n; i++) {
                    float y = (i + 0.5f) * sp;
                    Path pa = new Path();
                    pa.moveTo(e, y - h / 2);
                    pa.lineTo(W / 2f, y + h / 2);
                    pa.lineTo(W - e, y - h / 2);
                    pa.lineTo(W - e, y - h / 2 + t);
                    pa.lineTo(W / 2f, y + h / 2 + t);
                    pa.lineTo(e, y - h / 2 + t);
                    pa.close();
                    k.drawPath(pa, q);
                }
                q.setColor(0xFFD4A648);
                k.drawRect(0, 0, dp(2.5f), C, q);
                k.drawRect(W - dp(2.5f), 0, W, C, q);
                break;
            }
            case 11: { // wooden cart wheel rim
                k.drawColor(0xFF8A5A2B);
                int n = 12;
                float sp = C / (float) n;
                q.setStyle(Paint.Style.STROKE);
                for (int i = 0; i < n; i++) {
                    float y = (i + 0.5f) * sp;
                    q.setColor(0xFF5E3B1B);
                    q.setStrokeWidth(dp(1.2f));
                    k.drawLine(0, y, W, y, q);
                    q.setColor(0x405E3B1B);
                    q.setStrokeWidth(dp(0.7f));
                    k.drawLine(dp(8), y + sp * 0.35f, W - dp(8), y + sp * 0.45f, q);
                    k.drawLine(dp(10), y + sp * 0.7f, W - dp(12), y + sp * 0.62f, q);
                }
                q.setStyle(Paint.Style.FILL);
                q.setColor(0xFF4A4A4A);
                k.drawRect(0, 0, dp(6), C, q);
                k.drawRect(W - dp(6), 0, W, C, q);
                q.setColor(0xFFBDBDBD);
                int m = 12;
                for (int i = 0; i <= m; i++) {
                    float y = i * C / (float) m;
                    k.drawCircle(dp(3), y, dp(1.6f), q);
                    k.drawCircle(W - dp(3), y, dp(1.6f), q);
                }
                break;
            }
            case 12: { // green with gold ribs
                k.drawColor(0xFF0F5A3C);
                int n = 12;
                float sp = C / (float) n;
                q.setColor(0xFFD4A648);
                for (int i = 0; i < n; i++) {
                    float y = (i + 0.5f) * sp;
                    k.drawRect(dp(7), y - dp(2.2f), W - dp(7), y + dp(2.2f), q);
                }
                k.drawRect(0, 0, dp(4), C, q);
                k.drawRect(W - dp(4), 0, W, C, q);
                break;
            }
            case 13: { // tasbih beads
                k.drawColor(0xFF16241D);
                int n = 12;
                float sp = C / (float) n;
                float rx = W / 2f - dp(6), ry = sp / 2f - dp(1);
                for (int i = -1; i <= n; i++) {
                    float y = (i + 0.5f) * sp;
                    int j = Math.floorMod(i, n);
                    q.setColor(j % 9 == 0 ? 0xFFD4A648 : 0xFF1F8A5C);
                    k.drawOval(new RectF(W / 2f - rx, y - ry, W / 2f + rx, y + ry), q);
                    q.setColor(0x55FFFFFF);
                    k.drawOval(new RectF(W / 2f - rx * 0.55f, y - ry * 0.7f, W / 2f - rx * 0.1f, y - ry * 0.35f), q);
                }
                q.setColor(0xFFD4A648);
                k.drawRect(dp(2), 0, dp(3), C, q);
                k.drawRect(W - dp(3), 0, W - dp(2), C, q);
                break;
            }
            default: { // tape with the dhikr written around it
                k.drawColor(0xFFEEF3F0);
                TextPaint t = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                t.setTypeface(amiriBold);
                t.setTextSize(Math.min(dp(16), W * 0.42f));
                t.setColor(0xFF0F5A3C);
                t.setTextAlign(Paint.Align.CENTER);
                String s = text == null || text.isEmpty() ? "سُبْحَانَ اللهِ" : text;
                s = TextUtils.ellipsize(s, t, C - dp(30), TextUtils.TruncateAt.END).toString();
                float tw = t.measureText(s);
                int n = Math.max(1, (int) Math.floor(C / (tw + dp(24))));
                float sp = C / (float) n;
                stripWords = n;
                Paint.FontMetrics fm = t.getFontMetrics();
                for (int i = 0; i < n; i++) {
                    float y = (i + 0.5f) * sp;
                    k.save();
                    k.translate(W / 2f, y);
                    k.rotate(90);
                    k.drawText(s, 0, -(fm.ascent + fm.descent) / 2f, t);
                    k.restore();
                }
                q.setColor(0xFFD4A648);
                for (int i = 0; i <= n; i++) k.drawCircle(W / 2f, i * sp, dp(2.5f), q);
                q.setColor(0xFFC9D3CE);
                k.drawRect(0, 0, dp(3), C, q);
                k.drawRect(W - dp(3), 0, W, C, q);
            }
        }
        notches = style == 14 ? stripWords : 12;
        strip = b;
        stripStyle = style;
        stripD = d;
        stripText = text;
        return b;
    }
}
