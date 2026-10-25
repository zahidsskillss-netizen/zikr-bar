package com.ghora.zikrbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.ghora.zikrbar.Kit.*;
import static com.ghora.zikrbar.TasbihActivity.*;

/** A dua category as a numbered list, and a single dua with its meaning, reference, favourite and share. */
public class DuaListActivity extends Activity {

    static final String EXTRA_CAT = "cat", EXTRA_DUA = "dua";
    static final int REF_GREEN = 0xFF4CAF6A, ORANGE = 0xFFF08A1C;

    private Kit k;
    private ScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.darkWindow(this);
        scroll = new ScrollView(this);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build(); // favourites may have changed
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(28));
        int y = scroll.getScrollY();
        scroll.removeAllViews();
        scroll.addView(root);
        String duaId = getIntent().getStringExtra(EXTRA_DUA);
        if (duaId != null) buildDua(root, duaId);
        else buildList(root, getIntent().getStringExtra(EXTRA_CAT));
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    private void header(LinearLayout root, String title) {
        LinearLayout h = k.row();
        h.setBackgroundColor(D_HEAD);
        h.setPadding(k.dp(8), k.dp(10), k.dp(8), k.dp(10));
        TextView back = new TextView(this);
        back.setText(L.rtl() ? "→" : "←");
        back.setTextSize(22);
        back.setTextColor(D_TEXT);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back, new LinearLayout.LayoutParams(k.dp(44), k.dp(44)));
        TextView t = k.title(title, 20, D_TEXT);
        t.setGravity(Gravity.CENTER);
        h.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        h.addView(new View(this), new LinearLayout.LayoutParams(k.dp(44), k.dp(44)));
        root.addView(h);
    }

    /** Category icon in a dark rounded box, a thin line, then the title — as on the dua pages. */
    private void titleRow(LinearLayout root, String glyph, String title) {
        LinearLayout r = k.row();
        r.setPadding(k.dp(14), k.dp(18), k.dp(16), k.dp(12));
        r.addView(icon(this, k, glyph, 64));
        View line = new View(this);
        line.setBackgroundColor(Theme.ov(0xCC));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(k.dp(1.5f), k.dp(58));
        lp.setMargins(k.dp(14), 0, k.dp(14), 0);
        r.addView(line, lp);
        TextView t = k.title(title, 18, D_TEXT);
        t.setLineSpacing(0, 1.15f);
        r.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(r);
    }

    // ---------------------------------------------------------------- list
    private void buildList(LinearLayout root, String cat) {
        boolean fav = "fav".equals(cat);
        String glyph = fav ? "⭐" : glyphOf(cat);
        String name = fav ? L.t("my_duas") : L.t("dua_" + cat);
        header(root, L.t("duas_list"));
        titleRow(root, glyph, name);

        List<DuaData.Dua> duas = new ArrayList<>();
        if (fav) {
            for (String id : DuaData.favs(this)) {
                DuaData.Dua d = DuaData.find(this, id);
                if (d != null) duas.add(d);
            }
        } else {
            for (DuaData.Dua d : DuaData.category(this, cat)) duas.add(d);
        }
        if (duas.isEmpty()) {
            TextView t = k.text(L.t(fav ? "fav_empty" : "duas_soon"), 15, D_MUTED);
            t.setGravity(Gravity.CENTER);
            t.setPadding(k.dp(24), k.dp(40), k.dp(24), 0);
            root.addView(t);
            return;
        }
        for (int i = 0; i < duas.size(); i++) {
            DuaData.Dua d = duas.get(i);
            LinearLayout r = k.row();
            r.setPadding(k.dp(16), k.dp(16), k.dp(16), k.dp(16));
            r.setBackground(new RippleDrawable(ColorStateList.valueOf(Theme.ov(0x30)), null, k.rounded(Theme.ov(0xFF), 0, 0)));
            TextView n = k.title(L.num(i + 1), 16, D_TEXT);
            n.setGravity(Gravity.CENTER);
            n.setBackground(k.rounded(GREEN2, 24, 0));
            r.addView(n, new LinearLayout.LayoutParams(k.dp(48), k.dp(48)));
            TextView t = k.title(d.title(), 17, D_TEXT);
            t.setLineSpacing(0, 1.15f);
            t.setPaddingRelative(k.dp(18), 0, 0, 0);
            r.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            r.setOnClickListener(v -> startActivity(new Intent(this, DuaListActivity.class).putExtra(EXTRA_DUA, d.id())));
            root.addView(r);
            View line = new View(this);
            line.setBackgroundColor(Theme.ov(0x14));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, k.dp(1));
            lp.setMargins(k.dp(14), 0, k.dp(14), 0);
            root.addView(line, lp);
        }
    }

    // ---------------------------------------------------------------- one dua
    private void buildDua(LinearLayout root, String id) {
        DuaData.Dua d = DuaData.find(this, id);
        header(root, L.t("dua_one"));
        if (d == null) return;
        titleRow(root, glyphOf(d.cat), d.title());
        boolean many = d.parts.length > 1;
        for (int i = 0; i < d.parts.length; i++) {
            DuaData.Part p = d.parts[i];
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setPadding(k.dp(18), k.dp(18), k.dp(18), k.dp(8));

            TextView ar = k.arabic(p.text, 24, D_TEXT);
            ar.setGravity(Gravity.START);
            ar.setTextDirection(View.TEXT_DIRECTION_RTL);
            ar.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            ar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            ar.setLineSpacing(0, 1.7f);
            c.addView(ar, k.matchWrap());

            if (!p.meaning.isEmpty()) {
                TextView m = k.arabic((many ? L.num(i + 1) + "- " : "") + p.meaning, 17, Theme.ov(0xEE));
                m.setTypeface(k.amiri);
                m.setTextDirection(View.TEXT_DIRECTION_RTL);
                m.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                m.setGravity(Gravity.START);
                m.setLineSpacing(0, 1.45f);
                m.setPadding(0, k.dp(12), 0, 0);
                c.addView(m, k.matchWrap());
            }
            if (!p.ref.isEmpty()) {
                TextView r = k.text(p.ref, 13.5f, REF_GREEN);
                r.setGravity(Gravity.CENTER);
                r.setTextDirection(View.TEXT_DIRECTION_LTR);
                r.setPadding(0, k.dp(12), 0, 0);
                c.addView(r, k.matchWrap());
            }
            c.addView(actions(d, p));
            root.addView(c);
            View line = new View(this);
            line.setBackgroundColor(Theme.ov(0x14));
            root.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, k.dp(1)));
        }
    }

    private View actions(DuaData.Dua d, DuaData.Part p) {
        LinearLayout row = k.row();
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, k.dp(12), 0, k.dp(6));
        TextView star = round(DuaData.isFav(this, d.id()) ? "⭐" : "☆");
        star.setOnClickListener(v -> {
            DuaData.toggleFav(this, d.id());
            boolean on = DuaData.isFav(this, d.id());
            star.setText(on ? "⭐" : "☆");
            Toast.makeText(this, L.t(on ? "fav_added" : "fav_removed"), Toast.LENGTH_SHORT).show();
        });
        row.addView(star);
        row.addView(bar());
        TextView share = round("⇪");
        share.setOnClickListener(v -> {
            String text = d.title() + "\n\n" + p.text + (p.meaning.isEmpty() ? "" : "\n\n" + p.meaning)
                    + (p.ref.isEmpty() ? "" : "\n\n" + p.ref) + "\n\n— " + L.t("app_name");
            Intent s = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(s, L.t("share")));
        });
        row.addView(share);
        row.addView(bar());
        TextView copy = round("⎘");
        copy.setOnClickListener(v -> {
            android.content.ClipboardManager cm = getSystemService(android.content.ClipboardManager.class);
            if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("dua", p.text));
            Toast.makeText(this, L.t("copied"), Toast.LENGTH_SHORT).show();
        });
        row.addView(copy);
        return row;
    }

    private TextView round(String glyph) {
        TextView t = new TextView(this);
        t.setText(glyph);
        t.setTextSize(18);
        t.setTextColor(0xFF2E7D32);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setBackground(k.rounded(Theme.ov(0xFF), 22, 0xFFDDDDDD));
        t.setLayoutParams(new LinearLayout.LayoutParams(k.dp(42), k.dp(42)));
        return t;
    }

    private View bar() {
        View v = new View(this);
        v.setBackgroundColor(Theme.ov(0xCC));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(k.dp(2), k.dp(30));
        lp.setMargins(k.dp(30), 0, k.dp(30), 0);
        v.setLayoutParams(lp);
        return v;
    }

    // ---------------------------------------------------------------- icons
    static String glyphOf(String cat) {
        for (String[] c : DuaActivity.CATS) if (c[0].equals(cat)) return c[1];
        return "🤲";
    }

    /** Round dark icon box; the name of Allah is drawn in orange inside a dotted ring. */
    static TextView icon(Context ctx, Kit k, String glyph, float sizeDp) {
        TextView icon = new TextView(ctx);
        icon.setText(glyph);
        icon.setGravity(Gravity.CENTER);
        icon.setIncludeFontPadding(false);
        boolean allah = glyph.equals("ﷲ");
        if (allah) {
            icon.setTypeface(k.amiriBold);
            icon.setTextColor(ORANGE);
            icon.setTextSize(sizeDp * 0.30f);
            icon.setBackground(new LayerDrawable(new Drawable[]{k.rounded(D_CARD, sizeDp / 2, Theme.ov(0x10)), new DotRing(k.dp(1.3f))}));
        } else {
            icon.setTextSize(sizeDp * 0.40f);
            icon.setBackground(k.rounded(D_CARD, sizeDp / 2, Theme.ov(0x10)));
        }
        Theme.elev(icon, k.dp(4));
        icon.setLayoutParams(new LinearLayout.LayoutParams(k.dp(sizeDp), k.dp(sizeDp)));
        return icon;
    }

    /** A ring of small orange dots. */
    static final class DotRing extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float r;

        DotRing(float dotRadius) {
            r = dotRadius;
            p.setColor(ORANGE);
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float cx = b.exactCenterX(), cy = b.exactCenterY();
            float rad = Math.min(b.width(), b.height()) * 0.40f;
            int n = 44;
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                c.drawCircle(cx + (float) (rad * Math.cos(a)), cy + (float) (rad * Math.sin(a)), r, p);
            }
        }

        @Override
        public void setAlpha(int a) {
            p.setAlpha(a);
        }

        @Override
        public void setColorFilter(ColorFilter f) {
            p.setColorFilter(f);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
