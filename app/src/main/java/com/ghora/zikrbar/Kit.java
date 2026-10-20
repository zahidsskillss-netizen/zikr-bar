package com.ghora.zikrbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/** Shared colors, fonts and building blocks for the app screens. */
final class Kit {

    static final int GREEN = 0xFF1B5E20, GREEN2 = 0xFF2E7D32, DARK = 0xFF0B3319;
    static final int INK = 0xFF16231A, MUTED = 0xFF66746A, LINE = 0xFFE1E8E0;
    static final int BG = 0xFFF1F4EF, SOFT = 0xFFF6F8F4, GOLD = 0xFFF3E3A5, GOLD_LINE = 0xFFC9A227;
    static final int WHITE = 0xFFFFFFFF;

    static final String FACEBOOK = "https://www.facebook.com/zahid.alkozai";
    static final String TELEGRAM = "https://t.me/zahid_af";
    static final String PHONE = "0780630100";
    static final String WHATSAPP_LINK = "https://wa.me/93780630100";

    static void open(Context c, String url) {
        try {
            c.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(c, "لینک خلاص نه شو", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private final Context c;
    private final float d;
    final Typeface titr, body, strong, amiri, amiriBold;

    Kit(Context c) {
        this.c = c;
        d = c.getResources().getDisplayMetrics().density;
        titr = c.getResources().getFont(R.font.bahij_titr);
        body = c.getResources().getFont(R.font.bahij_light);
        strong = c.getResources().getFont(R.font.bahij_body);
        amiri = Ui.amiri(c);
        amiriBold = Ui.amiriBold(c);
    }

    int dp(float v) {
        return Math.round(v * d);
    }

    // ---------- text ----------
    TextView title(String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTypeface(titr);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    TextView text(String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTypeface(body);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.3f);
        return t;
    }

    /** Heavier body text for buttons, chips and small labels. */
    TextView strong(String s, float sp, int color) {
        TextView t = text(s, sp, color);
        t.setTypeface(strong);
        return t;
    }

    TextView arabic(String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTypeface(amiriBold);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    // ---------- shapes ----------
    GradientDrawable rounded(int fill, float radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1.2f), stroke);
        return g;
    }

    GradientDrawable greenGradient(float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GREEN2, DARK});
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    /** Small rounded green badge with a gold glyph. */
    TextView badge(String glyph, float sizeDp) {
        TextView m = new TextView(c);
        m.setText(glyph);
        m.setTypeface(amiriBold);
        m.setTextSize(sizeDp * 0.42f);
        m.setTextColor(GOLD);
        m.setGravity(Gravity.CENTER);
        m.setIncludeFontPadding(false);
        m.setBackground(greenGradient(sizeDp * 0.32f));
        m.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return m;
    }

    /** The developer photo in a gold ring. */
    ImageView photo(float sizeDp) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(R.drawable.developer);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(GOLD);
        iv.setBackground(ring);
        int p = dp(Math.max(2f, sizeDp / 32f));
        iv.setPadding(p, p, p, p);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        iv.setContentDescription("د زاهدالله الکوزی فیسبوک");
        iv.setOnClickListener(v -> open(c, FACEBOOK));
        return iv;
    }

    // ---------- blocks ----------
    LinearLayout card(String glyph, String title, String sub) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(18));
        card.setBackground(rounded(WHITE, 24, LINE));
        card.setElevation(dp(1.5f));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(dp(16), dp(14), dp(16), 0);
        card.setLayoutParams(lp);

        LinearLayout head = row();
        head.setPadding(0, 0, 0, dp(6));
        if (glyph != null) head.addView(badge(glyph, 40));
        LinearLayout tx = new LinearLayout(c);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(glyph != null ? dp(12) : 0, 0, 0, 0);
        tx.addView(title(title, 19, INK));
        if (sub != null) {
            TextView s = text(sub, 13.5f, MUTED);
            s.setPadding(0, dp(3), 0, 0);
            tx.addView(s);
        }
        head.addView(tx, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(head);
        return card;
    }

    TextView label(String s) {
        TextView t = title(s, 14.5f, INK);
        t.setPadding(0, dp(16), 0, dp(8));
        return t;
    }

    TextView button(String s, boolean filled, View.OnClickListener l) {
        TextView b = strong(s, 15, filled ? WHITE : GREEN);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(11), dp(16), dp(13));
        Drawable shape = filled ? greenGradient(16) : rounded(WHITE, 16, GREEN);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(filled ? 0x40FFFFFF : 0x201B5E20), shape, null));
        b.setClickable(true);
        b.setFocusable(true);
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    TextView goldButton(String s, View.OnClickListener l) {
        TextView b = strong(s, 16, 0xFF2D2002);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(12), dp(16), dp(14));
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFFF6DD7D, 0xFFC9A227});
        shape.setCornerRadius(dp(16));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), shape, null));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    Spinner spinner(String[] items, boolean arabicFont) {
        Spinner s = new Spinner(c);
        s.setAdapter(adapter(items, arabicFont));
        s.setBackground(rounded(SOFT, 16, LINE));
        s.setPadding(dp(10), dp(6), dp(10), dp(6));
        return s;
    }

    ArrayAdapter<String> adapter(String[] items, boolean arabicFont) {
        return new ArrayAdapter<String>(c, android.R.layout.simple_spinner_dropdown_item, items) {
            @Override
            public View getView(int pos, View v, ViewGroup g) {
                return style((TextView) super.getView(pos, v, g));
            }

            @Override
            public View getDropDownView(int pos, View v, ViewGroup g) {
                return style((TextView) super.getDropDownView(pos, v, g));
            }

            private TextView style(TextView t) {
                t.setTypeface(arabicFont ? amiriBold : body);
                t.setTextSize(arabicFont ? 17 : 16);
                t.setTextColor(INK);
                t.setSingleLine(true);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                return t;
            }
        };
    }

    LinearLayout row() {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    LinearLayout.LayoutParams topMargin(int dpv) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(dpv);
        return lp;
    }

    LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }

    View gap(int dpw) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(dpw), dp(dpw)));
        return v;
    }

    /** Green header background with rounded bottom and a faint eight-point star pattern. */
    Drawable headerBackground() {
        return new PatternDrawable(dp(34), dp(46));
    }

    static final class PatternDrawable extends Drawable {
        private final float radius, step;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clip = new Path();
        private final Path star = new Path();

        PatternDrawable(float radius, float step) {
            this.radius = radius;
            this.step = step;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(step / 40f);
            line.setColor(0x26F3E3A5);
        }

        @Override
        public void draw(Canvas cv) {
            Rect b = getBounds();
            RectF r = new RectF(b);
            clip.reset();
            clip.addRoundRect(r, new float[]{0, 0, 0, 0, radius, radius, radius, radius}, Path.Direction.CW);
            cv.save();
            cv.clipPath(clip);
            fill.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, GREEN2, DARK, Shader.TileMode.CLAMP));
            cv.drawRect(r, fill);

            float s = step * 0.36f;
            for (float y = b.top; y < b.bottom + step; y += step) {
                for (float x = b.left; x < b.right + step; x += step) {
                    star.reset();
                    star.addRect(x - s, y - s, x + s, y + s, Path.Direction.CW);
                    cv.drawPath(star, line);
                    cv.save();
                    cv.rotate(45, x, y);
                    cv.drawPath(star, line);
                    cv.restore();
                }
            }
            cv.restore();
        }

        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter f) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
