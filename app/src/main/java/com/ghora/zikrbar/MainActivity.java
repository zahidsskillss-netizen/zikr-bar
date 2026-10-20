package com.ghora.zikrbar;

import android.Manifest;
import android.app.Activity;
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

    private Kit k;
    private Switch barSwitch, tasbihSwitch;
    private TextView currentZk, countText, statBar, statCount, statGoal;
    private Spinner intervalSpinner, tasbihZkSpinner, targetSpinner, reminderSpinner;
    private EditText listInput;
    private final List<LinearLayout> tiles = new ArrayList<>();
    private boolean binding;
    private boolean waitingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        k = new Kit(this);
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
        root.setPadding(0, 0, 0, k.dp(28));

        root.addView(header());
        root.addView(stats());
        root.addView(statusCard());
        root.addView(tasbihCard());
        root.addView(reminderCard());
        root.addView(listCard());
        root.addView(moreCard());

        TextView credit = k.text("جوړونکی: زاهدالله الکوزی · غوره ګرافیکس پروډکشن", 12, MUTED);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, k.dp(22), 0, 0);
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
        h.setPadding(k.dp(24), k.dp(26), k.dp(24), k.dp(56));
        h.setBackground(k.headerBackground());

        TextView glyph = k.arabic("ﷲ", 44, GOLD);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(0x22000000);
        ring.setStroke(k.dp(1.5f), 0x99F3E3A5);
        glyph.setBackground(ring);
        glyph.setIncludeFontPadding(false);
        h.addView(glyph, new LinearLayout.LayoutParams(k.dp(92), k.dp(92)));

        TextView title = k.title("ذکر بار", 34, WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, k.dp(12), 0, k.dp(4));
        h.addView(title);

        TextView sub = k.arabic("فَاذْكُرُونِي أَذْكُرْكُمْ", 19, 0xE6FFFFFF);
        h.addView(sub);
        return h;
    }

    /** Summary strip overlapping the header. */
    private View stats() {
        LinearLayout s = k.row();
        s.setPadding(k.dp(8), k.dp(14), k.dp(8), k.dp(14));
        s.setBackground(k.rounded(WHITE, 22, LINE));
        s.setElevation(k.dp(4));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(20), -k.dp(38), k.dp(20), 0);
        s.setLayoutParams(lp);

        statBar = statCell(s, "ذکر بار");
        s.addView(divider());
        statCount = statCell(s, "تسبیح");
        s.addView(divider());
        statGoal = statCell(s, "هدف");
        return s;
    }

    private TextView statCell(LinearLayout parent, String label) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        TextView v = k.title("", 20, GREEN);
        v.setGravity(Gravity.CENTER);
        cell.addView(v);
        TextView l = k.text(label, 12, MUTED);
        l.setGravity(Gravity.CENTER);
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

    private View statusCard() {
        LinearLayout c = k.card("ﷲ", "ذکر بار", "د الله تعالی مبارک نوم په status bar کې، او ذکر په خبرتیا کې");

        barSwitch = new Switch(this);
        c.addView(switchRow("روښانه", barSwitch));
        barSwitch.setOnCheckedChangeListener((b, on) -> {
            if (binding) return;
            if (on) startBar(); else stopBar();
        });

        c.addView(k.label("اوسنی ذکر"));
        currentZk = k.arabic("", 20, GREEN);
        currentZk.setPadding(k.dp(14), k.dp(12), k.dp(14), k.dp(14));
        currentZk.setBackground(k.rounded(SOFT, 16, LINE));
        c.addView(currentZk, k.matchWrap());

        c.addView(k.label("ذکر هر څو دقیقې وروسته بدل شي"));
        String[] labels = new String[Prefs.INTERVALS.length];
        for (int i = 0; i < labels.length; i++) labels[i] = Ui.fa(Prefs.INTERVALS[i]) + " دقیقې";
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
        LinearLayout c = k.card("۳۳", "تسبیح بټن", "د نورو اپونو له پاسه ښکاري؛ کېکاږئ چې وشمېرل شي");

        tasbihSwitch = new Switch(this);
        c.addView(switchRow("روښانه", tasbihSwitch));
        tasbihSwitch.setOnCheckedChangeListener((b, on) -> {
            if (binding) return;
            if (on) requestTasbih(); else turnTasbihOff();
        });

        c.addView(k.label("ډیزاین غوره کړئ"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < TasbihView.NAMES.length; i++) row.addView(tile(i));
        hs.addView(row);
        c.addView(hs, k.matchWrap());

        c.addView(k.label("د تسبیح ذکر"));
        tasbihZkSpinner = k.spinner(new String[0], true);
        c.addView(tasbihZkSpinner, k.matchWrap());
        tasbihZkSpinner.setOnItemSelectedListener(onSelect(pos -> {
            if (pos == Prefs.tasbihIndex(this)) return;
            Prefs.setTasbihIndex(this, pos);
            Prefs.setCount(this, 0);
            refresh();
            refreshService();
        }));

        c.addView(k.label("هدف (څو ځله)"));
        String[] tl = new String[Prefs.TARGETS.length];
        for (int i = 0; i < tl.length; i++) tl[i] = Prefs.TARGETS[i] == 0 ? "بې حده" : Ui.fa(Prefs.TARGETS[i]) + " ځله";
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
        countRow.addView(k.title("شمېر", 15, INK));
        countText = k.title("۰", 30, GREEN);
        countText.setPadding(k.dp(12), 0, k.dp(12), 0);
        countRow.addView(countText, k.weight());
        countRow.addView(k.button("صفر کول", false, v -> {
            Prefs.setCount(this, 0);
            refresh();
            refreshService();
        }));
        c.addView(countRow, k.topMargin(16));

        c.addView(k.button("اصلي اندازه", false, v -> {
            Prefs.setScale(this, 1f);
            refreshService();
            Toast.makeText(this, "تسبیح خپلې اصلي اندازې ته راغی", Toast.LENGTH_SHORT).show();
        }), k.topMargin(10));

        c.addView(k.label("څنګه یې وکاروئ"));
        String[][] howTo = {
                {"کېکاږل", "یو ذکر شمېرل کیږي"},
                {"کش کول", "هر ځای ته یې یوسئ"},
                {"دوه ګوتې", "خلاصې یې کړئ: لوی · سره یې نږدې کړئ: کوچنی"},
                {"څنډې ته کش", "د سکرین ښي یا چپ اخر ته یې یوسئ: پټیږي او یوه کوچنۍ نښه پاتې کیږي؛ هغه کېکاږئ چې بېرته راشي"},
                {"لاندې ✕ ته", "تسبیح بندیږي"},
                {"اوږد نیول", "شمېر صفر کیږي"}
        };
        for (String[] h : howTo) c.addView(howRow(h[0], h[1]));
        return c;
    }

    private View howRow(String key, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, k.dp(4), 0, k.dp(4));
        TextView kk = k.text(key, 12.5f, GREEN);
        kk.setGravity(Gravity.CENTER);
        kk.setPadding(k.dp(8), k.dp(4), k.dp(8), k.dp(6));
        kk.setBackground(k.rounded(0x141B5E20, 10, 0));
        row.addView(kk, new LinearLayout.LayoutParams(k.dp(100), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView d = k.text(desc, 13, MUTED);
        d.setPadding(k.dp(10), k.dp(2), 0, 0);
        row.addView(d, k.weight());
        return row;
    }

    private View tile(int style) {
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

        TextView name = k.text(Ui.fa(style + 1) + " · " + TasbihView.NAMES[style], 13, INK);
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

    private View reminderCard() {
        LinearLayout c = k.card("☾", "د ذکر یادونه", "که په تسبیح کې مو دومره وخت ذکر نه وي کړی، دا خبرتیا درځي:");
        TextView ayah = k.arabic("فَاذْكُرُونِي أَذْكُرْكُمْ", 22, GREEN);
        ayah.setPadding(k.dp(12), k.dp(10), k.dp(12), k.dp(12));
        ayah.setBackground(k.rounded(SOFT, 16, LINE));
        c.addView(ayah, k.topMargin(6));

        c.addView(k.label("څو ساعته وروسته"));
        String[] labels = new String[Prefs.REMINDER_HOURS.length];
        for (int i = 0; i < labels.length; i++) {
            int h = Prefs.REMINDER_HOURS[i];
            labels[i] = h == 0 ? "بنده" : h == 1 ? "۱ ساعت" : Ui.fa(h) + " ساعته";
        }
        reminderSpinner = k.spinner(labels, false);
        c.addView(reminderSpinner, k.matchWrap());
        reminderSpinner.setOnItemSelectedListener(onSelect(pos -> {
            int h = Prefs.REMINDER_HOURS[pos];
            if (h == Prefs.reminderHours(this)) return;
            Prefs.setReminderHours(this, h);
            Prefs.setLastReminder(this, 0);
        }));

        TextView note = k.text("د شپې له ۹ بجو تر سهار ۶ بجو یادونه نه راځي.", 12, MUTED);
        note.setPadding(0, k.dp(10), 0, 0);
        c.addView(note);
        return c;
    }

    private View listCard() {
        LinearLayout c = k.card("≡", "ذکرونه", "هر ذکر په یوه کرښه کې ولیکئ؛ زیات، کم یا بدل یې کړئ");
        listInput = new EditText(this);
        listInput.setText(Prefs.rawList(this));
        listInput.setTypeface(k.amiri);
        listInput.setTextSize(18);
        listInput.setTextColor(INK);
        listInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        listInput.setSingleLine(false);
        listInput.setMinLines(6);
        listInput.setMaxLines(14);
        listInput.setGravity(Gravity.TOP | Gravity.START);
        listInput.setTextDirection(View.TEXT_DIRECTION_RTL);
        listInput.setPadding(k.dp(14), k.dp(10), k.dp(14), k.dp(10));
        listInput.setBackground(k.rounded(SOFT, 16, LINE));
        c.addView(listInput, k.topMargin(10));

        c.addView(k.button("ساتل", true, v -> {
            Prefs.saveList(this, listInput.getText().toString());
            Toast.makeText(this, "ذکرونه وساتل شول", Toast.LENGTH_SHORT).show();
            refresh();
            refreshService();
        }), k.topMargin(12));
        return c;
    }

    private View moreCard() {
        LinearLayout c = k.card("✦", "نور", "زموږ په اړه، او زموږ ملاتړ");
        c.addView(navRow(k.badge("♥", 46), "زموږ ملاتړ (ډونېټ)", "HesabPay · Atoma Pay · Payoneer", InfoActivity.PAGE_DONATE));
        c.addView(navRow(k.badge("ﷲ", 46), "د پروګرام په اړه", "موخه، ځانګړتیاوې او محرمیت", InfoActivity.PAGE_ABOUT));
        ImageView ph = k.photo(46);
        c.addView(navRow(ph, "پراختیاکوونکی", "زاهدالله الکوزی · غوره ګرافیکس پروډکشن", InfoActivity.PAGE_DEV));

        c.addView(k.button("ملګرو ته یې ولېږئ", true, v -> InfoActivity.share(this)), k.topMargin(12));
        return c;
    }

    private View navRow(View icon, String title, String sub, int page) {
        LinearLayout r = k.row();
        r.setPadding(k.dp(4), k.dp(10), k.dp(4), k.dp(10));
        r.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), null, k.rounded(WHITE, 14, 0)));
        r.setClickable(true);
        r.addView(icon);

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(k.dp(12), 0, k.dp(8), 0);
        tx.addView(k.title(title, 16, INK));
        tx.addView(k.text(sub, 12, MUTED));
        r.addView(tx, k.weight());

        r.addView(k.text("‹", 24, MUTED));
        r.setOnClickListener(v -> startActivity(new Intent(this, InfoActivity.class).putExtra(InfoActivity.EXTRA_PAGE, page)));
        return r;
    }

    private View switchRow(String label, Switch sw) {
        LinearLayout row = k.row();
        row.setPadding(k.dp(14), k.dp(8), k.dp(10), k.dp(8));
        row.setBackground(k.rounded(SOFT, 16, LINE));
        row.addView(k.title(label, 16, INK), k.weight());
        int[][] states = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{GREEN, 0xFFF1F1F1}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{0x801B5E20, 0x40000000}));
        row.addView(sw);
        LinearLayout.LayoutParams lp = k.topMargin(8);
        row.setLayoutParams(lp);
        return row;
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
        boolean running = Prefs.running(this);
        barSwitch.setChecked(running);
        tasbihSwitch.setChecked(Prefs.overlay(this) && Settings.canDrawOverlays(this) && running);
        currentZk.setText(Prefs.current(this));
        countText.setText(Ui.fa(Prefs.count(this)));

        statBar.setText(running ? "روښانه" : "بند");
        statBar.setTextColor(running ? GREEN : MUTED);
        statCount.setText(Ui.fa(Prefs.count(this)));
        int t = Prefs.target(this);
        statGoal.setText(t == 0 ? "∞" : Ui.fa(t));

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
            tiles.get(i).setBackground(i == sel ? k.rounded(0x141B5E20, 22, GREEN) : null);
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
