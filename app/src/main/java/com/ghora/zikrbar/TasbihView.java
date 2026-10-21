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

/** Draws the floating tasbih counter in one of six designs. */
public class TasbihView extends View {

    static final int COUNT = 6;

    static String name(int i) {
        return L.t("d" + i);
    }

    private static final float PAD = 10;
    /** Width of the column on the left that holds the "change dhikr" button. */
    private static final float BTN = 34, GAP = 6;

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
            case 4:
                w = dp(200);
                h = dp(196);
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
        float h = getHeight() - dp(PAD) * 2;
        drawSwitch(c, h);
        c.save();
        c.translate(dp(PAD) + dp(BTN + GAP), dp(PAD));
        float w = getWidth() - dp(PAD) * 2 - dp(BTN + GAP);
        switch (style) {
            case 0: drawRing(c, w); break;
            case 1: drawOrb(c, w); break;
            case 2: drawBeads(c, w); break;
            case 3: drawPill(c, w); break;
            case 4: drawGold(c, w); break;
            default: drawHead(c, w);
        }
        c.restore();
    }

    /** True when (x, y) — in view coordinates — is on the "change dhikr" button. */
    public boolean isSwitchHit(float x, float y) {
        RectF t = new RectF(btn);
        t.inset(-dp(6), -dp(6));
        return t.contains(x, y);
    }

    /** Small round button on the left: tap it to move to the next dhikr. */
    private void drawSwitch(Canvas c, float h) {
        float rad = dp(BTN) / 2;
        float cx = dp(PAD) + rad;
        float cy = dp(PAD) + ((style == 3 || style == 5) ? h / 2 : Math.max(rad, dp(17)));
        btn.set(cx - rad, cy - rad, cx + rad, cy + rad);
        fillCircle(c, cx, cy, rad, Color.WHITE, true);
        fillCircle(c, cx, cy, rad - dp(2.5f), 0xFF1B5E20, false);

        // circular arrows icon
        float ar = rad * 0.48f;
        r.set(cx - ar, cy - ar, cx + ar, cy + ar);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(2.2f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(0xFFF3E3A5);
        c.drawArc(r, 200, 140, false, p);
        c.drawArc(r, 20, 140, false, p);
        p.setStyle(Paint.Style.FILL);
        arrowHead(c, cx, cy, ar, 340);
        arrowHead(c, cx, cy, ar, 160);
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
}
