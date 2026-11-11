package com.ghora.zikrbar;

import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shows one short announcement card over other apps until the person uses its button or closes it. */
public class AnnounceService extends Service {

    static AnnounceService live;
    private static final int FG = 480;

    private WindowManager wm;
    private FrameLayout card;
    private int shownKind;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        L.init(this);
        wm = getSystemService(WindowManager.class);
        live = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.init(this);
        live = this;
        int kind = intent == null ? 0 : intent.getIntExtra(AnnounceAlarm.EXTRA_KIND, 0);
        AnnounceAlarm.ensureChannel(getSystemService(android.app.NotificationManager.class));
        Notification n = Ui.notif(this, "announce", true)
                .setSmallIcon(Icon.createWithBitmap(Ui.allahIcon(this)))
                .setContentTitle(AnnounceAlarm.title(kind))
                .setColor(0xFF1B5E20)
                .build();
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(FG, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(FG, n);
        } catch (RuntimeException e) {
            AnnounceAlarm.notifyOnly(this, kind);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (kind == 0 || !show(kind)) {
            if (kind != 0) AnnounceAlarm.notifyOnly(this, kind);
            finish();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        removeCard();
        if (live == this) live = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void dismissKind(int kind) {
        if (card != null && shownKind == kind) finish();
    }

    private void finish() {
        removeCard();
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        stopSelf();
    }

    private void removeCard() {
        handler.removeCallbacksAndMessages(null);
        if (card != null) {
            try {
                wm.removeView(card);
            } catch (RuntimeException ignored) {
            }
            card = null;
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private boolean show(int kind) {
        removeCard();
        Kit k = new Kit(this);
        boolean power = kind == AnnounceAlarm.POWER;
        FrameLayout wrap = new FrameLayout(this);
        wrap.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        wrap.setPadding(dp(12), dp(6), dp(12), dp(14));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF14502A, 0xFF0B3319, 0xFF06200F});
        bg.setCornerRadius(dp(26));
        bg.setStroke(dp(2), 0xFFF3C24A);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(18), dp(14), dp(18), dp(16));
        col.setBackground(bg);
        col.setElevation(dp(14));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = k.title(AnnounceAlarm.title(kind), 17, 0xFFF3C24A);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView x = k.strong("✕", 18, Color.WHITE);
        x.setGravity(Gravity.CENTER);
        x.setBackground(k.rounded(0x33FFFFFF, 20, 0));
        top.addView(x, new LinearLayout.LayoutParams(dp(38), dp(38)));
        col.addView(top);

        TextView msg = k.text(AnnounceAlarm.body(this, kind), 15.5f, Color.WHITE);
        msg.setLineSpacing(0, 1.25f);
        msg.setPadding(0, dp(8), 0, dp(12));
        col.addView(msg);

        View action;
        if (power) {
            TextView p = new TextView(this);
            p.setText("⏻");
            p.setTextColor(Color.WHITE);
            p.setTextSize(34);
            p.setGravity(Gravity.CENTER);
            GradientDrawable pb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF3EE08F, 0xFF0E8F55});
            pb.setShape(GradientDrawable.OVAL);
            pb.setStroke(dp(2), 0x99FFFFFF);
            p.setBackground(pb);
            p.setElevation(dp(6));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(78), dp(78));
            pp.gravity = Gravity.CENTER_HORIZONTAL;
            col.addView(p, pp);
            TextView tap = k.strong(L.t("an_power_tap"), 14, 0xFFC9F5DB);
            tap.setGravity(Gravity.CENTER);
            tap.setPadding(0, dp(6), 0, 0);
            col.addView(tap);
            action = p;
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(p,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f));
            pulse.setDuration(900);
            pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            pulse.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            pulse.start();
        } else {
            TextView b = k.title(L.t(kind == AnnounceAlarm.SALAH ? "an_salah_btn" : "an_asma_btn"), 16, 0xFF3A2500);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(10), dp(12), dp(10), dp(14));
            GradientDrawable bb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFFFFD166, 0xFFE09A12});
            bb.setCornerRadius(dp(18));
            b.setBackground(bb);
            col.addView(b, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            action = b;
        }
        wrap.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        x.setOnClickListener(v -> finish());
        action.setOnClickListener(v -> {
            if (power) {
                if (Settings_ok()) {
                    Prefs.setRunning(this, true);
                    try {
                        Ui.startZikrService(this, new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_REFRESH));
                    } catch (RuntimeException ignored) {
                    }
                    AnnounceAlarm.barStarted(this);
                    android.widget.Toast.makeText(this, L.t("an_power_done"), android.widget.Toast.LENGTH_LONG).show();
                } else {
                    open(kind);
                }
            } else {
                open(kind);
            }
            finish();
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, Ui.overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP;
        lp.y = dp(36);
        try {
            wm.addView(wrap, lp);
        } catch (RuntimeException e) {
            return false;
        }
        card = wrap;
        shownKind = kind;
        col.setAlpha(0f);
        col.setTranslationY(-dp(50));
        col.animate().alpha(1f).translationY(0f).setDuration(520).setInterpolator(new OvershootInterpolator(1.1f)).start();
        return true;
    }

    /** The bar can be started straight from here only when notifications are allowed; otherwise open the app. */
    private boolean Settings_ok() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void open(int kind) {
        try {
            startActivity(AnnounceAlarm.target(this, kind));
        } catch (RuntimeException ignored) {
        }
    }
}
