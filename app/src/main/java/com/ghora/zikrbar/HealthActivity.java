package com.ghora.zikrbar;

import static com.ghora.zikrbar.Kit.*;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** "Keep it working": what the phone must allow so popups and the unlock dhikr keep coming, with one-tap fixes. */
public class HealthActivity extends Activity {

    private Kit k;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        L.init(this);
        k = new Kit(this);
        Theme.window(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, k.dp(8), 0, k.dp(32));
        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private boolean batteryOk() {
        if (Build.VERSION.SDK_INT < 23) return true;
        PowerManager pm = getSystemService(PowerManager.class);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean alarmsOk() {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    private boolean notifOk() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void build() {
        root.removeAllViews();
        LinearLayout c = k.card("⚙", L.t("hl_t"), L.t("hl_s"));
        row(c, Settings.canDrawOverlays(this), L.t("hl_overlay"), () ->
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        row(c, batteryOk(), L.t("hl_battery"), this::askBattery);
        row(c, alarmsOk(), L.t("hl_alarm"), () -> {
            if (Build.VERSION.SDK_INT >= 31) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
            }
        });
        row(c, notifOk(), L.t("hl_notif"), this::appSettings);
        row(c, Prefs.running(this), L.t("hl_running"), () -> {
            Prefs.setRunning(this, true);
            Ui.startZikrService(this, new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_REFRESH));
            PopupAlarm.schedule(this);
            build();
        });
        row(c, ZikrService.live != null, L.t("hl_alive"), () -> {
            if (Prefs.running(this)) {
                Ui.startZikrService(this, new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_REFRESH));
            }
            root.postDelayed(this::build, 800);
        });
        root.addView(c);

        LinearLayout h = k.card("★", L.t("hl_phone_t"), L.t("hl_phone_s"));
        TextView steps = k.text(L.t("hl_steps"), 14, INK);
        steps.setLineSpacing(0, 1.35f);
        h.addView(steps, k.topMargin(6));
        h.addView(k.button(L.t("hl_appinfo"), true, v -> appSettings()), k.topMargin(12));
        root.addView(h);

        LinearLayout d = k.card("☰", L.t("hl_diag_t"), L.t("hl_diag_s"));
        d.addView(k.text(L.t("hl_d_alarm") + ": " + ago("dgAlarm") + "\n"
                + L.t("hl_d_popup") + ": " + ago("dgShown") + "\n"
                + L.t("hl_d_unlock") + ": " + ago("dgUnlock") + "\n"
                + L.t("hl_d_restart") + ": " + ago("dgRestart") + "\n"
                + L.t("hl_d_err") + ": " + Prefs.sp(this).getString("dgErr", "-"), 13.5f, MUTED), k.topMargin(4));
        root.addView(d);
    }

    private String ago(String key) {
        long t = Prefs.sp(this).getLong(key, 0);
        if (t == 0) return "-";
        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(t));
    }

    private void row(LinearLayout parent, boolean ok, String label, Runnable fix) {
        LinearLayout r = k.row();
        r.setPadding(k.dp(2), k.dp(8), k.dp(2), k.dp(8));
        TextView mark = k.strong(ok ? "✓" : "✗", 20, ok ? 0xFF2E9E5B : 0xFFD64545);
        mark.setPadding(0, 0, k.dp(10), 0);
        r.addView(mark);
        r.addView(k.text(label, 14.5f, INK), k.weight());
        if (!ok && fix != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            r.addView(k.button(L.t("hl_fix"), true, v -> fix.run()), lp);
        }
        parent.addView(r);
    }

    private void askBattery() {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
                return;
            }
        } catch (RuntimeException ignored) {
        }
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (RuntimeException e) {
            appSettings();
        }
    }

    private void appSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException ignored) {
        }
    }
}
