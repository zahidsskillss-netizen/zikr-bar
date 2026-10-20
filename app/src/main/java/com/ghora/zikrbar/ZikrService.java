package com.ghora.zikrbar;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Keeps the name of Allah Ta'ala in the status bar, rotates the dhikr in the notification,
 * and shows the floating tasbih over other apps when it is turned on.
 */
public class ZikrService extends Service {

    static final String ACTION_REFRESH = "com.ghora.zikrbar.REFRESH";
    static final String ACTION_TOGGLE_TASBIH = "com.ghora.zikrbar.TOGGLE_TASBIH";

    private static final String CHANNEL = "zikr_bar";
    private static final int NOTIF_ID = 1;

    private static Bitmap iconCache;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Prefs.next(ZikrService.this);
            getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
            handler.postDelayed(this, Prefs.intervalMs(ZikrService.this));
        }
    };

    private WindowManager wm;
    private TasbihView tasbih;
    private WindowManager.LayoutParams lp;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = getSystemService(WindowManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "ذکر بار", NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        ch.enableVibration(false);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE_TASBIH.equals(intent.getAction())) {
            Prefs.setOverlay(this, !Prefs.overlay(this));
        }
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, n);
        }
        Prefs.setRunning(this, true);
        syncTasbih();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, Prefs.intervalMs(this));
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        removeTasbih();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---------------- notification ----------------

    private Notification buildNotification() {
        String text = Prefs.current(this);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent toggle = PendingIntent.getService(this, 1,
                new Intent(this, ZikrService.class).setAction(ACTION_TOGGLE_TASBIH),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Icon icon = Icon.createWithBitmap(allahIcon());
        String toggleLabel = Prefs.overlay(this) ? "تسبیح پټول" : "تسبیح ښکاره کول";

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(icon)
                .setContentTitle(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setColor(0xFF1B5E20)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(icon, toggleLabel, toggle).build())
                .build();
    }

    /** The name of Allah Ta'ala (ﷲ in the Amiri font), white on transparent, for the status bar. */
    private Bitmap allahIcon() {
        if (iconCache != null) return iconCache;
        int size = 128;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setTypeface(Ui.amiriBold(this));
        String word = "ﷲ";
        Rect b = new Rect();
        p.setTextSize(100f);
        p.getTextBounds(word, 0, word.length(), b);
        float scale = Math.min((size - 6f) / b.width(), (size - 6f) / b.height());
        p.setTextSize(100f * scale);
        p.getTextBounds(word, 0, word.length(), b);
        canvas.drawText(word, size / 2f - b.exactCenterX(), size / 2f - b.exactCenterY(), p);
        iconCache = bmp;
        return bmp;
    }

    // ---------------- floating tasbih ----------------

    private void syncTasbih() {
        boolean want = Prefs.overlay(this) && Settings.canDrawOverlays(this);
        if (!want) {
            removeTasbih();
            return;
        }
        if (tasbih == null) addTasbih();
        refreshTasbih();
    }

    private void refreshTasbih() {
        if (tasbih != null) {
            tasbih.setState(Prefs.tasbihStyle(this), Prefs.tasbihText(this), Prefs.count(this), Prefs.target(this));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addTasbih() {
        tasbih = new TasbihView(this);
        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = Prefs.posX(this);
        lp.y = Prefs.posY(this);

        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        tasbih.setOnTouchListener(new android.view.View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean moved, longDone;
            final Runnable longPress = () -> {
                longDone = true;
                Prefs.setCount(ZikrService.this, 0);
                refreshTasbih();
                vibrate(80);
                Toast.makeText(ZikrService.this, "شمېر صفر شو", Toast.LENGTH_SHORT).show();
            };

            @Override
            public boolean onTouch(android.view.View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = lp.x;
                        startY = lp.y;
                        moved = false;
                        longDone = false;
                        handler.postDelayed(longPress, 700);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        if (!moved && Math.hypot(dx, dy) > slop) {
                            moved = true;
                            handler.removeCallbacks(longPress);
                        }
                        if (moved) {
                            lp.x = startX + (int) dx;
                            lp.y = startY + (int) dy;
                            wm.updateViewLayout(tasbih, lp);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(longPress);
                        if (moved) {
                            Prefs.setPos(ZikrService.this, lp.x, lp.y);
                        } else if (!longDone) {
                            countOne();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longPress);
                        return true;
                }
                return false;
            }
        });
        try {
            wm.addView(tasbih, lp);
        } catch (RuntimeException e) {
            tasbih = null;
        }
    }

    private void countOne() {
        int n = Prefs.count(this) + 1;
        Prefs.setCount(this, n);
        refreshTasbih();
        int t = Prefs.target(this);
        if (t > 0 && n % t == 0) {
            vibrate(350);
        } else {
            vibrate(18);
        }
    }

    private void removeTasbih() {
        if (tasbih != null) {
            try {
                wm.removeView(tasbih);
            } catch (RuntimeException ignored) {
            }
            tasbih = null;
        }
    }

    private void vibrate(long ms) {
        Vibrator v = getSystemService(Vibrator.class);
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
