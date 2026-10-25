package com.ghora.zikrbar;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.ghora.zikrbar.Kit.*;
import static com.ghora.zikrbar.TasbihActivity.*;

/** Dua categories with icons. Each opens its list from assets/duas/&lt;id&gt;.json (added as texts arrive). */
public class DuaActivity extends Activity {

    /** id (file name and text key dua_&lt;id&gt;) and icon. */
    static final String[][] CATS = {
            {"all", "🤲"}, {"sleep", "🛏️"}, {"daily", "🕰️"},
            {"toilet", "🚻"}, {"food", "🍽️"}, {"joy", "😊"},
            {"distress", "😔"}, {"travel", "✈️"}, {"masjid", "🕌"},
            {"praise", "ﷲ"}, {"hajj", "🕋"}, {"sick", "🤒"},
            {"fasting", "🌙"}, {"funeral", "🥀"}, {"nature", "🌦️"},
            {"home", "🚪"}, {"marriage", "💞"}, {"protect", "🛡️"},
            {"manners", "🤝"},
    };

    private Kit k;
    private LinearLayout grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.darkWindow(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(28));

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
        TextView t = k.title(L.t("duas_t"), 21, D_TEXT);
        t.setPaddingRelative(k.dp(14), 0, 0, 0);
        h.addView(t);
        root.addView(h);

        // tabs: categories | my duas (favourites)
        LinearLayout tabs = k.row();
        tabs.setPadding(k.dp(16), k.dp(14), k.dp(16), 0);
        TextView catTab = k.strong(L.t("categories"), 15, D_TEXT);
        catTab.setGravity(Gravity.CENTER);
        catTab.setPadding(0, k.dp(10), 0, k.dp(12));
        catTab.setBackground(k.rounded(Theme.ov(0x2E), 18, 0));
        tabs.addView(catTab, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView favTab = k.strong("\u2B50 " + L.t("my_duas"), 15, D_TEXT);
        favTab.setGravity(Gravity.CENTER);
        favTab.setPadding(0, k.dp(10), 0, k.dp(12));
        favTab.setOnClickListener(v -> startActivity(new Intent(this, DuaListActivity.class)
                .putExtra(DuaListActivity.EXTRA_CAT, "fav")));
        tabs.addView(favTab, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(tabs);

        EditText search = new EditText(this);
        search.setHint("🔍  " + L.t("dua_search"));
        search.setHintTextColor(D_MUTED);
        search.setTextColor(D_TEXT);
        search.setTypeface(k.body);
        search.setTextSize(15);
        search.setSingleLine(true);
        search.setPadding(k.dp(18), k.dp(12), k.dp(18), k.dp(12));
        search.setBackground(k.glass(22));
        LinearLayout.LayoutParams sp = k.matchWrap();
        sp.setMargins(k.dp(16), k.dp(16), k.dp(16), k.dp(6));
        root.addView(search, sp);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                fill(s.toString().trim());
            }
        });

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(k.dp(8), 0, k.dp(8), 0);
        root.addView(grid);
        fill("");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void fill(String q) {
        grid.removeAllViews();
        LinearLayout row = null;
        int n = 0;
        for (String[] c : CATS) {
            String name = L.t("dua_" + c[0]);
            if (!q.isEmpty() && !name.toLowerCase().contains(q.toLowerCase())) continue;
            if (n % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, k.matchWrap());
            }
            row.addView(tile(c[0], c[1], name), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            n++;
        }
        if (row != null) while (n % 3 != 0) {
            row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            n++;
        }
    }

    private View tile(String id, String glyph, String name) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(k.dp(4), k.dp(14), k.dp(4), k.dp(10));
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(Theme.ov(0x30)), null, k.rounded(Theme.ov(0xFF), 20, 0)));

        t.addView(DuaListActivity.icon(this, k, glyph, 92));

        TextView label = k.strong(name, 13.5f, D_TEXT);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, k.dp(12), 0, 0);
        label.setMaxLines(2);
        t.addView(label);

        t.setOnClickListener(v -> startActivity(new Intent(this, DuaListActivity.class)
                .putExtra(DuaListActivity.EXTRA_CAT, id)));
        return t;
    }
}
