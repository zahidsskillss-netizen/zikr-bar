package com.ghora.zikrbar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.ghora.zikrbar.Kit.*;

public class MainActivity extends Activity {

    private static final String AYAH = "فَاذْكُرُونِي أَذْكُرْكُمْ";

    private Kit k;
    private Switch barSwitch, tasbihSwitch, unlockSwitch;
    private TextView currentZk, countText, statBar, statCount, statGoal;
    private Spinner intervalSpinner, tasbihZkSpinner, targetSpinner, reminderSpinner;
    private final List<LinearLayout> tiles = new ArrayList<>();
    private boolean binding;
    private LinearLayout highlightBox;
    private boolean waitingOverlay, waitingUnlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.window(this);
        setContentView(build());
        askNotificationPermission();
        AdhkarAlarm.scheduleAll(this);
        PrayerAlarm.schedule(this);
        LearnAlarm.schedule(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingOverlay) {
            waitingOverlay = false;
            if (Settings.canDrawOverlays(this)) turnTasbihOn();
        }
        if (waitingUnlock) {
            waitingUnlock = false;
            if (Settings.canDrawOverlays(this)) {
                Prefs.setUnlockPopup(this, true);
                if (!Prefs.running(this)) startBar();
            }
        }
        refresh();
    }

    // =============================== UI ===============================

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(28));

        root.addView(header());
        root.addView(stats());
        highlightBox = new LinearLayout(this);
        highlightBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(highlightBox, k.matchWrap());
        root.addView(adhkarRow());
        root.addView(toolsRow());
        TextView words = k.button("\uD83D\uDCD6\u2003" + L.t("sw_title"), false, v -> startActivity(new Intent(this, SalahWordsActivity.class)));
        LinearLayout.LayoutParams wlp = k.matchWrap();
        wlp.setMargins(k.dp(16), k.dp(10), k.dp(16), 0);
        root.addView(words, wlp);
        root.addView(statusCard());
        root.addView(tasbihCard());
        root.addView(faceCard());
        root.addView(unlockCard());
        root.addView(reminderCard());
        root.addView(listCard());
        root.addView(moreCard());

        TextView credit = k.text(L.t("credit"), 13, MUTED);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(k.dp(16), k.dp(22), k.dp(16), 0);
        root.addView(credit);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(k.dp(18), k.dp(14), k.dp(18), k.dp(58));
        h.setBackground(k.headerBackground());

        // top bar: language chip + share
        LinearLayout top = k.row();
        TextView lang = k.strong("◐  " + L.NAMES[L.lang] + "  ▾", 13.5f, WHITE);
        lang.setPadding(k.dp(14), k.dp(6), k.dp(14), k.dp(8));
        lang.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), k.rounded(0x26FFFFFF, 18, 0x40FFFFFF), null));
        lang.setOnClickListener(v -> chooseLanguage());
        top.addView(lang);
        TextView theme = k.strong("\u2726  " + L.t(Theme.KEYS[Theme.id]), 13.5f, WHITE);
        theme.setPadding(k.dp(14), k.dp(6), k.dp(14), k.dp(8));
        theme.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), k.rounded(0x26FFFFFF, 18, 0x40FFFFFF), null));
        theme.setOnClickListener(v -> chooseTheme());
        LinearLayout.LayoutParams thp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        thp.setMarginStart(k.dp(8));
        top.addView(theme, thp);
        View spacer = new View(this);
        top.addView(spacer, k.weight());
        TextView share = k.strong("↗  " + L.t("share_now"), 13.5f, GOLD);
        share.setPadding(k.dp(14), k.dp(6), k.dp(14), k.dp(8));
        share.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), k.rounded(0x1A000000, 18, 0x66F3E3A5), null));
        share.setOnClickListener(v -> InfoActivity.share(this));
        top.addView(share);
        h.addView(top, k.matchWrap());

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        Theme.elev(logo, k.dp(8));
        logo.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        logo.setBackground(k.rounded(0x00000000, 24, 0));
        logo.setClipToOutline(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(k.dp(96), k.dp(96));
        lp.topMargin = k.dp(18);
        h.addView(logo, lp);

        TextView title = k.title(L.t("app_name"), 34, WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, k.dp(14), 0, k.dp(6));
        h.addView(title);

        h.addView(k.arabic(AYAH, 19, 0xE6FFFFFF));
        return h;
    }

    private void chooseTheme() {
        String[] names = new String[Theme.KEYS.length];
        for (int i = 0; i < names.length; i++) names[i] = L.t(Theme.KEYS[i]);
        new AlertDialog.Builder(this)
                .setTitle(L.t("choose_theme"))
                .setSingleChoiceItems(names, Theme.id, (d, which) -> {
                    d.dismiss();
                    if (which == Theme.id) return;
                    Theme.save(this, which);
                    Theme.load(this);
                    recreate();
                })
                .show();
    }

    private void chooseLanguage() {
        new AlertDialog.Builder(this)
                .setTitle(L.t("choose_lang"))
                .setSingleChoiceItems(L.NAMES, L.lang, (d, which) -> {
                    d.dismiss();
                    if (which == L.lang) return;
                    Prefs.setLang(this, which);
                    L.init(this);
                    refreshService();
                    recreate();
                })
                .show();
    }

    /** Summary strip overlapping the header. */
    private View stats() {
        LinearLayout s = k.row();
        s.setPadding(k.dp(8), k.dp(14), k.dp(8), k.dp(14));
        s.setBackground(k.glass(22));
        Theme.elev(s, k.dp(5));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(20), -k.dp(38), k.dp(20), 0);
        s.setLayoutParams(lp);

        statBar = statCell(s, L.t("stat_status"));
        s.addView(divider());
        statCount = statCell(s, L.t("stat_count"));
        s.addView(divider());
        statGoal = statCell(s, L.t("stat_goal"));
        return s;
    }

    private TextView statCell(LinearLayout parent, String label) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        TextView v = k.title("", 20, GREEN);
        v.setGravity(Gravity.CENTER);
        cell.addView(v);
        TextView l = k.strong(label, 12, MUTED);
        l.setGravity(Gravity.CENTER);
        l.setPadding(0, k.dp(4), 0, 0);
        cell.addView(l);
        parent.addView(cell, k.weight());
        return v;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(k.dp(1), k.dp(36)));
        return v;
    }

    /** Three quick tiles: morning adhkar, evening adhkar, and the page with all adhkar categories. */
    private View adhkarRow() {
        LinearLayout row = k.row();
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(12), k.dp(14), k.dp(12), 0);
        row.setLayoutParams(lp);
        String[][] tiles = {{"\u2600\uFE0E", "morning"}, {"\u263E\uFE0E", "evening"}, {"\u06DE", "adhkar_all"}};
        int[] tabs = {AdhkarData.MORNING, AdhkarData.EVENING, AdhkarData.HUB};
        for (int i = 0; i < tiles.length; i++) {
            final int tab = tabs[i];
            LinearLayout t = new LinearLayout(this);
            t.setOrientation(LinearLayout.VERTICAL);
            t.setGravity(Gravity.CENTER);
            t.setPadding(k.dp(6), k.dp(14), k.dp(6), k.dp(14));
            t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.glass(22), null));
            Theme.elev(t, k.dp(1.5f));
            TextView b = k.badge(tiles[i][0], 44);
            t.addView(b);
            TextView name = k.title(L.t(tiles[i][1]), 13.5f, INK);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, k.dp(12), 0, 0);
            t.addView(name);
            t.setOnClickListener(v -> startActivity(new Intent(this, AdhkarActivity.class).putExtra(AdhkarActivity.EXTRA_TAB, tab)));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tp.setMargins(k.dp(4), 0, k.dp(4), 0);
            row.addView(t, tp);
        }
        return row;
    }

    /** Second row of tiles: the tasbih page, duas and prayer times. */
    private View toolsRow() {
        LinearLayout row = k.row();
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(12), k.dp(8), k.dp(12), 0);
        row.setLayoutParams(lp);
        String[][] tiles = {{"\uD83D\uDCFF", "tile_tasbih"}, {"\uD83E\uDD32", "tile_duas"}, {"\uD83D\uDD4C", "tile_prayer"}};
        Class<?>[] pages = {TasbihActivity.class, DuaActivity.class, PrayerActivity.class};
        for (int i = 0; i < tiles.length; i++) {
            final Class<?> page = pages[i];
            LinearLayout t = new LinearLayout(this);
            t.setOrientation(LinearLayout.VERTICAL);
            t.setGravity(Gravity.CENTER);
            t.setPadding(k.dp(6), k.dp(14), k.dp(6), k.dp(14));
            t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.glass(22), null));
            Theme.elev(t, k.dp(1.5f));
            TextView b = k.badge(tiles[i][0], 44);
            b.setTextSize(20);
            t.addView(b);
            TextView name = k.title(L.t(tiles[i][1]), 13.5f, INK);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, k.dp(12), 0, 0);
            t.addView(name);
            t.setOnClickListener(v -> startActivity(new Intent(this, page)));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tp.setMargins(k.dp(4), 0, k.dp(4), 0);
            row.addView(t, tp);
        }
        return row;
    }

    // ---------------------------------------------------------------- today's highlight

    /** The adhkar for this time of day. */
    private int adhkarNow() {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (h >= 21 || h < 3) return AdhkarData.SLEEP;
        if (h >= 15) return AdhkarData.EVENING;
        if (h >= 12 && AdhkarData.allDone(this, AdhkarData.MORNING)) return AdhkarData.EVENING;
        return AdhkarData.MORNING;
    }

    private void fillHighlight() {
        highlightBox.removeAllViews();
        LinearLayout c = k.card(null, L.t("hl_title"), null);

        int cat = adhkarNow();
        AdhkarData.Item[] items = AdhkarData.items(this, cat);
        if (items.length > 0) {
            int[] left = AdhkarData.left(this, cat);
            int done = AdhkarData.doneCount(left);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(k.dp(14), k.dp(12), k.dp(14), k.dp(14));
            box.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.rounded(SOFT, 18, LINE), null));
            LinearLayout top = k.row();
            top.addView(k.iconGap(k.badge(AdhkarData.glyph(cat), 34), 12));
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.addView(k.title(L.t(AdhkarData.nameKey(cat)), 16, INK));
            TextView pr = k.strong(done == items.length ? L.t("done") : L.f("prog", L.num(done), L.num(items.length)), 12.5f,
                    done > 0 ? GREEN2 : MUTED);
            pr.setPadding(0, k.dp(2), 0, 0);
            tx.addView(pr);
            top.addView(tx, k.weight());
            box.addView(top);
            android.widget.ProgressBar bar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(items.length);
            bar.setProgress(done);
            bar.setProgressTintList(ColorStateList.valueOf(GOLD_LINE));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(Theme.ov(0x22)));
            box.addView(bar, k.topMargin(8));
            int next = -1;
            for (int i = 0; i < left.length; i++) if (left[i] > 0) { next = i; break; }
            if (next >= 0) {
                TextView ar = k.arabic(items[next].text, 18, INK);
                ar.setMaxLines(2);
                ar.setEllipsize(android.text.TextUtils.TruncateAt.END);
                ar.setTextDirection(View.TEXT_DIRECTION_RTL);
                ar.setPadding(0, k.dp(10), 0, k.dp(4));
                box.addView(ar);
            }
            String go = done == items.length ? L.t("hl_again") : done > 0 ? L.t("hl_continue") : L.t("hl_start");
            TextView b = k.button(go + "  " + (L.rtl() ? "\u2190" : "\u2192"), done < items.length, null);
            box.addView(b, k.topMargin(10));
            final int fc = cat;
            View.OnClickListener open = v -> startActivity(new Intent(this, AdhkarActivity.class).putExtra(AdhkarActivity.EXTRA_TAB, fc));
            b.setOnClickListener(open);
            box.setOnClickListener(open);
            c.addView(box, k.topMargin(8));
        }

        DuaData.Dua day = DuaData.ofDay(this);
        if (day != null && day.parts.length > 0) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(k.dp(14), k.dp(12), k.dp(14), k.dp(14));
            box.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.rounded(SOFT, 18, LINE), null));
            LinearLayout top = k.row();
            top.addView(k.iconGap(k.badge("\u2766", 34), 12));
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.addView(k.title(L.t("hl_dua"), 16, INK));
            TextView dt = k.text(day.title(), 12.5f, MUTED);
            dt.setMaxLines(1);
            dt.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tx.addView(dt);
            top.addView(tx, k.weight());
            box.addView(top);
            TextView ar = k.arabic(day.parts[0].text, 18, INK);
            ar.setMaxLines(3);
            ar.setEllipsize(android.text.TextUtils.TruncateAt.END);
            ar.setTextDirection(View.TEXT_DIRECTION_RTL);
            ar.setPadding(0, k.dp(10), 0, 0);
            box.addView(ar);
            box.setOnClickListener(v -> startActivity(new Intent(this, DuaListActivity.class).putExtra(DuaListActivity.EXTRA_DUA, day.id())));
            c.addView(box, k.topMargin(10));
        }

        DuaData.Dua last = DuaData.last(this);
        if (last != null) {
            TextView cont = k.button(L.t("hl_last") + "  " + last.title(), false,
                    v -> startActivity(new Intent(this, DuaListActivity.class).putExtra(DuaListActivity.EXTRA_DUA, last.id())));
            cont.setMaxLines(1);
            cont.setEllipsize(android.text.TextUtils.TruncateAt.END);
            c.addView(cont, k.topMargin(10));
        }
        highlightBox.addView(c);
    }

    private View statusCard() {
        LinearLayout c = k.card("ﷲ", L.t("c_bar_t"), L.t("c_bar_s"));

        barSwitch = new Switch(this);
        c.addView(switchRow(L.t("enabled"), barSwitch));
        barSwitch.setOnCheckedChangeListener((b, on) -> {
            if (binding) return;
            if (on) startBar(); else stopBar();
        });

        c.addView(k.label(L.t("current_zikr")));
        currentZk = k.arabic("", 20, GREEN);
        currentZk.setPadding(k.dp(14), k.dp(12), k.dp(14), k.dp(14));
        currentZk.setBackground(k.rounded(SOFT, 16, LINE));
        c.addView(currentZk, k.matchWrap());

        c.addView(k.label(L.t("interval")));
        String[] labels = new String[Prefs.INTERVALS.length];
        for (int i = 0; i < labels.length; i++) {
            int m = Prefs.INTERVALS[i];
            labels[i] = m == 1 ? L.t("minute1") : L.f("minutes", L.num(m));
        }
        intervalSpinner = k.spinner(labels, false);
        c.addView(intervalSpinner, k.matchWrap());
        intervalSpinner.setOnItemSelectedListener(onSelect(pos -> {
            int m = Prefs.INTERVALS[pos];
            if (m == Prefs.intervalMinutes(this)) return;
            Prefs.setIntervalMinutes(this, m);
            refreshService();
        }));
        return c;
    }

    private View tasbihCard() {
        LinearLayout c = k.card(L.num(33), L.t("c_tas_t"), L.t("c_tas_s"));

        tasbihSwitch = new Switch(this);
        c.addView(switchRow(L.t("enabled"), tasbihSwitch));
        tasbihSwitch.setOnCheckedChangeListener((b, on) -> {
            if (binding) return;
            if (on) requestTasbih(); else turnTasbihOff();
        });

        c.addView(k.label(L.t("choose_design")));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < TasbihView.COUNT; i++) row.addView(tile(TasbihView.STYLES[i], i));
        hs.addView(row);
        c.addView(hs, k.matchWrap());

        // wheel designs: "krrr" sound and vibration while rolling
        Switch rollSnd = new Switch(this);
        rollSnd.setChecked(Prefs.rollSound(this));
        rollSnd.setOnCheckedChangeListener((b, on) -> Prefs.setRollSound(this, on));
        c.addView(switchRow(L.t("roll_snd"), rollSnd));
        Switch rollVib = new Switch(this);
        rollVib.setChecked(Prefs.rollVib(this));
        rollVib.setOnCheckedChangeListener((b, on) -> Prefs.setRollVib(this, on));
        c.addView(switchRow(L.t("roll_vib"), rollVib));
        TextView rollNote = k.text(L.t("roll_note"), 12.5f, MUTED);
        rollNote.setPadding(k.dp(4), k.dp(6), k.dp(4), 0);
        c.addView(rollNote);

        c.addView(k.label(L.t("tas_zikr")));
        tasbihZkSpinner = k.spinner(new String[0], true);
        c.addView(tasbihZkSpinner, k.matchWrap());
        tasbihZkSpinner.setOnItemSelectedListener(onSelect(pos -> {
            if (pos == Prefs.tasbihIndex(this)) return;
            Prefs.setTasbihIndex(this, pos);
            Prefs.setCount(this, 0);
            refresh();
            refreshService();
        }));

        c.addView(k.label(L.t("goal_label")));
        String[] tl = new String[Prefs.TARGETS.length];
        for (int i = 0; i < tl.length; i++) {
            tl[i] = Prefs.TARGETS[i] == 0 ? L.t("unlimited") : L.f("times", L.num(Prefs.TARGETS[i]));
        }
        targetSpinner = k.spinner(tl, false);
        c.addView(targetSpinner, k.matchWrap());
        targetSpinner.setOnItemSelectedListener(onSelect(pos -> {
            int t = Prefs.TARGETS[pos];
            if (t == Prefs.target(this)) return;
            Prefs.setTarget(this, t);
            refresh();
            refreshService();
        }));

        LinearLayout countRow = k.row();
        countRow.setPadding(k.dp(14), k.dp(10), k.dp(14), k.dp(10));
        countRow.setBackground(k.rounded(SOFT, 16, LINE));
        countRow.addView(k.title(L.t("count"), 15, INK));
        countText = k.title("0", 30, GREEN);
        countText.setPadding(k.dp(12), 0, k.dp(12), 0);
        countRow.addView(countText, k.weight());
        countRow.addView(k.button(L.t("reset"), false, v -> {
            Prefs.setCount(this, 0);
            refresh();
            refreshService();
        }));
        c.addView(countRow, k.topMargin(16));

        c.addView(k.button(L.t("orig_size"), false, v -> {
            Prefs.setScale(this, 1f);
            refreshService();
            Toast.makeText(this, L.t("size_reset"), Toast.LENGTH_SHORT).show();
        }), k.topMargin(10));

        // collapsible "how to use"
        LinearLayout howBox = new LinearLayout(this);
        howBox.setOrientation(LinearLayout.VERTICAL);
        howBox.setVisibility(View.GONE);
        String[][] howTo = {
                {"h_tap_k", "h_tap_v"}, {"h_drag_k", "h_drag_v"}, {"h_pinch_k", "h_pinch_v"},
                {"h_edge_k", "h_edge_v"}, {"h_switch_k", "h_switch_v"}, {"h_close_k", "h_close_v"}, {"h_long_k", "h_long_v"},
                {"h_roll_k", "h_roll_v"}, {"h_grip_k", "h_grip_v"}
        };
        for (String[] h : howTo) howBox.addView(howRow(L.t(h[0]), L.t(h[1])));

        TextView howToggle = k.strong(L.t("howto") + "  ▾", 14.5f, GREEN);
        howToggle.setPadding(k.dp(14), k.dp(10), k.dp(14), k.dp(12));
        howToggle.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.rounded(0x0F1B5E20, 16, 0), null));
        howToggle.setOnClickListener(v -> {
            boolean show = howBox.getVisibility() != View.VISIBLE;
            howBox.setVisibility(show ? View.VISIBLE : View.GONE);
            howToggle.setText(L.t("howto") + (show ? "  ▴" : "  ▾"));
        });
        c.addView(howToggle, k.topMargin(14));
        c.addView(howBox, k.topMargin(4));
        return c;
    }

    private View howRow(String key, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, k.dp(5), 0, k.dp(5));
        TextView kk = k.strong(key, 12.5f, GREEN);
        kk.setGravity(Gravity.CENTER);
        kk.setPadding(k.dp(8), k.dp(4), k.dp(8), k.dp(6));
        kk.setBackground(k.rounded(0x141B5E20, 10, 0));
        row.addView(kk, new LinearLayout.LayoutParams(k.dp(104), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView d = k.text(desc, 14, MUTED);
        d.setPadding(k.dp(10), k.dp(2), k.dp(10), 0);
        row.addView(d, k.weight());
        return row;
    }

    private View tile(int style, int index) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(k.dp(6), k.dp(6), k.dp(6), k.dp(8));

        ScaledBox box = new ScaledBox(this);
        GradientDrawable boxBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFFE9EEF3, 0xFFCFD8E2});
        boxBg.setCornerRadius(k.dp(18));
        box.setBackground(boxBg);
        box.setClipToOutline(true);
        TasbihView preview = new TasbihView(this);
        preview.setState(style, "سُبْحَانَ اللَّهِ", 12, 33);
        box.addView(preview);
        t.addView(box, new LinearLayout.LayoutParams(k.dp(140), k.dp(160)));

        TextView name = k.strong(L.num(index + 1) + " · " + TasbihView.name(style), 13, INK);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, k.dp(6), 0, 0);
        t.addView(name);

        t.setOnClickListener(v -> {
            Prefs.setTasbihStyle(this, style);
            refresh();
            refreshService();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(k.dp(8));
        t.setLayoutParams(lp);
        tiles.add(t);
        return t;
    }

    /** Separate option: count dhikr by blinking or by moving the lips, seen by the front camera. */
    private View faceCard() {
        LinearLayout c = k.card("\u25C9", L.t("c_face_t"), L.t("c_face_s"));
        c.addView(k.label(L.t("face_mode")));
        Spinner mode = k.spinner(new String[]{L.t("face_blink"), L.t("face_mouth")}, false);
        mode.setSelection(Prefs.faceMode(this) == 1 ? 1 : 0);
        mode.setOnItemSelectedListener(onSelect(pos -> Prefs.setFaceMode(this, pos)));
        c.addView(mode, k.matchWrap());
        c.addView(k.button(L.t("face_open"), true, v -> startActivity(new Intent(this, FaceZikrActivity.class))), k.topMargin(12));
        return c;
    }

    private View unlockCard() {
        LinearLayout c = k.card("☀", L.t("c_unl_t"), L.t("c_unl_s"));
        unlockSwitch = new Switch(this);
        c.addView(switchRow(L.t("enabled"), unlockSwitch));
        unlockSwitch.setOnCheckedChangeListener((b, on) -> {
            if (binding) return;
            if (!on) {
                Prefs.setUnlockPopup(this, false);
                refresh();
                return;
            }
            if (!Settings.canDrawOverlays(this)) {
                waitingUnlock = true;
                askOverlay();
                refresh();
                return;
            }
            Prefs.setUnlockPopup(this, true);
            if (!Prefs.running(this)) startBar();
            refresh();
        });
        TextView note = k.text(L.t("unl_note"), 13, MUTED);
        note.setPadding(0, k.dp(10), 0, 0);
        c.addView(note);

        c.addView(k.label(L.t("us_choose")));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] marks = {"\u25A4", "\u2B2D", "\u21BB", "\u25CB", "\u2738", "\u2229", "\u2581", "\u2685"};
        for (int i = 0; i <= UnlockStyles.ROTATE; i++) {
            final int st = i;
            LinearLayout t = new LinearLayout(this);
            t.setOrientation(LinearLayout.VERTICAL);
            t.setGravity(Gravity.CENTER);
            t.setPadding(k.dp(10), k.dp(12), k.dp(10), k.dp(10));
            TextView m = k.strong(marks[i], 22, GOLD_LINE);
            m.setGravity(Gravity.CENTER);
            t.addView(m);
            TextView n = k.strong(L.t(UnlockStyles.KEYS[i]), 12.5f, INK);
            n.setGravity(Gravity.CENTER);
            n.setPadding(0, k.dp(6), 0, 0);
            t.addView(n);
            t.setOnClickListener(v -> {
                Prefs.setUnlockStyle(this, st);
                paintStyles();
                previewUnlock();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(k.dp(92), LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(k.dp(8));
            row.addView(t, lp);
            styleTiles.add(t);
        }
        hs.addView(row);
        c.addView(hs, k.matchWrap());
        c.addView(k.button("\u25B6\u2003" + L.t("us_preview"), false, v -> previewUnlock()), k.topMargin(12));
        paintStyles();
        return c;
    }

    private final List<LinearLayout> styleTiles = new ArrayList<>();

    private void paintStyles() {
        int sel = Prefs.unlockStyle(this);
        for (int i = 0; i < styleTiles.size(); i++) {
            styleTiles.get(i).setBackground(i == sel ? k.rounded(Theme.ov(0x1A), 18, GOLD_LINE) : k.rounded(SOFT, 18, LINE));
        }
    }

    private void previewUnlock() {
        if (!Settings.canDrawOverlays(this)) {
            waitingUnlock = true;
            askOverlay();
            return;
        }
        if (!Prefs.running(this)) {
            Toast.makeText(this, L.t("us_need_on"), Toast.LENGTH_SHORT).show();
            return;
        }
        Ui.startZikrService(this, new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_PREVIEW_UNLOCK));
    }

    private View reminderCard() {
        LinearLayout c = k.card("☾", L.t("c_rem_t"), L.t("c_rem_s"));
        TextView ayah = k.arabic(AYAH, 22, GREEN);
        ayah.setPadding(k.dp(12), k.dp(10), k.dp(12), k.dp(12));
        ayah.setBackground(k.rounded(SOFT, 16, LINE));
        c.addView(ayah, k.topMargin(6));

        c.addView(k.label(L.t("after_hours")));
        String[] labels = new String[Prefs.REMINDER_HOURS.length];
        for (int i = 0; i < labels.length; i++) {
            int h = Prefs.REMINDER_HOURS[i];
            labels[i] = h == 0 ? L.t("rem_off") : h == 1 ? L.t("hour1") : L.f("hours", L.num(h));
        }
        reminderSpinner = k.spinner(labels, false);
        c.addView(reminderSpinner, k.matchWrap());
        reminderSpinner.setOnItemSelectedListener(onSelect(pos -> {
            int h = Prefs.REMINDER_HOURS[pos];
            if (h == Prefs.reminderHours(this)) return;
            Prefs.setReminderHours(this, h);
            Prefs.setLastReminder(this, 0);
        }));

        TextView note = k.text(L.t("quiet"), 13, MUTED);
        note.setPadding(0, k.dp(10), 0, 0);
        c.addView(note);
        return c;
    }

    private View listCard() {
        LinearLayout c = k.card("\u2261", L.t("c_list_t"), L.t("le_sub"));
        c.addView(ListEditor.inline(this, k, () -> {
            refresh();
            refreshService();
        }), k.topMargin(4));
        return c;
    }

    private View moreCard() {
        LinearLayout c = k.card("✦", L.t("c_more_t"), L.t("c_more_s"));
        c.addView(navRow(k.badge("\u2665\uFE0E", 38), L.t("nav_donate"), "HesabPay · Atoma Pay · Payoneer", InfoActivity.PAGE_DONATE));
        c.addView(navRow(k.badge("ﷲ", 38), L.t("nav_about"), L.t("nav_about_s"), InfoActivity.PAGE_ABOUT));
        ImageView ph = k.photo(38);
        c.addView(navRow(ph, L.t("nav_dev"), L.t("dev_name") + " · " + L.t("ghora"), InfoActivity.PAGE_DEV));
        c.addView(k.button(L.t("share_btn"), true, v -> InfoActivity.share(this)), k.topMargin(12));
        return c;
    }

    private View navRow(View icon, String title, String sub, int page) {
        LinearLayout r = k.row();
        r.setPadding(k.dp(4), k.dp(10), k.dp(4), k.dp(10));
        r.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), null, k.rounded(CARD, 14, 0)));
        r.setClickable(true);
        r.addView(k.iconGap(icon, 14));

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(k.dp(4), 0, k.dp(4), 0);
        tx.addView(k.title(title, 16, INK));
        TextView s = k.text(sub, 13, MUTED);
        s.setPadding(0, k.dp(3), 0, 0);
        s.setMaxLines(2);
        s.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tx.addView(s);
        r.addView(tx, k.weight());

        r.addView(k.chevron());
        r.setOnClickListener(v -> startActivity(new Intent(this, InfoActivity.class).putExtra(InfoActivity.EXTRA_PAGE, page)));
        return r;
    }

    private View switchRow(String label, Switch sw) {
        LinearLayout row = k.row();
        row.setPaddingRelative(k.dp(14), k.dp(8), k.dp(10), k.dp(8));
        row.setBackground(k.rounded(SOFT, 16, LINE));
        row.addView(k.title(label, 16, INK), k.weight());
        int[][] states = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{GREEN, 0xFFF1F1F1}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{0x801B5E20, 0x40000000}));
        row.addView(sw);
        row.setLayoutParams(k.topMargin(8));
        return row;
    }

    // ============================ actions ============================

    private void askOverlay() {
        Toast.makeText(this, L.t("overlay_perm"), Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    }

    private void startBar() {
        if (!hasNotificationPermission()) {
            askNotificationPermission();
            Toast.makeText(this, L.t("notif_perm"), Toast.LENGTH_LONG).show();
            refresh();
            return;
        }
        Prefs.setRunning(this, true);
        Ui.startZikrService(this, new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_REFRESH));
        refresh();
    }

    private void stopBar() {
        Prefs.setRunning(this, false);
        Prefs.setOverlay(this, false);
        stopService(new Intent(this, ZikrService.class));
        refresh();
    }

    private void requestTasbih() {
        if (!Settings.canDrawOverlays(this)) {
            waitingOverlay = true;
            askOverlay();
            refresh();
            return;
        }
        turnTasbihOn();
    }

    private void turnTasbihOn() {
        Prefs.setOverlay(this, true);
        if (Prefs.running(this)) refreshService(); else startBar();
        refresh();
    }

    private void turnTasbihOff() {
        Prefs.setOverlay(this, false);
        refreshService();
        refresh();
    }

    private void refreshService() {
        if (Prefs.running(this)) {
            Ui.startZikrService(this, new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_REFRESH));
        }
    }

    private void refresh() {
        if (barSwitch == null) return;
        fillHighlight();
        binding = true;
        boolean running = Prefs.running(this);
        boolean canDraw = Settings.canDrawOverlays(this);
        barSwitch.setChecked(running);
        tasbihSwitch.setChecked(Prefs.overlay(this) && canDraw && running);
        unlockSwitch.setChecked(Prefs.unlockPopup(this) && canDraw && running);
        currentZk.setText(Prefs.current(this));
        countText.setText(L.num(Prefs.count(this)));

        statBar.setText(running ? L.t("on") : L.t("off"));
        statBar.setTextColor(running ? GREEN : MUTED);
        statCount.setText(L.num(Prefs.count(this)));
        int t = Prefs.target(this);
        statGoal.setText(t == 0 ? "∞" : L.num(t));

        for (int i = 0; i < Prefs.INTERVALS.length; i++) {
            if (Prefs.INTERVALS[i] == Prefs.intervalMinutes(this)) intervalSpinner.setSelection(i);
        }
        for (int i = 0; i < Prefs.TARGETS.length; i++) {
            if (Prefs.TARGETS[i] == t) targetSpinner.setSelection(i);
        }
        for (int i = 0; i < Prefs.REMINDER_HOURS.length; i++) {
            if (Prefs.REMINDER_HOURS[i] == Prefs.reminderHours(this)) reminderSpinner.setSelection(i);
        }
        List<String> list = Prefs.list(this);
        tasbihZkSpinner.setAdapter(k.adapter(list.toArray(new String[0]), true));
        tasbihZkSpinner.setSelection(Prefs.tasbihIndex(this));

        int sel = Prefs.tasbihStyle(this);
        for (int i = 0; i < tiles.size(); i++) {
            tiles.get(i).setBackground(TasbihView.STYLES[i] == sel ? k.rounded(0x141B5E20, 22, GREEN) : null);
        }
        binding = false;
    }

    // ============================ helpers ============================

    private interface OnPos { void on(int pos); }

    private AdapterView.OnItemSelectedListener onSelect(OnPos f) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!binding) f.on(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void askNotificationPermission() {
        if (!hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    /** Shows a child view centered and shrunk to fit, for the design previews. */
    static class ScaledBox extends FrameLayout {
        ScaledBox(Context c) {
            super(c);
        }

        @Override
        protected void onMeasure(int w, int h) {
            super.onMeasure(w, h);
            View ch = getChildAt(0);
            if (ch != null) ch.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            View ch = getChildAt(0);
            if (ch == null) return;
            int bw = r - l, bh = b - t, cw = ch.getMeasuredWidth(), chh = ch.getMeasuredHeight();
            int x = (bw - cw) / 2, y = (bh - chh) / 2;
            ch.layout(x, y, x + cw, y + chh);
            float s = Math.min(1f, Math.min(bw * 0.94f / cw, bh * 0.94f / chh));
            ch.setPivotX(cw / 2f);
            ch.setPivotY(chh / 2f);
            ch.setScaleX(s);
            ch.setScaleY(s);
        }
    }
}
