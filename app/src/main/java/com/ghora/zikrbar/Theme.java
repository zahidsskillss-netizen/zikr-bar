package com.ghora.zikrbar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

/**
 * App themes. "Midnight Aurora" (glass) is the default; the others are Emerald Night, Golden Dawn,
 * Pearl Frost, Turquoise Mosaic and the original Classic look. Glass themes draw soft colour lights
 * behind see-through cards with a thin light border.
 */
final class Theme {

    static final int AURORA = 0, EMERALD = 1, DAWN = 2, FROST = 3, TURQUOISE = 4, CLASSIC = 5;
    static final String[] KEYS = {"th_aurora", "th_emerald", "th_dawn", "th_frost", "th_turquoise", "th_classic"};

    static int id = AURORA;
    static boolean dark, glass;
    /** page colour, three light spots behind the glass, and their strengths */
    static int base, spot1, spot2, spot3;
    /** see-through card fill (top and bottom) and its border */
    static int cardTop, cardBottom, border;

    private Theme() {}

    static int saved(Context c) {
        return c.getSharedPreferences("zikr", Context.MODE_PRIVATE).getInt("theme", AURORA);
    }

    static void save(Context c, int t) {
        c.getSharedPreferences("zikr", Context.MODE_PRIVATE).edit().putInt("theme", t).apply();
    }

    /** Loads the saved theme into the shared colours (called from L.init on every screen). */
    static void load(Context c) {
        set(saved(c));
    }

    static void set(int t) {
        id = Math.max(0, Math.min(CLASSIC, t));
        switch (id) {
            case EMERALD:
                pal(true, 0xFF04140D, 0xD90F8A53, 0x73C9A227, 0x991D6B8F,
                        0xFFF2FFF7, 0xFFBFE6CF, 0xFF8FF0BF, 0xFF13A463, 0xFF0B5D39, 0xFF0A3D26, 0xFFF3D77A, 0xFFF3D77A);
                break;
            case DAWN:
                pal(false, 0xFFFBE7C6, 0xBFF5A524, 0x80E9795A, 0xE6FFF3B0,
                        0xFF2B1C02, 0xFF6E4A0C, 0xFF7A4E06, 0xFF5A3B06, 0xFF3D2803, 0xFF2B1C02, 0xFFF6D27A, 0xFFB7791F);
                break;
            case FROST:
                pal(false, 0xFFEEF3F0, 0xCC8FE3B8, 0xB3A9CDFA, 0xB3F7DC8F,
                        0xFF10281C, 0xFF3F5A4C, 0xFF0F7A4B, 0xFF0F7A4B, 0xFF0B5D39, 0xFF0B3D27, 0xFFF3E3A5, 0xFFB8860B);
                break;
            case TURQUOISE:
                pal(true, 0xFF053B3F, 0x8C14B8A6, 0x66E7B36A, 0x800E7490,
                        0xFFEFFFFC, 0xFFA7E6DD, 0xFF7FF5E6, 0xFF0D9488, 0xFF115E59, 0xFF0B3F3C, 0xFFF4CF96, 0xFFF4CF96);
                break;
            case CLASSIC:
                classic();
                break;
            default:
                pal(true, 0xFF070B1F, 0x732DD4BF, 0x807C3AED, 0x59E0B040,
                        0xFFEEF4FF, 0xFFA9C2E8, 0xFF5EEAD4, 0xFF0F9B8E, 0xFF115E59, 0xFF0B3B39, 0xFFE8C96A, 0xFFE8C96A);
        }
    }

    /** Sets a glass palette and maps it onto the colours every screen uses. */
    private static void pal(boolean isDark, int bg, int s1, int s2, int s3, int ink, int muted, int link,
                            int green2, int green, int deep, int gold, int goldLine) {
        dark = isDark;
        glass = true;
        base = bg;
        spot1 = s1;
        spot2 = s2;
        spot3 = s3;
        cardTop = isDark ? 0x2EFFFFFF : 0x99FFFFFF;
        cardBottom = isDark ? 0x12FFFFFF : 0x59FFFFFF;
        border = isDark ? 0x40FFFFFF : 0xE6FFFFFF;

        Kit.GREEN = link;
        Kit.GREEN2 = green2;
        Kit.DARK = deep;
        Kit.INK = ink;
        Kit.MUTED = muted;
        Kit.LINE = border;
        Kit.BG = bg;
        Kit.SOFT = isDark ? 0x14FFFFFF : 0x66FFFFFF;
        Kit.GOLD = gold;
        Kit.GOLD_LINE = goldLine;
        Kit.CARD = isDark ? 0x1AFFFFFF : 0x80FFFFFF;
        Kit.ON_CARD_DIM = isDark ? 0x24FFFFFF : 0x1A000000;

        TasbihActivity.D_BG = bg;
        TasbihActivity.D_BG2 = bg;
        TasbihActivity.D_CARD = Kit.CARD;
        TasbihActivity.D_HEAD = 0x00000000;
        TasbihActivity.D_TEXT = ink;
        TasbihActivity.D_MUTED = muted;
        TasbihActivity.D_ACCENT = link;
        TasbihActivity.D_GOLD = isDark ? gold : goldLine;
    }

