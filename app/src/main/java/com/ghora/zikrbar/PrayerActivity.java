package com.ghora.zikrbar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.ghora.zikrbar.Kit.*;
import static com.ghora.zikrbar.TasbihActivity.*;

/** Prayer times for the chosen city or the phone's location, the Hijri date, and a message for each prayer. */
public class PrayerActivity extends Activity {

    private Kit k;
    private int dayOffset;
    private ScrollView scroll;
    private final Handler tick = new Handler(Looper.getMainLooper());
    private TextView countdown;
    private int nextPrayer = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.darkWindow(this);
        scroll = new ScrollView(this);
        setContentView(scroll);
        build();
        PrayerAlarm.schedule(this);
    }

    private boolean shown;

    @Override
    protected void onResume() {
        super.onResume();
        if (shown) build(); // alert settings may have changed
        shown = true;
        tick.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tick.removeCallbacks(ticker);
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            tick.postDelayed(this, 1000);
        }
    };

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(28));
        scroll.removeAllViews();
        scroll.addView(root);

        // header
        LinearLayout h = k.row();
        h.setBackgroundColor(D_HEAD);
        h.setPadding(k.dp(8), k.dp(10), k.dp(8), k.dp(10));
        h.addView(icon(L.rtl() ? "→" : "←", v -> finish()));
        TextView t = k.title(L.t("prayer_t"), 21, D_TEXT);
        t.setPaddingRelative(k.dp(14), 0, 0, 0);
        h.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        h.addView(icon("⚙︎", v -> settings()));
        root.addView(h);

        // place
        LinearLayout place = new LinearLayout(this);
        place.setOrientation(LinearLayout.VERTICAL);
        place.setGravity(Gravity.CENTER);
        place.setPadding(k.dp(16), k.dp(16), k.dp(16), k.dp(14));
        place.setBackground(new RippleDrawable(ColorStateList.valueOf(Theme.ov(0x30)), null, null));
        TextView name = k.title("📍 " + PrayTimes.placeName(this), 20, D_TEXT);
        name.setGravity(Gravity.CENTER);
        place.addView(name);
        TextView method = k.text(L.t(PrayTimes.METHOD_KEYS[PrayTimes.method(this)]) + " · "
                + L.t(PrayTimes.hanafi(this) ? "asr_hanafi" : "asr_shafii"), 13, D_MUTED);
        method.setGravity(Gravity.CENTER);
        place.addView(method);
        TextView change = k.strong(L.t("change_city"), 12.5f, D_ACCENT);
        change.setGravity(Gravity.CENTER);
        change.setPadding(0, k.dp(4), 0, 0);
        place.addView(change);
        place.setOnClickListener(v -> pickCity());
        root.addView(place);

        // date row
        LinearLayout dr = k.row();
        dr.setPadding(k.dp(8), 0, k.dp(8), 0);
        dr.addView(icon(L.rtl() ? "›" : "‹", v -> { dayOffset--; build(); }));
        Calendar day = Calendar.getInstance();
        day.add(Calendar.DAY_OF_YEAR, dayOffset);
        LinearLayout dates = new LinearLayout(this);
        dates.setOrientation(LinearLayout.VERTICAL);
        dates.setGravity(Gravity.CENTER);
        TextView greg = k.strong(new SimpleDateFormat("EEEE، d MMMM yyyy", locale()).format(day.getTime()), 14, D_TEXT);
        greg.setGravity(Gravity.CENTER);
        dates.addView(greg);
        TextView hij = k.arabic(hijriFor(dayOffset), 17, D_GOLD);
        dates.addView(hij);
        dr.addView(dates, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        dr.addView(icon(L.rtl() ? "‹" : "›", v -> { dayOffset++; build(); }));
        root.addView(dr);
        if (dayOffset != 0) {
            TextView today = k.strong(L.t("today"), 13, D_ACCENT);
            today.setGravity(Gravity.CENTER);
            today.setPadding(0, k.dp(6), 0, 0);
            today.setOnClickListener(v -> { dayOffset = 0; build(); });
            root.addView(today);
        }

        // times
        int[] m = PrayTimes.minutes(this, day);
        nextPrayer = dayOffset == 0 ? next(m) : -1;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(k.glass(24));
        LinearLayout.LayoutParams cp = k.matchWrap();
        cp.setMargins(k.dp(14), k.dp(16), k.dp(14), 0);
        root.addView(card, cp);
        for (int p = 0; p < 6; p++) {
            final int pr = p;
            LinearLayout r = k.row();
            r.setPadding(k.dp(18), k.dp(14), k.dp(12), k.dp(14));
            boolean isNext = p == nextPrayer;
            if (isNext) r.setBackground(k.rounded(Theme.ov(0x26), 18, D_ACCENT));
            TextView pn = k.title(L.t(PrayTimes.KEYS[p]), 18, isNext ? D_GOLD : D_TEXT);
            r.addView(pn);
            TextView mid = k.text("", 12, D_ACCENT);
            mid.setGravity(Gravity.CENTER);
            if (isNext) countdown = mid;
            r.addView(mid, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            TextView time = k.title(PrayTimes.clock(m[p], true), 18, D_TEXT);
            time.setTextDirection(View.TEXT_DIRECTION_LTR);
            r.addView(time);
            TextView bell = icon(PrayerAlertActivity.icon(PrayTimes.alertMode(this, p)), null);
            bell.setTextSize(18);
            bell.setBackground(k.rounded(0x00000000, 22, D_ACCENT));
            LinearLayout.LayoutParams bpl = new LinearLayout.LayoutParams(k.dp(40), k.dp(40));
            bpl.setMarginStart(k.dp(10));
            bell.setLayoutParams(bpl);
            bell.setOnClickListener(v -> startActivity(new Intent(this, PrayerAlertActivity.class)
                    .putExtra(PrayerAlertActivity.EXTRA_PRAYER, pr)));
            r.addView(bell);
            card.addView(r);
            if (p < 5) {
                View line = new View(this);
                line.setBackgroundColor(Theme.ov(0x14));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, k.dp(1));
                lp.setMargins(k.dp(16), 0, k.dp(16), 0);
                card.addView(line, lp);
            }
        }

        TextView words = k.button("\uD83D\uDCD6\u2003" + L.t("sw_title"), false, v -> startActivity(new Intent(this, SalahWordsActivity.class)));
        LinearLayout.LayoutParams wp = k.matchWrap();
        wp.setMargins(k.dp(24), k.dp(20), k.dp(24), 0);
        root.addView(words, wp);

        TextView fix = k.button(L.t("fix_delay"), true, v -> fixDelay());
        LinearLayout.LayoutParams fp = k.matchWrap();
        fp.setMargins(k.dp(40), k.dp(20), k.dp(40), 0);
        root.addView(fix, fp);
        TextView note = k.text(L.t("prayer_note"), 12, D_MUTED);
        note.setGravity(Gravity.CENTER);
        note.setPadding(k.dp(22), k.dp(12), k.dp(22), 0);
        root.addView(note);
        updateCountdown();
    }

    private TextView icon(String glyph, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(glyph);
        b.setTextSize(22);
        b.setTextColor(D_TEXT);
        b.setGravity(Gravity.CENTER);
        if (l != null) b.setOnClickListener(l);
        b.setLayoutParams(new LinearLayout.LayoutParams(k.dp(44), k.dp(44)));
        return b;
    }

    private Locale locale() {
        switch (L.lang) {
            case L.EN: return Locale.ENGLISH;
            case L.AR: return new Locale("ar");
            case L.UR: return new Locale("ur");
            case L.FA: return new Locale("fa");
            default: return new Locale("ps");
        }
    }

    private String hijriFor(int offset) {
        return PrayTimes.hijriText(this, offset);
    }

    /** Index of the next prayer today, or Fajr when Isha has passed. */
    private int next(int[] m) {
        Calendar now = Calendar.getInstance(PrayTimes.zone(this));
        int cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        for (int p = 0; p < 6; p++) if (m[p] > cur) return p;
        return PrayTimes.FAJR;
    }

    private void updateCountdown() {
        if (countdown == null || nextPrayer < 0) return;
        long t = PrayTimes.when(this, 0, nextPrayer);
        if (t < System.currentTimeMillis()) t = PrayTimes.when(this, 1, nextPrayer);
        long s = Math.max(0, (t - System.currentTimeMillis()) / 1000);
        if (s == 0) {
            build();
            return;
        }
        String left = L.num((int) (s / 3600)) + ":" + two((int) (s / 60 % 60)) + ":" + two((int) (s % 60));
        countdown.setText(L.f("in_time", left));
    }

    private static String two(int n) {
        return (n < 10 ? L.num(0) : "") + L.num(n);
    }

    // ---------------------------------------------------------------- place
    private void pickCity() {
        String[] names = new String[Cities.ALL.length + 1];
        names[0] = "📍 " + L.t("my_location");
        for (int i = 0; i < Cities.ALL.length; i++) names[i + 1] = Cities.ALL[i].name();
        new AlertDialog.Builder(this)
                .setTitle(L.t("change_city"))
                .setItems(names, (d, w) -> {
                    if (w == 0) useLocation();
                    else {
                        PrayTimes.setCity(this, w - 1);
                        changed();
                    }
                })
                .show();
    }

    private void changed() {
        PrayerAlarm.schedule(this);
        build();
    }

    private void useLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 7);
            return;
        }
        LocationManager lm = getSystemService(LocationManager.class);
        if (lm == null) return;
        Location best = null;
        try {
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
        } catch (SecurityException ignored) {
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 6 * 3_600_000L) {
            gotLocation(best);
            return;
        }
        Toast.makeText(this, L.t("finding_loc"), Toast.LENGTH_SHORT).show();
        try {
            String provider = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER
                    : LocationManager.GPS_PROVIDER;
            final Location fallback = best;
            requestOnce(lm, provider);
            tick.postDelayed(() -> {
                if (waiting && fallback != null) gotLocation(fallback);
            }, 15000);
        } catch (RuntimeException e) {
            if (best != null) gotLocation(best);
            else Toast.makeText(this, L.t("loc_fail"), Toast.LENGTH_LONG).show();
        }
    }

    private boolean waiting;

    @SuppressWarnings({"deprecation", "MissingPermission"})
    private void requestOnce(LocationManager lm, String provider) {
        waiting = true;
        lm.requestSingleUpdate(provider, new LocationListener() {
            @Override
            public void onLocationChanged(Location l) {
                if (waiting) gotLocation(l);
            }

            @Override
            public void onStatusChanged(String p, int s, Bundle b) {}

            @Override
            public void onProviderEnabled(String p) {}

            @Override
            public void onProviderDisabled(String p) {}
        }, Looper.getMainLooper());
    }

    private void gotLocation(Location l) {
        waiting = false;
        PrayTimes.setLocation(this, l.getLatitude(), l.getLongitude());
        changed();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 7) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) useLocation();
            else Toast.makeText(this, L.t("loc_fail"), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- settings
    private void settings() {
        String[] rows = {
                L.t("calc_method") + ": " + L.t(PrayTimes.METHOD_KEYS[PrayTimes.method(this)]),
                L.t("asr_method") + ": " + L.t(PrayTimes.hanafi(this) ? "asr_hanafi" : "asr_shafii"),
                L.t("hijri_adj") + ": " + signed(PrayTimes.hijriOffset(this)),
        };
        new AlertDialog.Builder(this).setTitle(L.t("settings_t")).setItems(rows, (d, w) -> {
            if (w == 0) {
                String[] ms = new String[PrayTimes.METHOD_KEYS.length];
                for (int i = 0; i < ms.length; i++) ms[i] = L.t(PrayTimes.METHOD_KEYS[i]);
                choose(L.t("calc_method"), ms, PrayTimes.method(this), i -> PrayTimes.sp(this).edit().putInt("method", i).apply());
            } else if (w == 1) {
                choose(L.t("asr_method"), new String[]{L.t("asr_hanafi"), L.t("asr_shafii")}, PrayTimes.hanafi(this) ? 0 : 1,
                        i -> PrayTimes.sp(this).edit().putBoolean("hanafi", i == 0).apply());
            } else {
                String[] off = {signed(-2), signed(-1), signed(0), signed(1), signed(2)};
                choose(L.t("hijri_adj"), off, PrayTimes.hijriOffset(this) + 2,
                        i -> PrayTimes.sp(this).edit().putInt("hijri", i - 2).apply());
            }
        }).show();
    }

    private interface Pick {
        void on(int i);
    }

    private void choose(String title, String[] items, int sel, Pick p) {
        new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(items, sel, (d, i) -> {
            p.on(i);
            d.dismiss();
            changed();
        }).show();
    }

    private static String signed(int n) {
        return (n > 0 ? "+" : n < 0 ? "−" : "") + L.num(Math.abs(n));
    }

    /** Opens the phone settings that let the prayer messages arrive on time. */
    private void fixDelay() {
        try {
            if (Build.VERSION.SDK_INT >= 31 && !getSystemService(android.app.AlarmManager.class).canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
            } else {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
            Toast.makeText(this, L.t("fix_delay_s"), Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        }
    }
}
