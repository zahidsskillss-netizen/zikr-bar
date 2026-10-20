package com.ghora.zikrbar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int GREEN = 0xFF1B5E20, GREEN2 = 0xFF2E7D32, DARK = 0xFF0F3D1F;
    private static final int INK = 0xFF16231A, MUTED = 0xFF5B695E, LINE = 0xFFDBE4DA;
    private static final int BG = 0xFFF2F5F1, SOFT = 0xFFF5F8F4, GOLD = 0xFFF3E3A5, GOLD_LINE = 0xFFC9A227;

    private Switch barSwitch, tasbihSwitch;
    private TextView currentZk, countText;
    private Spinner intervalSpinner, tasbihZkSpinner, targetSpinner;
    private EditText listInput;
    private final List<LinearLayout> tiles = new ArrayList<>();
    private boolean binding;
    private boolean waitingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(build());
        askNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingOverlay) {
            waitingOverlay = false;
            if (Settings.canDrawOverlays(this)) turnTasbihOn();
        }
        refresh();
    }

    // =============================== UI ===============================

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(0, 0, 0, dp(28));

        root.addView(header());
        root.addView(statusCard());
        root.addView(tasbihCard());
        root.addView(listCard());

        TextView credit = text("Ghora Graphics — غوره ګرافیکس", 13, MUTED, false);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, dp(24), 0, 0);
        root.addView(credit);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        return scroll;
    }

    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(dp(24), dp(28), dp(24), dp(26));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GREEN2, DARK});
        float rr = dp(32);
        bg.setCornerRadii(new float[]{0, 0, 0, 0, rr, rr, rr, rr});
        h.setBackground(bg);

        TextView glyph = new TextView(this);
        glyph.setText("ﷲ");
        glyph.setTypeface(Ui.amiriBold(this));
        glyph.setTextSize(58);
        glyph.setTextColor(GOLD);
        glyph.setGravity(Gravity.CENTER);
        h.addView(glyph);

        TextView title = new TextView(this);
        title.setText("ذکر بار");
        title.setTypeface(Ui.amiriBold(this));
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        h.addView(title);

        TextView sub = new TextView(this);
        sub.setText("فَاذْكُرُونِي أَذْكُرْكُمْ");
        sub.setTypeface(Ui.amiri(this));
        sub.setTextSize(18);
        sub.setTextColor(0xD9FFFFFF);
        sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        return h;
    }

    private View statusCard() {
        LinearLayout c = card("ذکر بار", "د الله تعالی مبارک نوم په status bar کې، او ذکر په خبرتیا کې");

        barSwitch = new Switch(this);
        c.addView(switchRow("روښانه", barSwitch));
        barSwitch.setOnCheckedChangeListener((b, on) -> {
            if (binding) return;
            if (on) startBar(); else stopBar();
        });

        c.addView(label("اوسنی ذکر"));
        currentZk = new TextView(this);
        currentZk.setTypeface(Ui.amiriBold(this));
        currentZk.setTextSize(19);
        currentZk.setTextColor(GREEN);
        currentZk.setGravity(Gravity.CENTER);
        currentZk.setPadding(dp(14), dp(10), dp(14), dp(12));
        currentZk.setBackground(rounded(SOFT, 14, LINE));
        c.addView(currentZk, matchWrap());

        c.addView(label("ذکر هر څو دقیقې وروسته بدل شي"));
        String[] labels = new String[Prefs.INTERVALS.length];
        for (int i = 0; i < labels.length; i++) labels[i] = Ui.fa(Prefs.INTERVALS[i]) + " دقیقې";
        intervalSpinner = spinner(labels);
        c.addView(intervalSpinner, matchWrap());
        intervalSpinner.setOnItemSelectedListener(onSelect(pos -> {
            int m = Prefs.INTERVALS[pos];
            if (m == Prefs.intervalMinutes(this)) return;
            Prefs.setIntervalMinutes(this, m);
            refreshService();
        }));
        return c;
    }

    private View tasbihCard() {
        LinearLayout c = card("تسبیح بټن", "د نورو اپونو له پاسه ښکاري؛ کېکاږئ چې وشمېرل شي");

        tasbihSwitch = new Switch(this);
        c.addView(switchRow("روښانه", tasbihSwitch));
        tasbihSwitch.setOnCheckedChangeListener((b, on) -> {
            if (binding) return;
            if (on) requestTasbih(); else turnTasbihOff();
        });

        c.addView(label("ډیزاین غوره کړئ"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < TasbihView.NAMES.length; i++) row.addView(tile(i));
        hs.addView(row);
        c.addView(hs, matchWrap());

        c.addView(label("د تسبیح ذکر"));
        tasbihZkSpinner = spinner(new String[0]);
        c.addView(tasbihZkSpinner, matchWrap());
        tasbihZkSpinner.setOnItemSelectedListener(onSelect(pos -> {
            if (pos == Prefs.tasbihIndex(this)) return;
            Prefs.setTasbihIndex(this, pos);
            Prefs.setCount(this, 0);
            refresh();
            refreshService();
        }));

        c.addView(label("هدف (څو ځله)"));
        String[] tl = new String[Prefs.TARGETS.length];
        for (int i = 0; i < tl.length; i++) tl[i] = Prefs.TARGETS[i] == 0 ? "بې حده" : Ui.fa(Prefs.TARGETS[i]) + " ځله";
        targetSpinner = spinner(tl);
        c.addView(targetSpinner, matchWrap());
        targetSpinner.setOnItemSelectedListener(onSelect(pos -> {
            int t = Prefs.TARGETS[pos];
            if (t == Prefs.target(this)) return;
            Prefs.setTarget(this, t);
            refreshService();
        }));

        LinearLayout countRow = new LinearLayout(this);
        countRow.setOrientation(LinearLayout.HORIZONTAL);
        countRow.setGravity(Gravity.CENTER_VERTICAL);
        countRow.setPadding(0, dp(16), 0, 0);
        TextView cl = text("شمېر:", 16, INK, true);
        countRow.addView(cl);
        countText = text("۰", 30, GREEN, true);
        countText.setPadding(dp(10), 0, dp(10), 0);
        countRow.addView(countText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView reset = button("صفر کول", false);
        reset.setOnClickListener(v -> {
            Prefs.setCount(this, 0);
            refresh();
            refreshService();
        });
        countRow.addView(reset);
        c.addView(countRow, matchWrap());

        TextView hint = text("کېکاږئ: شمېرل  ·  کش یې کړئ: ځای بدلول  ·  اوږد یې ونیسئ: صفر کول", 12, MUTED, false);
        hint.setPadding(0, dp(12), 0, 0);
        hint.setLineSpacing(0, 1.3f);
        c.addView(hint);
        return c;
    }

    private View tile(int style) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(6), dp(6), dp(6), dp(8));

        ScaledBox box = new ScaledBox(this);
        GradientDrawable boxBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFFE9EEF3, 0xFFCFD8E2});
        boxBg.setCornerRadius(dp(16));
        box.setBackground(boxBg);
        box.setClipToOutline(true);
        TasbihView preview = new TasbihView(this);
        preview.setState(style, "سُبْحَانَ اللَّهِ", 12, 33);
        box.addView(preview);
        t.addView(box, new LinearLayout.LayoutParams(dp(140), dp(160)));

        TextView name = text(Ui.fa(style + 1) + " · " + TasbihView.NAMES[style], 13, INK, true);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(6), 0, 0);
        t.addView(name);

        t.setOnClickListener(v -> {
            Prefs.setTasbihStyle(this, style);
            refresh();
            refreshService();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        t.setLayoutParams(lp);
        tiles.add(t);
        return t;
    }

    private View listCard() {
        LinearLayout c = card("ذکرونه", "هر ذکر په یوه کرښه کې ولیکئ؛ زیات، کم یا بدل یې کړئ");
        listInput = new EditText(this);
        listInput.setText(Prefs.rawList(this));
        listInput.setTypeface(Ui.amiri(this));
        listInput.setTextSize(18);
        listInput.setTextColor(INK);
        listInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        listInput.setSingleLine(false);
        listInput.setMinLines(6);
        listInput.setMaxLines(14);
        listInput.setGravity(Gravity.TOP | Gravity.START);
        listInput.setTextDirection(View.TEXT_DIRECTION_RTL);
        listInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        listInput.setBackground(rounded(SOFT, 14, LINE));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(12);
        c.addView(listInput, lp);

        TextView save = button("ساتل", true);
        save.setOnClickListener(v -> {
            Prefs.saveList(this, listInput.getText().toString());
            Toast.makeText(this, "ذکرونه وساتل شول", Toast.LENGTH_SHORT).show();
            refresh();
            refreshService();
        });
        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = dp(12);
        c.addView(save, sp);
        return c;
    }

    // ============================ actions ============================

    private void startBar() {
        if (!hasNotificationPermission()) {
            askNotificationPermission();
            Toast.makeText(this, "مهرباني وکړئ د خبرتیا (Notification) اجازه ورکړئ", Toast.LENGTH_LONG).show();
            refresh();
            return;
        }
        Prefs.setRunning(this, true);
        startForegroundService(new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_REFRESH));
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
            Toast.makeText(this, "Zikr Bar ته اجازه ورکړئ چې د نورو اپونو له پاسه ښکاره شي", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
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
            startForegroundService(new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_REFRESH));
        }
    }

    private void refresh() {
        if (barSwitch == null) return;
        binding = true;
        barSwitch.setChecked(Prefs.running(this));
        tasbihSwitch.setChecked(Prefs.overlay(this) && Settings.canDrawOverlays(this) && Prefs.running(this));
        currentZk.setText(Prefs.current(this));
        countText.setText(Ui.fa(Prefs.count(this)));

        for (int i = 0; i < Prefs.INTERVALS.length; i++) {
            if (Prefs.INTERVALS[i] == Prefs.intervalMinutes(this)) intervalSpinner.setSelection(i);
        }
        for (int i = 0; i < Prefs.TARGETS.length; i++) {
            if (Prefs.TARGETS[i] == Prefs.target(this)) targetSpinner.setSelection(i);
        }
        List<String> list = Prefs.list(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        tasbihZkSpinner.setAdapter(a);
        tasbihZkSpinner.setSelection(Prefs.tasbihIndex(this));

        int sel = Prefs.tasbihStyle(this);
        for (int i = 0; i < tiles.size(); i++) {
            tiles.get(i).setBackground(i == sel ? rounded(0x141B5E20, 20, GREEN) : null);
        }
        binding = false;
    }

    // ============================ helpers ============================

    private LinearLayout card(String title, String sub) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(18));
        c.setBackground(rounded(Color.WHITE, 22, LINE));
        c.setElevation(dp(1));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(dp(16), dp(16), dp(16), 0);
        c.setLayoutParams(lp);

        TextView t = text(title, 19, INK, true);
        c.addView(t);
        TextView s = text(sub, 13, MUTED, false);
        s.setPadding(0, dp(2), 0, dp(6));
        c.addView(s);
        return c;
    }

    private View switchRow(String label, Switch sw) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(4));
        TextView l = text(label, 16, INK, false);
        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        int[][] states = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{GREEN, 0xFFF1F1F1}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{0x801B5E20, 0x40000000}));
        row.addView(sw);
        return row;
    }

    private TextView label(String s) {
        TextView t = text(s, 14, INK, true);
        t.setPadding(0, dp(14), 0, dp(6));
        return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView button(String s, boolean filled) {
        TextView b = text(s, 15, filled ? Color.WHITE : GREEN, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(20), dp(12), dp(20), dp(12));
        GradientDrawable shape = rounded(filled ? GREEN : Color.WHITE, 14, filled ? GREEN : GREEN);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(filled ? 0x40FFFFFF : 0x201B5E20), shape, null));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        s.setBackground(rounded(SOFT, 14, LINE));
        s.setPadding(dp(8), dp(4), dp(8), dp(4));
        return s;
    }

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

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1.5f), stroke);
        return g;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
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
