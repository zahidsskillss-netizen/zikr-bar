package com.ghora.zikrbar;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * The daily adhkar reminder as a small round logo (like a chat head) at the top of the screen.
 * It stays until it is tapped (opens the adhkar) or dragged onto the ✕ at the bottom.
 */
public class BubbleService extends Service {

    static final String EXTRA_CAT = "cat";
    private static final String CHANNEL = "adhkar_bubble";
    private static final int NOTIF = 450;

    private WindowManager wm;
    private FrameLayout bubble;
    private WindowManager.LayoutParams lp;
    private OverlayParts.CloseTarget close;
    private ObjectAnimator[] rings;
    private int cat = AdhkarData.MORNING;

    /** Shows the logo for this category; false when it cannot be shown (then a normal message is used). */
    static boolean show(Context c, int cat) {
        if (!Settings.canDrawOverlays(c)) return false;
        Intent i = new Intent(c, BubbleService.class).putExtra(EXTRA_CAT, cat);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Where tapping the logo (or its message) goes. */
    static Intent target(Context c, int cat) {
        Intent i = cat == AdhkarData.SLEEP
                ? new Intent(c, SleepActivity.class)
                : new Intent(c, AdhkarActivity.class).putExtra(AdhkarActivity.EXTRA_TAB, cat);
        return i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    static String title(int cat) {
        String g = cat == AdhkarData.MORNING ? "☀︎  " : cat == AdhkarData.EVENING ? "☾︎  " : "★︎  ";
        return g + L.t(AdhkarData.nameKey(cat));
    }

    static String body(int cat) {
        return L.t(cat == AdhkarData.MORNING ? "n_morning_b" : cat == AdhkarData.EVENING ? "n_evening_b" : "n_sleep_b");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        L.init(this);
        wm = getSystemService(WindowManager.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.init(this);
        cat = intent != null ? intent.getIntExtra(EXTRA_CAT, AdhkarData.MORNING) : AdhkarData.MORNING;
        Notification n = notification();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF, n);
            }
        } catch (RuntimeException e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        removeBubble();
        if (!Settings.canDrawOverlays(this) || !addBubble()) {
            // keep a normal message instead of the logo
            getSystemService(NotificationManager.class).notify(NOTIF + 1, n);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        removeBubble();
        hideClose();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification notification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) channel(nm);
        PendingIntent pi = PendingIntent.getActivity(this, 460 + cat, target(this, cat),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return Ui.notif(this, CHANNEL, false)
                .setSmallIcon(Icon.createWithBitmap(Ui.allahIcon(this)))
                .setContentTitle(title(cat))
                .setContentText(body(cat))
                .setStyle(new Notification.BigTextStyle().bigText(body(cat)))
                .setColor(0xFF1B5E20)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
    }

    @android.annotation.TargetApi(26)
    private void channel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("rem_daily_t"), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setShowBadge(true);
        nm.createNotificationChannel(ch);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private Point screen() {
        Point pt = new Point();
        if (Build.VERSION.SDK_INT >= 30) {
            Rect b = wm.getCurrentWindowMetrics().getBounds();
            pt.set(b.width(), b.height());
        } else {
            wm.getDefaultDisplay().getRealSize(pt);
        }
        return pt;
    }

    // ------------------------------------------------------------------ the logo

    @SuppressLint("ClickableViewAccessibility")
    private boolean addBubble() {
        Kit k = new Kit(this);
        int box = dp(96), size = dp(64);
        bubble = new FrameLayout(this);
        bubble.setClipChildren(false);

        int accent = cat == AdhkarData.MORNING ? 0xFF5EEAD4 : cat == AdhkarData.EVENING ? 0xFFF5B83D : 0xFFA78BFA;
        rings = new ObjectAnimator[6];
        for (int i = 0; i < 2; i++) {
            View ring = new View(this);
            GradientDrawable rg = new GradientDrawable();
            rg.setShape(GradientDrawable.OVAL);
            rg.setStroke(dp(2), accent);
            ring.setBackground(rg);
            bubble.addView(ring, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
            ObjectAnimator sx = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.45f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.45f);
            ObjectAnimator al = ObjectAnimator.ofFloat(ring, "alpha", 0.8f, 0f);
            ObjectAnimator[] trio = {sx, sy, al};
            for (int j = 0; j < 3; j++) {
                trio[j].setDuration(2200);
                trio[j].setStartDelay(i * 1100L);
                trio[j].setRepeatCount(ValueAnimator.INFINITE);
                rings[i * 3 + j] = trio[j];
            }
        }

        TextView face = k.arabic("ﷲ", 26, Color.WHITE);
        face.setIncludeFontPadding(false);
        face.setShadowLayer(dp(4), 0, dp(1), 0x66000000);
        GradientDrawable fg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xF22A3D66, 0xF20B1530});
        fg.setShape(GradientDrawable.OVAL);
        fg.setStroke(dp(1.6f), 0xCCFFFFFF);
        face.setBackground(fg);
        face.setElevation(dp(8));
        bubble.addView(face, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));

