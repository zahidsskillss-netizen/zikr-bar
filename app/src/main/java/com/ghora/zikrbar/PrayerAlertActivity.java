package com.ghora.zikrbar;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.ghora.zikrbar.Kit.*;
import static com.ghora.zikrbar.TasbihActivity.*;

/** Alert settings for one prayer: kind of alert (or the user's own adhan sound) and minutes before/after. */
public class PrayerAlertActivity extends Activity {

    static final String EXTRA_PRAYER = "prayer";
    static final String[] MODE_KEYS = {"pa_none", "pa_vibrate", "pa_silent", "pa_default", "pa_beep", "pa_custom"};
    static final String[] MODE_ICONS = {"🚫", "📳", "🔕", "🔔", "📢", "🎵"};
    static final int[] OFFSETS = {-30, -20, -15, -10, -6, -5, -3, 0, 3, 5, 10, 15};
    private static final int PICK_SOUND = 41;

    private Kit k;
    private int prayer;
    private ScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.darkWindow(this);
        prayer = getIntent().getIntExtra(EXTRA_PRAYER, PrayTimes.FAJR);
        scroll = new ScrollView(this);
        setContentView(scroll);
        build();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(28));
        scroll.removeAllViews();
        scroll.addView(root);

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
        TextView t = k.title(L.f("pa_title", L.t(PrayTimes.KEYS[prayer])), 19, D_TEXT);
        t.setPaddingRelative(k.dp(14), 0, 0, 0);
        h.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(h);

        // kinds of alert
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(k.glass(22));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(14), k.dp(16), k.dp(14), 0);
        root.addView(list, lp);
        int mode = PrayTimes.alertMode(this, prayer);
        for (int m = 0; m < MODE_KEYS.length; m++) {
            final int mm = m;
            LinearLayout r = k.row();
            r.setPadding(k.dp(16), k.dp(12), k.dp(16), k.dp(12));
            r.setBackground(new RippleDrawable(ColorStateList.valueOf(Theme.ov(0x30)), null, k.rounded(Theme.ov(0xFF), 22, 0)));
            TextView ic = new TextView(this);
            ic.setText(MODE_ICONS[m]);
            ic.setTextSize(20);
            ic.setGravity(Gravity.CENTER);
            ic.setBackground(k.rounded(0x00000000, 24, D_ACCENT));
            r.addView(ic, new LinearLayout.LayoutParams(k.dp(46), k.dp(46)));
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPaddingRelative(k.dp(18), 0, 0, 0);
            tx.addView(k.strong(L.t(MODE_KEYS[m]), 16, D_TEXT));
            if (m == PrayTimes.A_CUSTOM) {
                String name = PrayTimes.customName(this, prayer);
                tx.addView(k.text(name.isEmpty() ? L.t("pa_custom_s") : name, 12, D_MUTED));
            }
            r.addView(tx, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            if (m == mode) r.addView(k.title("✓", 22, 0xFF4F9DFF));
            r.setOnClickListener(v -> choose(mm));
            list.addView(r);
        }

        // notify at
        LinearLayout at = k.row();
        at.setPadding(k.dp(18), k.dp(16), k.dp(18), k.dp(16));
        at.setBackground(new RippleDrawable(ColorStateList.valueOf(Theme.ov(0x30)), k.glass(18), null));
        TextView atL = k.strong(L.t("pa_at"), 16, D_TEXT);
        at.addView(atL, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        at.addView(k.strong(offsetText(PrayTimes.alertOffset(this, prayer)), 15, D_ACCENT));
        at.setOnClickListener(v -> pickOffset());
        LinearLayout.LayoutParams ap = k.matchWrap();
        ap.setMargins(k.dp(14), k.dp(14), k.dp(14), 0);
        root.addView(at, ap);

        long next = PrayerAlarm.nextFor(this, prayer);
        if (PrayTimes.alertOn(this, prayer) && next > 0) {
            TextView nx = k.text(L.f("pa_next", whenText(next)), 14, D_MUTED);
            nx.setPadding(k.dp(20), k.dp(12), k.dp(20), 0);
            root.addView(nx);
        }

        if (mode != PrayTimes.A_NONE) {
            TextView test = k.button(L.t("pa_test"), true, v -> test(mode));
            LinearLayout.LayoutParams tp = k.matchWrap();
            tp.setMargins(k.dp(40), k.dp(18), k.dp(40), 0);
            root.addView(test, tp);
        }

        TextView note = k.text(L.t("pa_note"), 12, D_MUTED);
        note.setGravity(Gravity.CENTER);
        note.setPadding(k.dp(22), k.dp(16), k.dp(22), 0);
        root.addView(note);
    }

    private void choose(int m) {
        if (m == PrayTimes.A_CUSTOM) {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*");
            try {
                startActivityForResult(i, PICK_SOUND);
            } catch (RuntimeException e) {
                Toast.makeText(this, L.t("link_fail"), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        PrayTimes.setAlertMode(this, prayer, m);
        PrayerAlarm.schedule(this);
        build();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK_SOUND || res != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        String name = displayName(u);
        File dir = new File(getFilesDir(), "adhan");
        if (!dir.exists() && !dir.mkdirs()) return;
        File f = new File(dir, "prayer_" + prayer);
        try (InputStream in = getContentResolver().openInputStream(u); OutputStream out = new FileOutputStream(f)) {
            if (in == null) throw new java.io.IOException("no stream");
            byte[] b = new byte[16384];
            int n;
            long total = 0;
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
                total += n;
                if (total > 30L * 1024 * 1024) throw new java.io.IOException("too big");
            }
        } catch (Exception e) {
            Toast.makeText(this, L.t("pa_file_fail"), Toast.LENGTH_LONG).show();
            return;
        }
        PrayTimes.setCustom(this, prayer, f.getAbsolutePath(), name);
        PrayTimes.setAlertMode(this, prayer, PrayTimes.A_CUSTOM);
        PrayerAlarm.schedule(this);
        build();
    }

    private String displayName(Uri u) {
        try (Cursor c = getContentResolver().query(u, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (RuntimeException ignored) {
        }
        return "audio";
    }

    private void pickOffset() {
        String[] items = new String[OFFSETS.length];
        int sel = 0;
        for (int i = 0; i < OFFSETS.length; i++) {
            items[i] = offsetText(OFFSETS[i]);
            if (OFFSETS[i] == PrayTimes.alertOffset(this, prayer)) sel = i;
        }
        new android.app.AlertDialog.Builder(this).setTitle(L.t("pa_at")).setSingleChoiceItems(items, sel, (d, i) -> {
            PrayTimes.setAlertOffset(this, prayer, OFFSETS[i]);
            PrayerAlarm.schedule(this);
            d.dismiss();
            build();
        }).show();
    }

    private String offsetText(int m) {
        String name = L.t(PrayTimes.KEYS[prayer]);
        if (m == 0) return L.f("pa_on_time", name);
        return L.f(m < 0 ? "pa_before" : "pa_after", L.num(Math.abs(m)), name);
    }

    private String whenText(long t) {
        Calendar now = Calendar.getInstance(), c = Calendar.getInstance();
        c.setTimeInMillis(t);
        String day = c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) ? L.t("today") : L.t("tomorrow");
        int min = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        return day + " " + PrayTimes.clock(min, true);
    }

    private void test(int mode) {
        if (mode == PrayTimes.A_BEEP || mode == PrayTimes.A_CUSTOM) {
            Intent s = new Intent(this, AdhanService.class).putExtra("p", prayer).putExtra("mode", mode).putExtra("test", true);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(s);
            else startService(s);
        } else {
            PrayerAlarm.show(this, prayer, mode);
        }
    }

    static String icon(int mode) {
        return MODE_ICONS[Math.max(0, Math.min(MODE_ICONS.length - 1, mode))];
    }

    @SuppressWarnings("unused")
    private static String fmt(long t) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(t);
    }
}