    private static void classic() {
        dark = false;
        glass = false;
        base = 0xFFF1F4EF;
        spot1 = spot2 = spot3 = 0;
        cardTop = cardBottom = 0xFFFFFFFF;
        border = 0xFFE1E8E0;
        Kit.GREEN = 0xFF1B5E20;
        Kit.GREEN2 = 0xFF2E7D32;
        Kit.DARK = 0xFF0B3319;
        Kit.INK = 0xFF16231A;
        Kit.MUTED = 0xFF66746A;
        Kit.LINE = 0xFFE1E8E0;
        Kit.BG = 0xFFF1F4EF;
        Kit.SOFT = 0xFFF6F8F4;
        Kit.GOLD = 0xFFF3E3A5;
        Kit.GOLD_LINE = 0xFFC9A227;
        Kit.CARD = 0xFFFFFFFF;
        Kit.ON_CARD_DIM = 0x1A000000;
        TasbihActivity.D_BG = 0xFF0D1B18;
        TasbihActivity.D_BG2 = 0xFF0F2B27;
        TasbihActivity.D_CARD = 0xFF1A2724;
        TasbihActivity.D_HEAD = 0xFF0F2A1C;
        TasbihActivity.D_TEXT = 0xFFFFFFFF;
        TasbihActivity.D_MUTED = 0x99FFFFFF;
        TasbihActivity.D_ACCENT = 0xFF63D3A0;
        TasbihActivity.D_GOLD = 0xFFE9C46A;
    }

    /** A faint overlay that shows on this theme: white on dark themes, black on light ones. */
    static int ov(int alpha) {
        boolean darkPage = dark || id == CLASSIC;
        return (alpha << 24) | (darkPage ? 0xFFFFFF : 0x000000);
    }

    /** Sets the page background (colour lights) and the system bar colours. */
    static void window(Activity a) {
        a.getWindow().setBackgroundDrawable(new Lights());
        int bar = glass ? (dark ? base : Kit.DARK) : Kit.GREEN2;
        a.getWindow().setStatusBarColor(bar);
        a.getWindow().setNavigationBarColor(base);
    }

    /** Same for the dark pages (tasbih, prayer, duas) in the classic theme. */
    static void darkWindow(Activity a) {
        if (glass) {
            window(a);
            return;
        }
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{TasbihActivity.D_BG, TasbihActivity.D_BG2});
        a.getWindow().setBackgroundDrawable(g);
        a.getWindow().setStatusBarColor(TasbihActivity.D_HEAD);
        a.getWindow().setNavigationBarColor(TasbihActivity.D_BG2);
    }

    /** Shadows look muddy under see-through cards, so only the classic theme keeps them. */
    static void elev(View v, float px) {
        v.setElevation(glass ? 0 : px);
    }

    /** See-through glass card. */
    static GradientDrawable card(float radiusPx, float strokePx) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{cardTop, cardBottom});
        g.setCornerRadius(radiusPx);
        g.setStroke(Math.max(1, Math.round(strokePx)), border);
        return g;
    }

    /** Page background: base colour with three soft colour lights. */
    static final class Lights extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            c.drawColor(base);
            if (!glass) return;
            float w = b.width(), h = b.height();
            spot(c, w * 0.95f, h * 0.08f, w * 0.85f, spot1);
            spot(c, w * 0.05f, h * 0.42f, w * 0.80f, spot2);
            spot(c, w * 0.70f, h * 0.95f, w * 0.75f, spot3);
        }

        private void spot(Canvas c, float x, float y, float r, int color) {
            p.setShader(new RadialGradient(x, y, r, new int[]{color, color & 0x00FFFFFF},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, r, p);
        }

        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter f) {}
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
}