        TextView mark = k.strong(cat == AdhkarData.MORNING ? "☀︎" : cat == AdhkarData.EVENING ? "☾︎" : "★︎", 11, 0xFF2B1C02);
        mark.setGravity(Gravity.CENTER);
        mark.setIncludeFontPadding(false);
        GradientDrawable mg = new GradientDrawable();
        mg.setShape(GradientDrawable.OVAL);
        mg.setColor(accent);
        mg.setStroke(dp(2), 0xFF0B1530);
        mark.setBackground(mg);
        mark.setElevation(dp(9));
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER);
        mp.leftMargin = -dp(24);
        mp.topMargin = dp(24);
        bubble.addView(mark, mp);

        lp = new WindowManager.LayoutParams(box, box, Ui.overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        Point s = screen();
        lp.x = s.x - box - dp(2);
        lp.y = dp(70);

        int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        final float[] down = new float[4];
        final boolean[] moved = new boolean[1];
        bubble.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down[0] = e.getRawX();
                    down[1] = e.getRawY();
                    down[2] = lp.x;
                    down[3] = lp.y;
                    moved[0] = false;
                    face.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - down[0], dy = e.getRawY() - down[1];
                    if (!moved[0] && Math.hypot(dx, dy) > slop) {
                        moved[0] = true;
                        showClose();
                    }
                    if (moved[0]) {
                        lp.x = Math.round(down[2] + dx);
                        lp.y = Math.round(down[3] + dy);
                        try {
                            wm.updateViewLayout(bubble, lp);
                        } catch (RuntimeException ignored) {
                        }
                        if (close != null) close.setActive(inClose(e));
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    face.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    if (!moved[0]) {
                        if (e.getActionMasked() == MotionEvent.ACTION_UP) open();
                        return true;
                    }
                    boolean drop = inClose(e);
                    hideClose();
                    if (drop) dismiss(); else snap();
                    return true;
            }
            return false;
        });

        try {
            wm.addView(bubble, lp);
        } catch (RuntimeException e) {
            bubble = null;
            return false;
        }
        for (ObjectAnimator a : rings) a.start();
        face.setScaleX(0.2f);
        face.setScaleY(0.2f);
        face.setAlpha(0f);
        face.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(600).setInterpolator(new OvershootInterpolator(2f)).start();
        mark.setScaleX(0f);
        mark.setScaleY(0f);
        mark.animate().scaleX(1f).scaleY(1f).setStartDelay(450).setDuration(300).setInterpolator(new OvershootInterpolator(2f)).start();
        return true;
    }

    private boolean inClose(MotionEvent e) {
        Point s = screen();
        return Math.hypot(e.getRawX() - s.x / 2f, e.getRawY() - (s.y - dp(110))) < dp(80);
    }

    /** After a drag the logo slides to the nearest side. */
    private void snap() {
        if (bubble == null) return;
        Point s = screen();
        int box = lp.width;
        int to = lp.x + box / 2 < s.x / 2 ? -dp(2) : s.x - box + dp(2);
        lp.y = Math.max(dp(24), Math.min(s.y - box - dp(80), lp.y));
        ValueAnimator a = ValueAnimator.ofInt(lp.x, to);
        a.setDuration(260);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(an -> {
            if (bubble == null) return;
            lp.x = (int) an.getAnimatedValue();
            try {
                wm.updateViewLayout(bubble, lp);
            } catch (RuntimeException ignored) {
            }
        });
        a.start();
    }

    private void open() {
        try {
            startActivity(target(this, cat));
        } catch (RuntimeException ignored) {
        }
        dismiss();
    }

    private void dismiss() {
        removeBubble();
        hideClose();
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        stopSelf();
    }

    private void removeBubble() {
        if (rings != null) {
            for (ObjectAnimator a : rings) if (a != null) a.cancel();
            rings = null;
        }
        if (bubble != null) {
            try {
                wm.removeView(bubble);
            } catch (RuntimeException ignored) {
            }
            bubble = null;
        }
    }

    private void showClose() {
        if (close != null) return;
        close = new OverlayParts.CloseTarget(this);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, Ui.overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        p.y = dp(62);
        try {
            wm.addView(close, p);
            close.setAlpha(0f);
            close.animate().alpha(1f).setDuration(150).start();
        } catch (RuntimeException e) {
            close = null;
        }
    }

    private void hideClose() {
        if (close == null) return;
        try {
            wm.removeView(close);
        } catch (RuntimeException ignored) {
        }
        close = null;
    }
}
